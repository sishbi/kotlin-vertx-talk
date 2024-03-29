package sishbi.vertx.kotlin

import io.vertx.core.Future
import io.vertx.core.impl.NoStackTraceThrowable
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
import sishbi.vertx.grpc.ConferenceCheckGrpc.getCheckMethod
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

class ConferenceRegGrpcController
    : VertxConferenceRegGrpcServer.ConferenceRegApi {
    override fun register(request: RegisterRequest): Future<Attendee> =
        vertxFuture { registerUser(request) }

    private suspend fun registerUser(
        request: RegisterRequest
    ): Attendee = try {
        val attendee = AttendeesRepository.addAttendee(
            request.name, request.role
        ) ?: throw IllegalStateException("Failed for: ${request.name}")
        attendee {
            id = attendee.id.toString()
            name = attendee.name
            role = attendee.role
        }
    } catch (e: NoStackTraceThrowable) {
        throw IllegalStateException("Failed, ${e.message}")
    }
}

class ConferenceCheckGrpcController(
    private val grpcServer: GrpcServer
): CoroutineVerticle() {
    override suspend fun start() {
        grpcServer.callHandler(getCheckMethod()) {
            it.handler { request ->
                launch(vertx.dispatcher()) {
                    check(request, it.response())
                }
            }
            it.exceptionHandler { e ->
                launch(vertx.dispatcher()) {
                    LOG.error(e) { "Failed" }
                    it.response().status(GrpcStatus.INTERNAL).end().coAwait()
                }
            }
        }
    }

    private suspend fun check(
        request: CheckRequest,
        response: GrpcServerResponse<CheckRequest, Attendee>
    ) = AttendeesRepository.getAttendeeOrNull(request.name)?.let {
        response.end(attendee {
            id = it.id.toString()
            name = it.name
            role = it.role
        }).coAwait()
        return@let
    } ?: response.status(GrpcStatus.NOT_FOUND)
                 .statusMessage("attendee not found")
                 .end().coAwait()
}
