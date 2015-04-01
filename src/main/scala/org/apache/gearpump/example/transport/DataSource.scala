package org.apache.gearpump.example.transport

import org.apache.gearpump.Message
import org.apache.gearpump.cluster.UserConfig
import org.apache.gearpump.example.transport.generator.{PassRecordGenerator, MockCity}
import org.apache.gearpump.streaming.task.{TaskId, StartTime, TaskContext, Task}
import scala.concurrent.duration._

class DataSource(taskContext: TaskContext, conf: UserConfig) extends Task(taskContext, conf){
  import taskContext.{output, parallelism, taskId}

  import system.dispatcher
  private val overdriveThreshold = conf.getInt(VelocityInspector.OVER_DRIVE_THRESHOLD).get
  private val vehicleNum = conf.getInt(DataSource.VEHICLE_NUM).get / parallelism
  private val citySize = conf.getInt(DataSource.MOCK_CITY_SIZE).get
  private val mockCity = new MockCity(citySize)
  private val recordGenerators: Array[PassRecordGenerator] = 
    PassRecordGenerator.create(vehicleNum, getIdentifier(taskId), mockCity, overdriveThreshold)
  
  override def onStart(startTime: StartTime): Unit = {
    self ! Message("start", System.currentTimeMillis())
  }

  override def onNext(msg: Message): Unit = {
    recordGenerators.foreach(generator => 
      output(Message(generator.getNextPassRecord(), System.currentTimeMillis())))
    system.scheduler.scheduleOnce(1 second)(self ! Message("continue", System.currentTimeMillis()))
  }
  
  private def getIdentifier(taskId: TaskId): String = {
    s"${taskId.processorId}${taskId.index}"
  }
}

object DataSource {
  final val VEHICLE_NUM = "vehicle.number"
  final val MOCK_CITY_SIZE = "mock.city.size"
}
