package com.bwsw.common.client

import com.bwsw.sj.common.utils.ConfigUtils
import org.apache.log4j.Logger

/**
 * Main object for running TcpClient
 *
 * @author Kseniya Tomskikh
 */
object ClientExample {

  private val logger = Logger.getLogger(getClass)

  def main(args: Array[String]) = {
    val zkServers = Array("176.120.25.19:2181")
    val prefix = "zk_test/global"

    val retryPeriod = ConfigUtils.getClientRetryPeriod()
    val retryCount = ConfigUtils.getServerRetryPeriod()

    val options = new TcpClientOptions()
      .setZkServers(zkServers)
      .setPrefix(prefix)
      .setRetryPeriod(retryPeriod)
      .setRetryCount(retryCount)

    val client = new TcpClient(options)
    client.open()
    //val consoleReader = new BufferedReader(new InputStreamReader(System.in))
    var i = 0
    val t0 = System.nanoTime()
    while (i <= 10) {
      //while (consoleReader.readLine() != null) {
      logger.debug("send request")
      println(client.get())
      //client.get()
      i += 1
    }
    client.close()
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) / 1000000 + "ms")
  }
}