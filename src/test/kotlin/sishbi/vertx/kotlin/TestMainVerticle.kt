package sishbi.vertx.kotlin

import io.vertx.core.Vertx
import io.vertx.core.net.SocketAddress
import io.vertx.ext.web.client.WebClient
import io.vertx.grpc.client.GrpcClient
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import sishbi.vertx.grpc.VertxGreeterGrpcClient
import sishbi.vertx.grpc.helloRequest


private val LOG = KotlinLogging.logger {}

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestMainVerticle {
    private val postgres = PostgreSQLContainer("postgres:15")

    @BeforeAll
    fun deploy_verticle(vertx: Vertx) {
        postgres.start()
        configuration.dbHost = postgres.host
        configuration.dbPort = postgres.firstMappedPort
        configuration.dbDB = postgres.databaseName
        configuration.dbUser = postgres.username
        configuration.dbPassword = postgres.password

        runBlocking {
            vertx.deployVerticle(MainVerticle()).coAwait()
        }
    }

    @AfterAll
    fun stop_db() {
        postgres.stop()
    }

    @Test
    fun `hello grpc with unknown user`(vertx: Vertx) {
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
    fun `hello grpc with known user`(vertx: Vertx) {
        val client = GrpcClient.client(vertx)
        val hello = VertxGreeterGrpcClient(client, SocketAddress.inetSocketAddress(8889, "localhost"))

        runBlocking {
            val response = hello.sayHello(helloRequest {
                name = "User 1"
            }).coAwait()
            LOG.info { "gRPC Response: ${response.message}" }
        }
    }

    @Test
    fun `hello http`(vertx: Vertx) {
        val client = WebClient.create(vertx)
        runBlocking {
            val response = client.getAbs("http://localhost:8888/hello").send().coAwait()
            LOG.info { "Http Response: ${response.bodyAsString()}" }
        }
    }
}
