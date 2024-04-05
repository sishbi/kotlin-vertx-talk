package sishbi.vertx.kotlin

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import kotlin.system.exitProcess


private val LOG = mu.KotlinLogging.logger {}

class MainVerticle : CoroutineVerticle() {
    override suspend fun start() {
        DB(vertx).initDB()
        vertx.deployVerticle(WebVerticle()).coAwait()
        vertx.deployVerticle(GrpcVerticle()).coAwait()
    }
}

fun main() {
    LOG.info { "Started" }
    Vertx.vertx().deployVerticle(MainVerticle())
        .onFailure { e ->
            LOG.error(e) { "Failed" }
            exitProcess(-1)
        }
}
