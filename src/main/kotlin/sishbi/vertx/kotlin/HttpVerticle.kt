package sishbi.vertx.kotlin

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineRouterSupport
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class HttpVerticle : CoroutineVerticle(), CoroutineRouterSupport {
    override suspend fun start() {
        val router = Router.router(vertx)

        //TODO: talk about vertx dispatcher here
        router.get("/attendees").coRespond(MDCContext()) { ctx -> attendees(ctx) }

        val server = vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888)
            .coAwait()
        LOG.info { "HTTP server listening on port ${server.actualPort()}" }
    }

    private suspend fun attendees(ctx: RoutingContext) {
        LOG.info { "Received attendees HTTP request" }
        ctx.response().putHeader("content-type", "application/json")
        val users = AttendeesRepository.findAttendees().map { json { obj(
            "name" to it.name,
            "regNumber" to it.regNumber
        ) } }
        ctx.end(jsonArrayOf(users).encode())
            .coAwait()
    }
}
