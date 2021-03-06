package com.bwsw.common.client

import org.apache.log4j.Logger

/**
 * Main object for running TcpClient
 *
 * @author Kseniya Tomskikh
 */
object TcpClientExample {

  private val logger = Logger.getLogger(getClass)

  def main(args: Array[String]) = {
    val zkServers = Array("176.120.25.19:2181")
    val prefix = "zk_test/global"

    val options = new TcpClientOptions()
      .setZkServers(zkServers)
      .setPrefix(prefix)

    val client = new TcpClient(options)
    client.open()
    //val consoleReader = new BufferedReader(new InputStreamReader(System.in))
    var i = 0
    val t0 = System.currentTimeMillis()
    while (i <= 1000000) {
      //while (consoleReader.readLine() != null) {
      logger.debug("send request")
      //println(client.get())
      client.get()
      i += 1
    }
    client.close()
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
  }
}