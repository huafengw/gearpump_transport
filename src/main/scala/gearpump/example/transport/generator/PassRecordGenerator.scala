package gearpump.example.transport.generator

import gearpump.example.transport.PassRecord
import PassRecord
import org.apache.gearpump.util.LogUtil

import scala.util.Random

class PassRecordGenerator(vehicleId: String, city: MockCity, overdriveThreshold: Int) {
  private val LOG = LogUtil.getLogger(getClass)
  LOG.info(s"Generate pass record for vehicle $vehicleId")
  private var timeStamp = System.currentTimeMillis()

  private var locationId = city.randomLocationId()
  private val random = new Random()
  private val fakePlate = random.nextInt(1000) < 1000 * PassRecordGenerator.FAKE_PLATE_RATE
  private val (randomMin, randomRange) = {
    val lowerBound = MockCity.LENGTH_PER_BLOCK * 1000 * 60 * 60 / overdriveThreshold.toFloat
    val upperBound = MockCity.LENGTH_PER_BLOCK * 1000 * 60 * 60 / MockCity.MINIMAL_SPEED.toFloat
    val overdrive = (upperBound - lowerBound) * PassRecordGenerator.OVERDRIVE_RATE
    val randomMin = Math.max(lowerBound - overdrive, PassRecordGenerator.TWOMINUTES)
    val randomRange = upperBound - randomMin
    (randomMin.toInt, randomRange.toInt)
  }
  
  def getNextPassRecord(): PassRecord = {
    locationId = if(fakePlate) {
      city.randomLocationId()
    } else {
      city.nextLocation(locationId)
    }
    timeStamp += (random.nextInt(randomRange) + randomMin)
    PassRecord(vehicleId, locationId, timeStamp)
  }
}

object PassRecordGenerator {
  final val FAKE_PLATE_RATE = 0F
  final val OVERDRIVE_RATE = 0.1F
  final val TWOMINUTES = 2 * 60 * 1000
  
  def create(generatorNum: Int, prefix: String, city: MockCity, overdriveThreshold: Int): Array[PassRecordGenerator] = {
    var result = Map.empty[String, PassRecordGenerator]
    val digitsNum = (Math.log10(generatorNum) + 1).toInt
    for(i <- 1 to generatorNum) {
      val vehicleId = prefix + s"%0${digitsNum}d".format(i)
      val generator = new PassRecordGenerator(vehicleId, city, overdriveThreshold)
      result += vehicleId -> generator
    }
    result.values.toArray
  }
}
