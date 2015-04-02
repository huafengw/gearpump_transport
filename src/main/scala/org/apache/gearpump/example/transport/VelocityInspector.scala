package org.apache.gearpump.example.transport

import java.util.concurrent.TimeUnit

import akka.actor.Actor._
import akka.actor.ActorRef
import akka.pattern.ask
import org.apache.gearpump.Message
import org.apache.gearpump.cluster.UserConfig
import org.apache.gearpump.example.transport.generator.MockCity
import org.apache.gearpump.partitioner.Partitioner
import org.apache.gearpump.streaming._
import org.apache.gearpump.streaming.appmaster.AppMaster.{TaskActorRef, LookupTaskActorRef}
import org.apache.gearpump.streaming.task.{TaskId, StartTime, Task, TaskContext}
import org.apache.gearpump.util.Graph

import scala.collection.immutable.Queue
import scala.collection.mutable
import scala.concurrent.Future

class VelocityInspector(taskContext: TaskContext, conf: UserConfig) extends Task(taskContext, conf) {
  import taskContext.appMaster
  import system.dispatcher
  implicit val timeOut = akka.util.Timeout(3, TimeUnit.SECONDS)
  private val passRecords = mutable.Map.empty[String, Queue[PassRecord]]
  private val fakePlateThreshold = conf.getInt(VelocityInspector.FAKE_PLATE_THRESHOLD).get
  private val overdriveThreshold = conf.getInt(VelocityInspector.OVER_DRIVE_THRESHOLD).get
  private val citySize = conf.getInt(DataSource.MOCK_CITY_SIZE).get
  private val mockCity = new MockCity(citySize)
  private var queryServerActor: ActorRef = null

  override def onStart(startTime: StartTime): Unit = {
    val dag = DAG(conf.getValue[Graph[TaskDescription, Partitioner]](AppDescription.DAG).get)
    val queryServer = dag.processors.find { kv =>
      val (_, processor) = kv
      processor.taskClass == classOf[QueryServer].getName
    }.get
    val queryServerTaskId = TaskId(queryServer._1, 0)
    (appMaster ? LookupTaskActorRef(queryServerTaskId)).asInstanceOf[Future[TaskActorRef]].map {task =>
      queryServerActor = task.task
    }
  }

  import VelocityInspector._
  override def onNext(msg: Message): Unit = {
    msg.msg match {
      case passRecord: PassRecord =>
        val records = passRecords.getOrElse(passRecord.vehicleId, Queue.empty[PassRecord])
        if(records.size > 0) {
          val velocity = getVelocity(passRecord, records.last)
          val formatted = "%.2f".format(velocity)
          if(velocity > fakePlateThreshold) {
            LOG.info(s"vehicle ${passRecord.vehicleId} maybe a fake plate, the speed is $formatted km/h")
          } else if(velocity > overdriveThreshold) {
            if(queryServerActor != null) {
              queryServerActor ! OverSpeedReport(passRecord.vehicleId, formatted, passRecord.timeStamp)
            }
          }
        }
        passRecords.update(passRecord.vehicleId, records.enqueueFinite(passRecord, RECORDS_NUM))
    }
  }

  override def receiveUnManagedMessage: Receive = {
    case GetTrace(vehicleId) =>
      val records = passRecords.getOrElse(vehicleId, Queue.empty[PassRecord])
      sender ! VehicleTrace(records.toArray.sortBy(_.timeStamp))
  }
  
  private def getVelocity(passRecord: PassRecord, lastPassRecord: PassRecord): Float = {
    val distanceInKm = getDistance(lastPassRecord.locationId, passRecord.locationId)
    val timeInHour = (passRecord.timeStamp - lastPassRecord.timeStamp).toFloat / (1000 * 60 * 60)
    distanceInKm / timeInHour
  }
  
  private def getDistance(location1: String, location2: String): Long = {
    mockCity.getDistance(location1, location2)
  }
}

object VelocityInspector{
  final val OVER_DRIVE_THRESHOLD = "overdrive.threshold"
  final val FAKE_PLATE_THRESHOLD = "fakeplate.threshold"
  final val RECORDS_NUM = 100

  class FiniteQueue[T](q: Queue[T]) {
    def enqueueFinite[B >: T](elem: B, maxSize: Int): Queue[B] = {
      var result = q.enqueue(elem)
      while (result.size > maxSize) {
        result = result.dequeue._2
      }
      result
    }
  }

  implicit def queue2FiniteQueue[T](q: Queue[T]): FiniteQueue[T] = new FiniteQueue[T](q)
}