package com.bwsw.sj.transaction.generator.server

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import com.bwsw.sj.common.utils.{ConfigSettingsUtils, TransactionGeneratorLiterals}
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer, EventLoopGroup}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.log4j.Logger;

/**
 * Simple tcp server for creating transaction ID
 *
 *
 * @author Kseniya Tomskikh
 */
class TcpServer(zkServers: String, prefix: String, address: String) {
  private val logger = Logger.getLogger(getClass)
  private val retryPeriod = ConfigSettingsUtils.getServerRetryPeriod()
  private val curatorClient = createCuratorClient()
  private val masterNode = s"/$prefix/master"

  createMasterNode()

  private def createCuratorClient() = {
    val curatorClient = CuratorFrameworkFactory.newClient(zkServers, new ExponentialBackoffRetry(1000, 3))
    curatorClient.start()
    curatorClient.getZookeeperClient.blockUntilConnectedOrTimedOut()
    curatorClient
  }

  private def createMasterNode() = {
    val doesPathExist = Option(curatorClient.checkExists().forPath(masterNode))
    if (doesPathExist.isEmpty) curatorClient.create.creatingParentsIfNeeded().forPath(masterNode)
  }

  def launch() = {
    val leader = new LeaderLatch(curatorClient, masterNode, address)
    leader.start()
    while (!leader.hasLeadership) {
      Thread.sleep(retryPeriod)
    }
    logger.info(s"Launch a tcp server on: '$address'\n")
    val bossGroup: EventLoopGroup = new NioEventLoopGroup(1)
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrapServer = new ServerBootstrap()
      bootstrapServer.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new TcpServerChannelInitializer())

      val parts = address.split(":")
      bootstrapServer.bind(parts(0), parts(1).toInt).sync().channel().closeFuture().sync()
    } finally {
      leader.close()
      curatorClient.close()
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}

class TcpServerChannelInitializer() extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    channel.config().setTcpNoDelay(true)
    channel.config().setKeepAlive(true)
    channel.config().setTrafficClass(0x10)
    channel.config().setPerformancePreferences(0, 1, 0)

    val pipeline = channel.pipeline()

    pipeline.addLast("handler", new TransactionGenerator())
  }
}

@Sharable
class TransactionGenerator() extends ChannelInboundHandlerAdapter {
  private val counter = new AtomicInteger(0)
  private val currentMillis = new AtomicLong(0)
  private val scale = TransactionGeneratorLiterals.scale
  private var i = 0

  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    val id = generateID()
    val response = ctx.alloc().buffer(8).writeLong(id)
    ctx.writeAndFlush(response)
  }

  private def generateID() = this.synchronized {
    val now = System.currentTimeMillis()
    if (now - currentMillis.get > 0) {
      currentMillis.set(now)
      counter.set(0)
    }
    now * scale + counter.getAndIncrement()
  }

  /**
   * Exception handler that print stack trace and than close the connection when an exception is raised.
   * @param ctx Channel handler context
   * @param cause What has caused an exception
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) = {
    cause.printStackTrace()
    ctx.channel().close()
    ctx.channel().parent().close()
  }
}
