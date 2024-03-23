package sishbi.vertx.kotlin

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineRouterSupport
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class HttpVerticle : CoroutineVerticle(), CoroutineRouterSupport {
    override suspend fun start() {
        val router = Router.router(vertx)
        router.get("/attendees").coRespond { ctx ->
            attendees(ctx)
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888)
            .coAwait()
    }

    private suspend fun attendees(ctx: RoutingContext) = try {
        val jsonBody = json {
            obj("attendees" to AttendeesRepository.findAttendees().map {
                json { obj(
                        "id" to it.id, "name" to it.name, "role" to it.role
                ) }
            })
        }

        ctx.response().putHeader("content-type", "application/json")
        ctx.end(jsonBody.encode()).coAwait()
    } catch (e: Exception) {
        ctx.response()
            .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
            .end("Error: ${e.message}")
            .coAwait()
    }
}
