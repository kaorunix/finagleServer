package http

import com.twitter.finagle.http.{Request,Response,RichHttp}
import com.twitter.finagle.{Service, SimpleFilter}
//import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future
import java.net.InetSocketAddress
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.Http

/**
 * This example demonstrates a sophisticated HTTP server that handles exceptions
 * and performs authorization via a shared secret. The exception handling and
 * authorization code are written as Filters, thus isolating these aspects from
 * the main service (here called "Respond") for better code organization.
 */
object HttpServer {
  /**
   * A simple Filter that catches exceptions and converts them to appropriate
   * HTTP responses.
   */
  class HandleExceptions extends SimpleFilter[Request, Response] {
    def apply(request: Request, service: Service[Request, Response]) = {

      // `handle` asynchronously handles exceptions.
      service(request) handle { case error =>
        val statusCode = error match {
          case _: IllegalArgumentException =>
            403 //FORBIDDEN
          case _ =>
            500 //INTERNAL_SERVER_ERROR
        }
        val errorResponse = Response() //new DefaultHttpResponse(HTTP_1_1, statusCode)
        errorResponse.setStatusCode(statusCode)
        errorResponse.setContent(copiedBuffer(error.getStackTraceString, UTF_8))

        errorResponse
      }
    }
  }

  /**
   * A simple Filter that checks that the request is valid by inspecting the
   * "Authorization" header.
   */
  class Authorize extends SimpleFilter[Request, Response] {
    def apply(request: Request, continue: Service[Request, Response]) = {
      if ("open sesame" == request.getHeader("Authorization")) {
        continue(request)
      } else {
        Future.exception(new IllegalArgumentException("You don't know the secret"))
      }
    }
  }

  /**
   * The service itself. Simply echos back "hello world"
   */
  class Respond extends Service[Request, Response] {
    def apply(request: Request) = {
      val response = Response()
	  response.setStatusCode(200)
	  response.setContent(copiedBuffer("SUCCESS", UTF_8))
	  Future.value(response)
    }
  }

  def main(args: Array[String]) {
    val handleExceptions = new HandleExceptions
    val authorize = new Authorize
    val respond = new Respond

    // compose the Filters and Service together:
    val myService: Service[Request, Response] = handleExceptions andThen respond

    val server: Server = ServerBuilder()
      .codec(RichHttp[Request](Http()))
      .bindTo(new InetSocketAddress(8080))
      .maxConcurrentRequests(100)
      .name("httpserver")
      .build(myService)



      // これが正しいかわからないが終了処理
      Runtime.getRuntime().addShutdownHook(new Thread() {
    	  override def run() = {
    	    println("close function")
//    		cassandracon.close();println("finish close")
    	  }
      })

  }
}