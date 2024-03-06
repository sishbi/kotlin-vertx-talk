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
import sishbi.vertx.grpc.VertxConferenceGrpcClient
import sishbi.vertx.grpc.registerRequest


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
    fun `register grpc with unknown user`(vertx: Vertx) {
        val client = GrpcClient.client(vertx)
        val hello = VertxConferenceGrpcClient(client, SocketAddress.inetSocketAddress(8889, "localhost"))

        try {
            runBlocking {
                val response = hello.register(registerRequest {
                    name = "Unknown User"
                }).coAwait()
                LOG.info { "gRPC Response: ${response.name} = ${response.registrationNumber}" }
            }
        } catch (e: Throwable) {
            LOG.info { "$e" }
        }
    }

    @Test
    fun `register grpc with known user`(vertx: Vertx) {
        val client = GrpcClient.client(vertx)
        val hello = VertxConferenceGrpcClient(client, SocketAddress.inetSocketAddress(8889, "localhost"))

        runBlocking {
            val response = hello.register(registerRequest {
                name = "User 1"
            }).coAwait()
            LOG.info { "gRPC Response: ${response.name} = ${response.registrationNumber}" }
        }
    }

    @Test
    fun `attendees http`(vertx: Vertx) {
        val client = WebClient.create(vertx)
        runBlocking {
            val response = client.getAbs("http://localhost:8888/attendees").send().coAwait()
            LOG.info { "Http Response: ${response.bodyAsString()}" }
        }
    }
}
