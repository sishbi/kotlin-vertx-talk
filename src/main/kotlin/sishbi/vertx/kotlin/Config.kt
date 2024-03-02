package sishbi.vertx.kotlin

data class Config(
    var dbHost: String = "localhost",
    var dbPort: Int = 5432,
    var dbDB: String = "test",
    var dbUser: String = "postgres",
    var dbPassword: String = "postgres",

) {
    val dbUrl: String get() = "jdbc:postgresql://${dbHost}:${dbPort}/${dbDB}"
}

val configuration = Config()
