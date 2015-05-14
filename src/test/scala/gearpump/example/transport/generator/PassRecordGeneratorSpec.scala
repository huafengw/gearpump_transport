package gearpump.example.transport.generator

import gearpump.example.transport.generator.{MockCity, PassRecordGenerator}
import org.apache.gearpump.example.transport.generator.MockCity
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.PropertyChecks

class PassRecordGeneratorSpec extends PropSpec with PropertyChecks with Matchers{

  property("PassRecordGenerator should generate pass record"){
    val id = "test"
    val city = new MockCity(10)
    val generator = new PassRecordGenerator(id, city, 60)
    val passrecord1 = generator.getNextPassRecord()
    val passrecord2 = generator.getNextPassRecord()
    assert(city.getDistance(passrecord1.locationId, passrecord2.locationId) == MockCity.LENGTH_PER_BLOCK)
  }
}
