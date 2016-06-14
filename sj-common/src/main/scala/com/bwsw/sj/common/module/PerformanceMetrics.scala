package com.bwsw.sj.common.module

/**
 * Class represents a set of metrics that characterize performance of module
 * Created: 07/06/2016
 * @author Kseniya Mikhaleva
 */

import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock
import com.bwsw.common.JsonSerializer
import org.slf4j.LoggerFactory
import scala.collection._

class PerformanceMetrics(taskId: String, host: String, streamNames: Array[String]) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val mutex = new ReentrantLock(true)
  private val serializer = new JsonSerializer()
  private var inputEnvelopesPerStream = mutable.Map(streamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
  private var outputEnvelopesPerStream = mutable.Map(streamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)
  private var totalIdleTime = 0L
  private var numberOfStateVariables = 0
  private val startTime = System.currentTimeMillis()

  /**
   * Increases time when there are no messages (envelopes)
   * @param idle How long waiting was a new envelope
   */
  def increaseTotalIdleTime(idle: Long) = {
    mutex.lock()
    logger.debug(s"Increase total idle time on $idle ms\n")
    totalIdleTime += idle
    mutex.unlock()
  }

  /**
   * Sets an amount of how many state variables are
   * @param amount Number of state variables
   */
  def setNumberOfStateVariables(amount: Int) = {
    mutex.lock()
    logger.debug(s"Set number of state varibles to $amount\n")
    numberOfStateVariables = amount
    mutex.unlock()
  }

  /**
   * Invokes when a new envelope from some input stream is received
   * @param name Stream name
   * @param elementsSize Set of sizes of elements
   */
  def addEnvelopeToInputStream(name: String, elementsSize: List[Int]) = {
    mutex.lock()
    logger.debug(s"Indicate that a new envelope is received from input stream: $name\n")
    if (inputEnvelopesPerStream.contains(name)) {
      inputEnvelopesPerStream(name) += elementsSize
    } else {
      logger.error(s"Input stream with name: $name doesn't exist\n")
      throw new Exception(s"Input stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  /**
   * Invokes when a new txn is created for some output stream
   * @param name Stream name
   * @param envelopeID Id of envelope of output stream
   * @param elementsSize Set of sizes of elements
   */
  def addEnvelopeToOutputStream(name: String, envelopeID: String, elementsSize: mutable.ListBuffer[Int]) = {
    mutex.lock()
    logger.debug(s"Indicate that a new txn: $envelopeID is created for output stream: $name\n")
    if (outputEnvelopesPerStream.contains(name)) {
      outputEnvelopesPerStream(name) += (envelopeID -> elementsSize)
    } else {
      logger.error(s"Output stream with name: $name doesn't exist\n")
      throw new Exception(s"Output stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  /**
   * Invokes when a new element is sent to txn of some output stream
   * @param name Stream name
   * @param envelopeID Id of envelope of output stream
   * @param elementSize Size of appended element
   */
  def addElementToOutputEnvelope(name: String, envelopeID: String, elementSize: Int) = {
    mutex.lock()
    logger.debug(s"Indicate that a new element is sent to txn: $envelopeID of output stream: $name\n")
    if (outputEnvelopesPerStream.contains(name)) {
      if (outputEnvelopesPerStream(name).contains(envelopeID)) {
        outputEnvelopesPerStream(name)(envelopeID) += elementSize
      } else {
        logger.error(s"Output stream with name: $name doesn't contain txn: $envelopeID\n")
        throw new Exception(s"Output stream with name: $name doesn't contain txn: $envelopeID")
      }
    } else {
      logger.error(s"Output stream with name: $name doesn't exist\n")
      throw new Exception(s"Output stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  /**
   * Constructs a report of performance metrics of task's work
   * @return Constructed performance report
   */
  def getReport = {
    logger.info(s"Start preparing a report of performance for task: $taskId of regular module\n")
    mutex.lock()
    val numberOfInputEnvelopesPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.size))
    val numberOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.size))
    val numberOfInputElementsPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.size).sum))
    val numberOfOutputElementsPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.size).sum))
    val bytesOfInputEnvelopesPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.sum).sum))
    val bytesOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.sum).sum))
    val inputEnvelopesTotalNumber = numberOfInputEnvelopesPerStream.values.sum
    val inputElementsTotalNumber = numberOfInputElementsPerStream.values.sum
    val outputEnvelopesTotalNumber = numberOfOutputEnvelopesPerStream.values.sum
    val outputElementsTotalNumber = numberOfOutputElementsPerStream.values.sum
    val inputEnvelopesSize = inputEnvelopesPerStream.flatMap(x => x._2.map(_.size))
    val outputEnvelopesSize = outputEnvelopesPerStream.flatMap(x => x._2.map(_._2.size))


    logger.debug(s"Reset variables for performance report for next reporting\n")
    inputEnvelopesPerStream = mutable.Map(streamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
    outputEnvelopesPerStream = mutable.Map(streamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)
    totalIdleTime = 0L
    numberOfStateVariables = 0

    val performanceReport =
    s"""{
    |"datetime" : "${LocalDateTime.now()}"
    |"task-id" : "$taskId",
    |"host" : "$host",
    |"total-idle-time" : "$totalIdleTime",
    |"total-input-envelopes" : "$inputEnvelopesTotalNumber",
    |"input-envelopes-per-stream" : ${serializer.serialize(numberOfInputEnvelopesPerStream)},
    |"total-input-elements" : "$inputElementsTotalNumber",
    |"input-elements-per-stream" : ${serializer.serialize(numberOfInputElementsPerStream)},
    |"total-input-bytes" : "${bytesOfInputEnvelopesPerStream.values.sum}",
    |"input-bytes-per-stream" : ${serializer.serialize(bytesOfInputEnvelopesPerStream)},
    |"average-size-input-envelope" : "${if (inputElementsTotalNumber != 0)  inputElementsTotalNumber / inputEnvelopesTotalNumber else 0}",
    |"max-size-input-envelope" : "${if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0}",
    |"min-size-input-envelope" : "${if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0}",
    |"average-size-input-element" : "${if (inputElementsTotalNumber != 0) bytesOfInputEnvelopesPerStream.values.sum / inputElementsTotalNumber else 0}",
    |"total-output-envelopes" : "$outputEnvelopesTotalNumber",
    |"output-envelopes-per-stream" : ${serializer.serialize(numberOfOutputEnvelopesPerStream)},
    |"total-output-elements" : "$outputElementsTotalNumber",
    |"output-elements-per-stream" : ${serializer.serialize(numberOfOutputElementsPerStream)},
    |"total-output-bytes" : "${bytesOfOutputEnvelopesPerStream.values.sum}",
    |"output-bytes-per-stream" : ${serializer.serialize(bytesOfOutputEnvelopesPerStream)},
    |"average-size-output-envelope" : "${if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0}",
    |"max-size-output-envelope" : "${if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0}",
    |"min-size-output-envelope" : "${if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0}",
    |"average-size-output-element" : "${if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopesPerStream.values.sum / outputElementsTotalNumber else 0}",
    |"state-variables-number" : "$numberOfStateVariables",
    |"uptime" : "${(System.currentTimeMillis() - startTime) / 1000}",
      }
    """.stripMargin


    mutex.unlock()

    performanceReport
  }
}
