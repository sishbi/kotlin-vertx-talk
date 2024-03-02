package sishbi.vertx.kotlin

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Tuple

object UserQuery {
    suspend fun getUserOrNull(name: String): MyUser? {
        val conn = DB.pool.connection.coAwait()
        return try {
            conn.prepare("select * from public.test_users where name = $1").coAwait()
                .query().execute(Tuple.of(name)).coAwait()
                .map {
                    MyUser(
                        name = it.getString("name"),
                        age = it.getInteger("age")
                    )
                }.firstOrNull()
        } finally {
            conn.close().coAwait()
        }
    }

    suspend fun findUsers(): List<MyUser> {
        val conn = DB.pool.connection.coAwait()
        return try {
            conn.query("select * from public.test_users").execute().coAwait()
                .map {
                    MyUser(
                        name = it.getString("name"),
                        age = it.getInteger("age")
                    )
                }
        } finally {
            conn.close().coAwait()
        }
    }
}

data class MyUser(
    val name: String,
    val age: Int
)