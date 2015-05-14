package gearpump.example.transport.generator

import gearpump.example.transport.generator.MockCity
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.PropertyChecks

class MockCitySpec extends PropSpec with PropertyChecks with Matchers{

  property("MockCity should maintain the location properly") {
    val city = new MockCity(10)
    val start = city.randomLocationId()
    val nextLocation = city.nextLocation(start)
    assert(city.getDistance(start, nextLocation) == MockCity.LENGTH_PER_BLOCK)
  }
}
