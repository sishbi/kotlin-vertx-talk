package sishbi.vertx.kotlin

import io.vertx.core.Vertx
import io.vertx.core.net.SocketAddress
import io.vertx.ext.web.client.WebClient
import io.vertx.grpc.client.GrpcClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import sishbi.vertx.grpc.VertxGreeterGrpcClient
import sishbi.vertx.grpc.helloRequest

private val LOG = KotlinLogging.logger {}

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestMainVerticle {
  @BeforeAll
  fun deploy_verticle(vertx: Vertx) {
    runBlocking {
      vertx.deployVerticle(MainVerticle()).coAwait()
    }
  }

  @Test
  fun hello_grpc(vertx: Vertx) {
    val client = GrpcClient.client(vertx)
    val hello = VertxGreeterGrpcClient(client, SocketAddress.inetSocketAddress(8889, "localhost"))

    runBlocking {
      val response = hello.sayHello(helloRequest {
        name = "User"
      }).coAwait()
      LOG.info { "gRPC Response: ${response.message}" }
    }
  }

  @Test
  fun hello_http(vertx: Vertx) {
    val client = WebClient.create(vertx)
    runBlocking {
      val response = client.getAbs("http://localhost:8888/hello").send().coAwait()
      LOG.info { "Http Response: ${response.bodyAsString()}" }
    }
  }
}
