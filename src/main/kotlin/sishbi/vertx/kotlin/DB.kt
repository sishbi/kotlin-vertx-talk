package sishbi.vertx.kotlin

import io.vertx.core.Vertx
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.Pool

class DB(vertx: Vertx) {
    init {
        pool = PgBuilder
            .pool()
            .with(poolOptionsOf(maxSize = 5))
            .connectingTo(pgConnectOptionsOf(
                host = configuration.dbHost,
                port = configuration.dbPort,
                database = configuration.dbDB,
                user = configuration.dbUser,
                password = configuration.dbPassword))
            .using(vertx)
            .build()
    }

    companion object {
        lateinit var pool : Pool
    }
}
