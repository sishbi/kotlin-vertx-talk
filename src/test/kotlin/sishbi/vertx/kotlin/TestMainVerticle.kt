package sishbi.vertx.kotlin

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.Vertx
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.net.SocketAddress
import io.vertx.ext.web.client.WebClient
import io.vertx.grpc.client.GrpcClient
import io.vertx.grpc.common.GrpcStatus
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
import sishbi.vertx.grpc.ConferenceCheckGrpc
import sishbi.vertx.grpc.ConferenceRegGrpc
import sishbi.vertx.grpc.VertxConferenceCheckGrpcClient
import sishbi.vertx.grpc.VertxConferenceRegGrpcClient
import sishbi.vertx.grpc.checkRequest
import sishbi.vertx.grpc.registerRequest


private val LOG = KotlinLogging.logger {}

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestMainVerticle {
    private val postgres = PostgreSQLContainer("postgres:15")
    private val address = SocketAddress.inetSocketAddress(8889, "localhost")
    private lateinit var grpcClient: GrpcClient
    private lateinit var webClient: WebClient

    @BeforeAll
    fun setup(vertx: Vertx) {
        DatabindCodec.mapper().registerKotlinModule()
        postgres.start()
        config.dbHost = postgres.host
        config.dbPort = postgres.firstMappedPort
        config.dbDB = postgres.databaseName
        config.dbUser = postgres.username
        config.dbPassword = postgres.password

        runBlocking {
            vertx.deployVerticle(MainVerticle()).coAwait()
            grpcClient = GrpcClient.client(vertx)
            webClient = WebClient.create(vertx)
        }
    }

    @AfterAll
    fun stop_db() {
        postgres.stop()
    }

    @Test
    fun `check grpc with known user`() {
        try {
            runBlocking {
                val checkClient =
                    VertxConferenceCheckGrpcClient(grpcClient, address)
                checkClient.check(
                    checkRequest {
                        name = "User 1"
                    }
                ).coAwait().also {
                    LOG.info { "${it.name} = ${it.role} (${it.id})" }
                }
            }
        } catch (e: Throwable) {
            LOG.info { "gRPC Error Response: ${e.message}" }
        }
    }

    @Test
    fun `check grpc with unknown user`() {
        try {
            runBlocking {
                val request = grpcClient.request(address, ConferenceCheckGrpc.getCheckMethod()).coAwait()

                val res = request.send(checkRequest {
                        name = "Unknown User"
                    }).coAwait()
                if (res.status() == GrpcStatus.OK) {
                    val response = res.last().coAwait()
                    LOG.info { "gRPC Response: ${response?.name} = ${response?.role} (${response?.id})" }
                } else {
                    LOG.info { "Failed to find user: ${res.status().name} = ${res.statusMessage()}" }
                }
            }
        } catch (e: Throwable) {
            LOG.info { "gRPC Error Response: ${e.message}" }
        }
    }

    @Test
    fun `register grpc with new user`() {
        val registerClient = VertxConferenceRegGrpcClient(grpcClient, address)
        try {
            runBlocking {
                val response = registerClient.register(registerRequest {
                    name = "User 4"
                    role = "Manager"
                }).coAwait()
                LOG.info { "gRPC Response: ${response.name} = ${response.role} (${response.id})" }
            }
        } catch (e: Throwable) {
            LOG.info { "gRPC Error Response: ${e.message}" }
        }
    }

    @Test
    fun `register grpc with duplicate user`() {
        try {
            runBlocking {
                val request = grpcClient.request(address, ConferenceRegGrpc.getRegisterMethod()).coAwait()
                val res = request.send(registerRequest {
                    name = "User 1"
                    role = "Manager"
                }).coAwait()
                if (res.status() == GrpcStatus.OK) {
                    res.last().coAwait().also {
                        LOG.info { "gRPC Response: ${it.name} = ${it.role} (${it.id})" }
                    }
                } else {
                    LOG.info { "Failed: ${res.status().name} = ${res.statusMessage()}" }
                }
            }
        } catch (e: Throwable) {
            LOG.info { "gRPC Error Response: ${e.message}" }
        }
    }

    @Test
    fun `attendees http`() {
        runBlocking {
            data class Attendees(
                val attendees: List<AttendeesRepository.Attendee>
            )
            val response = webClient.getAbs(
                "http://localhost:8888/attendees"
            ).send().coAwait()

            response.bodyAsJsonObject()
                .mapTo(Attendees::class.java).attendees
                .map {
                    "${it.id} = ${it.name}, ${it.role}"
                }
        }
    }
}

