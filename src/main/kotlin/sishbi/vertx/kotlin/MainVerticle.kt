package sishbi.vertx.kotlin

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineRouterSupport
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import kotlin.system.exitProcess


private val LOG = mu.KotlinLogging.logger {}

class MainVerticle : CoroutineVerticle(), CoroutineRouterSupport {
    override suspend fun start() {
        initDB()
        DB(vertx)
        vertx.deployVerticle(HttpVerticle()).coAwait()
        vertx.deployVerticle(GrpcVerticle()).coAwait()
    }

    override suspend fun stop() {
        DB.pool.close()
    }

    private suspend fun initDB() {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                Flyway
                    .configure()
                    .dataSource(configuration.dbUrl, configuration.dbUser, configuration.dbPassword)
                    .load()
                    .migrate()
            }.onFailure {
                LOG.error(it) { "Failed to migrate DB" }
                throw it
            }
        }
    }
}

fun main() {
    LOG.info { "Started" }
    Vertx.vertx().deployVerticle(MainVerticle())
        .onFailure {
            LOG.error(it) { "Failed" }
            exitProcess(-1)
        }
}
