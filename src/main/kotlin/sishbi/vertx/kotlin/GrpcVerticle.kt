package sishbi.vertx.kotlin

import io.vertx.core.Future
import io.vertx.grpc.server.GrpcServer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import mu.KotlinLogging
import sishbi.vertx.grpc.HelloReply
import sishbi.vertx.grpc.HelloRequest
import sishbi.vertx.grpc.VertxGreeterGrpcServer
import sishbi.vertx.grpc.helloReply

private val LOG = KotlinLogging.logger {}

class GrpcVerticle: CoroutineVerticle() {
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

class GreeterController: VertxGreeterGrpcServer.GreeterApi {
    override fun sayHello(request: HelloRequest): Future<HelloReply> {
        LOG.info { "Received hello gRPC request from: ${request.name}" }
        val reply = helloReply {
            message = "Hello ${request.name} from Vert.x gRPC!"
        }
        return Future.succeededFuture(reply)
    }
}
