package sishbi.vertx.kotlin

import io.vertx.core.Future
import io.vertx.grpc.server.GrpcServer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.vertxFuture
import mu.KotlinLogging
import sishbi.vertx.grpc.RegisterReply
import sishbi.vertx.grpc.RegisterRequest
import sishbi.vertx.grpc.VertxConferenceGrpcServer
import sishbi.vertx.grpc.registerReply

private val LOG = KotlinLogging.logger {}

class GrpcVerticle : CoroutineVerticle() {
    override suspend fun start() {
        val grpc = GrpcServer.server(vertx)

        ConferenceGrpcController().bindAll(grpc)

        val server = vertx.createHttpServer()
            .requestHandler(grpc)
            .listen(8889)
            .coAwait()
        LOG.info { "gRPC server listening on port ${server.actualPort()}" }
    }
}

class ConferenceGrpcController : VertxConferenceGrpcServer.ConferenceApi {
    override fun register(request: RegisterRequest): Future<RegisterReply> =
        vertxFuture { registerUser(request) }

    private suspend fun registerUser(request: RegisterRequest): RegisterReply {
        LOG.info { "Received registration gRPC request for: ${request.name}" }
        val attendee = AttendeesRepository.getRegistrationNumberOrNull(request.name)
            ?: throw IllegalStateException("Attendee not found for name: ${request.name}")

        return registerReply {
            name = attendee.name
            registrationNumber = attendee.regNumber
        }
    }
}
