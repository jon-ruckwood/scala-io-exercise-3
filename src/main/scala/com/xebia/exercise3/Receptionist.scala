package com.xebia
package exercise3

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import akka.actor.{Props, ActorRef}
import akka.util.Timeout

import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing._

class Receptionist extends HttpServiceActor
                      with ReverseRoute
                      with CreationSupport {

  import ReverseActor._

  def receive = runRoute(reverseRoute)

}

trait ReverseRoute extends HttpService {

  def createChild(props:Props, name:String):ActorRef

  import ReverseActor._
  private val reverseActor = createChild(props, name)

  def reverseRoute:Route = path("reverse") {
    post {
      entity(as[ReverseRequest]) { request =>
        // We will fix this import and the timeout definition in a next exercise
        import ExecutionContext.Implicits.global
        import scala.concurrent.duration._
        implicit val timeout = Timeout(20 seconds)
        import akka.pattern.ask

        import ReverseActor._

        val futureResponse = reverseActor.ask(Reverse(request.value)).mapTo[Result]

        onComplete(futureResponse) {
          case Success(PalindromeResult) => complete(ReverseResponse(request.value, true))
          case Success(ReverseResult(value)) => complete(ReverseResponse(value, false))
          case Success(NotInitialized) => complete(StatusCodes.ServiceUnavailable)
          case Failure(e) => complete(StatusCodes.InternalServerError)
        }
      }
    }
  }
}

