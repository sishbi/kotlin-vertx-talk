package sishbi.vertx.kotlin

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

object AttendeesRepository {
    suspend fun getRegistrationNumberOrNull(name: String): MyUser? {
        lateinit var conn: SqlConnection
        return try {
            conn = DB.pool.connection.coAwait()
            conn.prepare("select * from public.conference_attendees where name = $1").coAwait()
                .query().execute(Tuple.of(name)).coAwait()
                .map {
                    MyUser(
                        name = it.getString("name"),
                        regNumber = it.getInteger("reg_number")
                    )
                }.firstOrNull()
        } finally {
            conn.close().coAwait()
        }
    }

    suspend fun findAttendees(): List<MyUser> {
        val conn = DB.pool.connection.coAwait()
        return try {
            conn.query("select * from public.conference_attendees").execute().coAwait()
                .map {
                    MyUser(
                        name = it.getString("name"),
                        regNumber = it.getInteger("reg_number")
                    )
                }
        } finally {
            conn.close().coAwait()
        }
    }
}

data class MyUser(
    val name: String,
    val regNumber: Int
)
