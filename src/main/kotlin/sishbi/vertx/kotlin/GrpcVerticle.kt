package sishbi.vertx.kotlin

import io.vertx.core.Future
import io.vertx.grpc.common.GrpcStatus
import io.vertx.grpc.server.GrpcServer
import io.vertx.grpc.server.GrpcServerResponse
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.coroutines.vertxFuture
import kotlinx.coroutines.launch
import mu.KotlinLogging
import sishbi.vertx.grpc.Attendee
import sishbi.vertx.grpc.CheckRequest
import sishbi.vertx.grpc.ConferenceCheckGrpc
import sishbi.vertx.grpc.RegisterRequest
import sishbi.vertx.grpc.VertxConferenceRegGrpcServer
import sishbi.vertx.grpc.attendee

private val LOG = KotlinLogging.logger {}

class GrpcVerticle : CoroutineVerticle() {
    override suspend fun start() {
        val grpc = GrpcServer.server(vertx)

        ConferenceRegGrpcController().bindAll(grpc)
        vertx.deployVerticle(ConferenceCheckGrpcController(grpc))

        val server = vertx.createHttpServer()
            .requestHandler(grpc)
            .listen(8889)
            .coAwait()
        LOG.info { "gRPC server listening on port ${server.actualPort()}" }
    }
}

class ConferenceRegGrpcController : VertxConferenceRegGrpcServer.ConferenceRegApi {
    override fun register(request: RegisterRequest): Future<Attendee> =
        vertxFuture { registerUser(request) }

    private suspend fun registerUser(request: RegisterRequest): Attendee {
        LOG.info { "Received registration gRPC request for: ${request.name} with role ${request.role}" }
        try {
            val attendee = AttendeesRepository.addAttendee(request.name, request.role)
                ?: throw IllegalStateException("Attendee registration failed for name: ${request.name}")
                    .also { LOG.error { "Attendee registration failed for name: ${request.name}" } }
            LOG.info { "Registered attendee: $attendee" }
            return attendee {
                id = attendee.id.toString()
                name = attendee.name
                role = attendee.role
            }
        } catch (e: Throwable) {
            LOG.error(e) { "Attendee registration failed for name: ${request.name}" }
            throw IllegalStateException("Attendee registration failed for name: ${request.name}")
        }
    }
}

class ConferenceCheckGrpcController(private val grpcServer: GrpcServer): CoroutineVerticle() {
    override suspend fun start() {
        grpcServer.callHandler(ConferenceCheckGrpc.getCheckMethod()) {
            it.handler { request ->
                launch(vertx.dispatcher()) {
                    check(request, it.response())
                }
            }
        }
    }

    private suspend fun check(request: CheckRequest, response: GrpcServerResponse<CheckRequest, Attendee>) {
        LOG.info { "Received check gRPC request for: ${request.name}" }
        val attendeeOrNull = AttendeesRepository.getAttendeeOrNull(request.name)
        if (attendeeOrNull == null) {
            LOG.info { "Attendee not found: ${request.name}" }
            response.status(GrpcStatus.NOT_FOUND).statusMessage("attendee not found").end().coAwait()
        } else {
            LOG.info { "Attendee found: $attendeeOrNull" }
            response.end(attendee {
                id = attendeeOrNull.id.toString()
                name = attendeeOrNull.name
                role = attendeeOrNull.role
            })
        }
    }
}
