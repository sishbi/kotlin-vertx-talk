package sishbi.vertx.kotlin

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineRouterSupport
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class HttpVerticle : CoroutineVerticle(), CoroutineRouterSupport {
    override suspend fun start() {
        val router = Router.router(vertx)

        router.get("/hello").coRespond { ctx -> hello(ctx) }

        val server = vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888)
            .coAwait()
        LOG.info { "HTTP server listening on port ${server.actualPort()}" }
    }

    private suspend fun hello(ctx: RoutingContext) {
        LOG.info { "Received hello HTTP request" }
        ctx.response().putHeader("content-type", "text/plain")
        val users = UserQuery.findUsers().joinToString { "$it" }
        ctx.end("Hello from Vert.x HTTP!\n  users: $users")
            .coAwait()
    }
}
