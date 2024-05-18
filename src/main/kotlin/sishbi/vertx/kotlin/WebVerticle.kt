package sishbi.vertx.kotlin

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineRouterSupport
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgException
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class WebVerticle : CoroutineVerticle(), CoroutineRouterSupport {
    override suspend fun start() {
        val router = Router.router(vertx)
        router.get("/attendees").coRespond { ctx ->
            attendees(ctx)
        }
        router.coErrorHandler(500) { ctx ->
            LOG.error(ctx.failure()) { "Uncaught exception" }
            if (!ctx.response().ended()) {
                ctx.end("${ctx.failure().message}").coAwait()
            } else {
                LOG.error { "Response has already been written" }
            }
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888)
            .coAwait()
    }

    private suspend fun attendees(ctx: RoutingContext) = try {
        val jsonBody = getAttendeesJson()
        ctx.response().putHeader("content-type", "application/json")
        ctx.response().end(jsonBody.encode()).coAwait()
    } catch (e: NoStackTraceThrowable) {
        LOG.error(e) { "Error finding attendees" }
        ctx.response()
            .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
            .end("Error: ${e.message}")
            .coAwait()
    }

    private suspend fun getAttendeesJson() = json {
        obj("attendees" to array(
            AttendeesRepository.findAttendees().map {
                obj(
                    "id" to it.id,
                    "name" to it.name,
                    "role" to it.role
                )
            }
        ))
    }
}
