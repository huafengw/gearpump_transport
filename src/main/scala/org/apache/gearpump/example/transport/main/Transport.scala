package org.apache.gearpump.example.transport.main

import org.apache.gearpump.cluster.UserConfig
import org.apache.gearpump.cluster.client.ClientContext
import org.apache.gearpump.cluster.main.{ParseResult, ArgumentsParser, CLIOption}
import org.apache.gearpump.example.transport.partitioner.PassRecordPartitioner
import org.apache.gearpump.example.transport.{VelocityInspector, DataSource}
import org.apache.gearpump.streaming.{TaskDescription, AppDescription}
import org.apache.gearpump.util.Graph
import org.apache.gearpump.util.Graph._

object Transport extends App with ArgumentsParser {
  override val options: Array[(String, CLIOption[Any])] = Array(
    "master" -> CLIOption[String]("<host1:port1,host2:port2,host3:port3>", required = true),
    "source"-> CLIOption[Int]("<how many task to generate data>", required = false, defaultValue = Some(10)),
    "inspector"-> CLIOption[Int]("<how many over speed inspector>", required = false, defaultValue = Some(4)),
    "vehicle"-> CLIOption[Int]("<how many vehicles's to generate>", required = false, defaultValue = Some(1000)),
    "citysize"-> CLIOption[Int]("<the blocks number of the mock city>", required = false, defaultValue = Some(10)),
    "threshold"-> CLIOption[Int]("<overdrive threshold, km/h>", required = false, defaultValue = Some(60)))
  
  def application(config: ParseResult): AppDescription = {
    val sourceNum = config.getInt("source")
    val inspectorNum = config.getInt("inspector")
    val vehicleNum = config.getInt("vehicle")
    val citysize = config.getInt("citysize")
    val threshold = config.getInt("threshold")
    val source = TaskDescription(classOf[DataSource].getName, sourceNum)
    val inspector = TaskDescription(classOf[VelocityInspector].getName, inspectorNum)
    val partitioner = new PassRecordPartitioner
    val userConfig = UserConfig.empty.withInt(DataSource.VEHICLE_NUM, vehicleNum).
      withInt(DataSource.MOCK_CITY_SIZE, citysize).
      withInt(VelocityInspector.OVER_DRIVE_THRESHOLD, threshold).
      withInt(VelocityInspector.FAKE_PLATE_THRESHOLD, 200)
    AppDescription("transport", userConfig, Graph(source ~ partitioner ~> inspector))
  }
  
  val config = parse(args)
  val context = ClientContext(config.getString("master"))
  implicit val system = context.system
  context.submit(application(config))
  context.close()
}

