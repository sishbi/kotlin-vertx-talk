package sishbi.vertx.kotlin

import io.vertx.core.Future
import io.vertx.grpc.server.GrpcServer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.vertxFuture
import mu.KotlinLogging
import sishbi.vertx.grpc.HelloReply
import sishbi.vertx.grpc.HelloRequest
import sishbi.vertx.grpc.VertxGreeterGrpcServer
import sishbi.vertx.grpc.helloReply

private val LOG = KotlinLogging.logger {}

class GrpcVerticle : CoroutineVerticle() {
    override suspend fun start() {
        val grpc = GrpcServer.server(vertx)

        GreeterController().bindAll(grpc)

        val server = vertx.createHttpServer()
            .requestHandler(grpc)
            .listen(8889)
            .coAwait()
        LOG.info { "gRPC server listening on port ${server.actualPort()}" }
    }
}

class GreeterController : VertxGreeterGrpcServer.GreeterApi {
    override fun sayHello(request: HelloRequest): Future<HelloReply> =
        vertxFuture { hello(request) }

    private suspend fun hello(request: HelloRequest): HelloReply {
        LOG.info { "Received hello gRPC request from: ${request.name}" }
        val user = UserQuery.getUserOrNull(request.name)
        val userDetails = user?.let { "Name:${it.name} + Age:${it.age}" } ?: "unknown user"
        return helloReply {
            message = "Hello $userDetails from Vert.x gRPC!"
        }
    }
}
