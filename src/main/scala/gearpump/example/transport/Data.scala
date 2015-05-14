package gearpump.example.transport

case class LocationInfo(id: String, row: Int, column: Int)

case class PassRecord(vehicleId: String, locationId: String, timeStamp: Long) {
  override def hashCode: Int = vehicleId.hashCode
}

case class GetTrace(vehicleId: String)

case class VehicleTrace(records: Array[PassRecord])

case class OverSpeedReport(vehicleId: String, speed: String, timestamp: Long, locationId: String)