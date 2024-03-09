package sishbi.vertx.kotlin

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.vertxFuture
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import org.checkerframework.checker.units.qual.t
import java.util.UUID

object AttendeesRepository {
    suspend fun getAttendeeOrNull(name: String): Attendee? =
        DB.pool.withConnection { conn ->
            vertxFuture {
                conn.prepare("select * from public.conference_attendees where name = $1").coAwait()
                    .query().execute(Tuple.of(name)).coAwait()
                    .map { it.toAttendee() }.firstOrNull()
            }
        }.coAwait()

    suspend fun findAttendees(): List<Attendee> =
        DB.pool.withConnection { conn ->
            vertxFuture {
                conn.query("select * from public.conference_attendees").execute().coAwait()
                    .map { it.toAttendee() }
            }
        }.coAwait()

    suspend fun addAttendee(name: String, role: String): Attendee? =
        DB.pool.withTransaction { conn ->
            vertxFuture {
                conn.prepare("insert into public.conference_attendees(name, role) values ($1, $2) returning *").coAwait()
                    .query().execute(Tuple.of(name, role)).coAwait()
                    .map { it.toAttendee() }.firstOrNull()
            }
        }.coAwait()

    private fun Row.toAttendee(): Attendee =
        Attendee(
            id = getUUID("id"),
            name = getString("name"),
            role = getString("role")
        )
}

data class Attendee(
    val id: UUID,
    val name: String,
    val role: String
)
