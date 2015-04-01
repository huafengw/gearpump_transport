import de.johoop.jacoco4sbt.JacocoPlugin.jacoco
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._

import scala.collection.immutable.Map.WithDefault
import sbt.Keys._
import sbt._
import xerial.sbt.Sonatype._
import com.typesafe.sbt.SbtPgp.autoImport._

object Build extends sbt.Build {

  class DefaultValueMap[+B](value : B) extends WithDefault[String, B](null, (key) => value) {
    override def get(key: String) = Some(value)
  }

  val gearPumpVersion = "0.3.3-SNAPSHOT"
  val json4sVersion = "3.2.10"

  val scalaVersionMajor = "scala-2.11"
  val scalaVersionNumber = "2.11.4"
  val sprayVersion = "1.3.2"
  val sprayJsonVersion = "1.3.1"
  val spraySwaggerVersion = "0.5.0"
  val swaggerUiVersion = "2.0.24"
  val scalaTestVersion = "2.2.0"
  val scalaCheckVersion = "1.11.3"
  val mockitoVersion = "1.10.8"
  val bijectionVersion = "0.7.0"

  val commonSettings = Defaults.defaultSettings ++ Seq(jacoco.settings:_*) ++ sonatypeSettings  ++ net.virtualvoid.sbt.graph.Plugin.graphSettings ++
    Seq(
      resolvers ++= Seq(
        "patriknw at bintray" at "http://dl.bintray.com/patriknw/maven",
        "maven-repo" at "http://repo.maven.apache.org/maven2",
        "maven1-repo" at "http://repo1.maven.org/maven2",
        "maven2-repo" at "http://mvnrepository.com",
        "sonatype" at "https://oss.sonatype.org/content/repositories/releases",
        "clockfly" at "http://dl.bintray.com/clockfly/maven",
        "sonatype snapshot" at "https://oss.sonatype.org/content/repositories/snapshots"
      )
    ) ++
    Seq(
      scalaVersion := scalaVersionNumber,
      version := gearPumpVersion,
      organization := "com.github.intel-hadoop",
      useGpg := true,
      scalacOptions ++= Seq("-Yclosure-elim","-Yinline"),
      pomExtra := {
        <url>https://github.com/intel-hadoop/gearpump</url>
          <licenses>
            <license>
              <name>Apache 2</name>
              <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
            </license>
          </licenses>
          <scm>
            <connection>scm:git:github.com/intel-hadoop/gearpump</connection>
            <developerConnection>scm:git:git@github.com:intel-hadoop/gearpump</developerConnection>
            <url>github.com/intel-hadoop/gearpump</url>
          </scm>
          <developers>
            <developer>
              <id>gearpump</id>
              <name>Gearpump Team</name>
              <url>https://github.com/intel-hadoop/teams/gearpump</url>
            </developer>
          </developers>
      }
    )

  val myAssemblySettings = assemblySettings ++ Seq(
    test in assembly := {},
    assemblyOption in assembly ~= { _.copy(includeScala = false) }
  )

  val coreDependencies = Seq(
    libraryDependencies ++= Seq(
      "com.github.intel-hadoop" %% "gearpump-streaming" % gearPumpVersion,
      "com.github.intel-hadoop" %% "gearpump-external-kafka" % gearPumpVersion,
      "joda-time" % "joda-time" % "2.7",
      "io.spray" %% "spray-httpx" % sprayVersion,
      "io.spray" %% "spray-client" % sprayVersion,
      "io.spray" %% "spray-json" % sprayJsonVersion,
      "com.gettyimages" %% "spray-swagger" % spraySwaggerVersion excludeAll(ExclusionRule(organization = "org.json4s"), ExclusionRule(organization = "io.spray")),
      "org.json4s" %% "json4s-jackson" % json4sVersion,
      "org.json4s" %% "json4s-native" % json4sVersion,
      "org.webjars" % "swagger-ui" % swaggerUiVersion,
      "com.lihaoyi" %% "upickle" % "0.2.5",
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test"
    )
  )

  lazy val root  = Project(
    id = "transport",
    base = file("."),
    settings = commonSettings ++ coreDependencies
  )
}
