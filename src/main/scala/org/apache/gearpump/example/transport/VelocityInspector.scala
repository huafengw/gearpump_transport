package org.apache.gearpump.example.transport

import org.apache.gearpump.Message
import org.apache.gearpump.cluster.UserConfig
import org.apache.gearpump.example.transport.generator.MockCity
import org.apache.gearpump.streaming.task.{StartTime, Task, TaskContext}

import scala.collection.mutable.Map

class VelocityInspector(taskContext: TaskContext, conf: UserConfig) extends Task(taskContext, conf) {
  private val passRecords = Map.empty[String, PassRecord]
  private val fakePlateThreshold = conf.getInt(VelocityInspector.FAKE_PLATE_THRESHOLD).get
  private val overdriveThreshold = conf.getInt(VelocityInspector.OVER_DRIVE_THRESHOLD).get
  private val citySize = conf.getInt(DataSource.MOCK_CITY_SIZE).get
  private val mockCity = new MockCity(citySize)
  
  override def onStart(startTime: StartTime): Unit = {
  }

  override def onNext(msg: Message): Unit = {
    msg.msg match {
      case passRecord: PassRecord =>
        if(passRecords.contains(passRecord.vehicleId)) {
          val velocity = getVelocity(passRecord, passRecords.get(passRecord.vehicleId).get)
          val formatted = "%.2f".format(velocity)
          if(velocity > fakePlateThreshold) {
            LOG.info(s"vehicle ${passRecord.vehicleId} maybe a fake plate, the speed is $formatted km/h")
            //Fake license
          } else if(velocity > overdriveThreshold) {
            LOG.info(s"vehicle ${passRecord.vehicleId} is over speed, the speed is $formatted km/h")
           //Overdrive
          }
        }
        passRecords.update(passRecord.vehicleId, passRecord)
    }
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
}