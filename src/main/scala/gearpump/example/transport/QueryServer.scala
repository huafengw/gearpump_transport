package gearpump.example.transport

import java.util.concurrent.TimeUnit

import akka.actor.{Props, Actor}
import akka.actor.Actor._
import akka.io.IO
import akka.pattern.ask
import org.apache.gearpump.Message
import org.apache.gearpump.cluster.MasterToAppMaster.AppMasterDataDetailRequest
import org.apache.gearpump.cluster.UserConfig
import QueryServer.WebServer
import org.apache.gearpump.streaming._
import org.apache.gearpump.streaming.appmaster.AppMaster.{TaskActorRef, LookupTaskActorRef}
import org.apache.gearpump.streaming.appmaster.StreamingAppMasterDataDetail
import org.apache.gearpump.streaming.task.{TaskId, StartTime, Task, TaskContext}
import spray.can.Http
import spray.http.StatusCodes
import spray.routing.HttpService
import upickle._
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class QueryServer(taskContext: TaskContext, conf: UserConfig) extends Task(taskContext, conf){
  import taskContext.{appMaster, appId}
  import system.dispatcher

  var inspector: (ProcessorId, TaskDescription) = null
  implicit val timeOut = akka.util.Timeout(3, TimeUnit.SECONDS)

  override def onStart(startTime: StartTime): Unit = {
    appMaster ! AppMasterDataDetailRequest(appId)
    taskContext.actorOf(Props(new WebServer))
  }

  override def onNext(msg: Message): Unit = {
  }

  override def receiveUnManagedMessage: Receive = {
    case detail: StreamingAppMasterDataDetail =>
      inspector = detail.processors.find { kv =>
        val (_, processor) = kv
        processor.taskClass == classOf[VelocityInspector].getName
      }.get
    case getTrace @ GetTrace(vehicleId: String) =>
      val parallism = inspector._2.parallelism
      val processorId = inspector._1
      val analyzerTaskId = TaskId(processorId, (vehicleId.hashCode & Integer.MAX_VALUE) % parallism)
      val requester = sender
      (appMaster ? LookupTaskActorRef(analyzerTaskId))
        .asInstanceOf[Future[TaskActorRef]].flatMap { task =>
        (task.task ? getTrace).asInstanceOf[Future[VehicleTrace]]
      }.map { trace =>
        LOG.info(s"reporting $trace")
        requester ! trace
      }
    case OverSpeedReport(vehicleId, speed, timestamp) =>
      LOG.info(s"vehicle $vehicleId is over speed, the speed is $speed km/h")
    case _ =>
      //ignore
  }
}

object QueryServer {
  class WebServer extends Actor with HttpService {

    import context.dispatcher
    implicit val timeOut = akka.util.Timeout(3, TimeUnit.SECONDS)
    def actorRefFactory = context
    implicit val system = context.system

    IO(Http) ! Http.Bind(self, interface = "localhost", port = 8080)

    override def receive: Receive = runRoute(webServer ~ staticRoute)

    def webServer = {
      path("trace" / PathElement) { vehicleId =>
        get {
          onComplete((context.parent ? GetTrace(vehicleId)).asInstanceOf[Future[VehicleTrace]]) {
            case Success(trace: VehicleTrace) =>
              val json = write(trace)
              complete(pretty(json))
            case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
        }
      }
    }

    val staticRoute = {
      pathEndOrSingleSlash {
        getFromResource("transport/transport.html")
      } ~
        pathPrefix("css") {
          get {
            getFromResourceDirectory("transport/css")
          }
        } ~
        pathPrefix("js") {
          get {
            getFromResourceDirectory("transport/js")
          }
        }
    }

    private def pretty(json: String): String = {
      json.parseJson.prettyPrint
    }
  }
}
