package sishbi.vertx.kotlin

import io.vertx.core.Vertx
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.Pool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.flywaydb.core.Flyway

private val LOG = KotlinLogging.logger {}

class DB(vertx: Vertx) {
    init {
        pool = PgBuilder
            .pool()
            .with(poolOptionsOf(maxSize = 5))
            .connectingTo(
                pgConnectOptionsOf(
                    host = configuration.dbHost,
                    port = configuration.dbPort,
                    database = configuration.dbDB,
                    user = configuration.dbUser,
                    password = configuration.dbPassword
                )
            )
            .using(vertx)
            .build()
    }

    suspend fun initDB(): Boolean = withContext(Dispatchers.IO) {
        try {
            Flyway
                .configure()
                .dataSource(configuration.dbUrl, configuration.dbUser, configuration.dbPassword)
                .load()
                .migrate()
                .success
        } catch (e: Exception) {
            LOG.error(e) { "Failed to migrate DB" }
            throw e
        }
    }

    companion object {
        lateinit var pool: Pool

    }
}
