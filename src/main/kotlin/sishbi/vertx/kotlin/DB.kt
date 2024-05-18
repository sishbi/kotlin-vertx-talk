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
    companion object {
        lateinit var pool: Pool
    }

    init {
        pool = PgBuilder.pool()
            .with(poolOptionsOf(maxSize = 5))
            .connectingTo(pgConnectOptionsOf(
                host = config.dbHost,
                port = config.dbPort,
                database = config.dbDB,
                user = config.dbUser,
                password = config.dbPassword
            ))
            .using(vertx).build()
    }

    suspend fun initDB(): Boolean = withContext(Dispatchers.IO) {
        try {
            Flyway
                .configure()
                .dataSource(config.dbUrl, config.dbUser, config.dbPassword)
                .load()
                .migrate()
                .success
        } catch (e: Exception) {
            LOG.error(e) { "Failed to migrate DB" }
            throw e
        }
    }

}
