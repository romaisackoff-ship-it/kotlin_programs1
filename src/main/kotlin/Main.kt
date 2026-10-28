package com.riokartus

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.riokartus.Users.id
import io.ktor.client.request.request
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.util.Date
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

val JWT_SECRET = "твой_супер_секретный_ключ_ни_кому_не_говори"
val JWT_ISSUER = "http://0.0.0.0:8081/"
val JWT_AUDIENCE = "http://0.0.0.0:8081/api"
val JWT_REALM = "Access to API"
@Serializable
data class User(val id: Int, val name: String)

fun main() {
    connectToDatabase()   // подключаемся к БД
    setupDatabase()
    printUsers()

    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    @Serializable
    data class User(val id: Int, val name: String)

    @Serializable
    data class RegisterRequest(val name: String, val email: String, val password: String)

    @Serializable
    data class RegisterResponse(val id: Int, val email: String)

    @Serializable
    data class LoginRequest(val email: String, val password: String)

    @Serializable
    data class LoginResponse(val token: String)

    embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
        install(ContentNegotiation) { json() }

        install(StatusPages) {}
        install(Authentication) {
            jwt("auth-jwt") {
                realm = JWT_REALM
                verifier(
                    JWT.require(Algorithm.HMAC256(JWT_SECRET))
                        .withAudience(JWT_AUDIENCE)
                        .withIssuer(JWT_ISSUER)
                        .build()
                )
                validate { credential ->
                    if (credential.payload.getClaim("userId").asInt() != null) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }
        routing {
            get("/users/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val user = transaction {
                    Users.selectAll().where { Users.id eq id }.firstOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                call.respond(User(user[Users.id], user[Users.name]))
            }
            authenticate("auth-jwt") {

            get("/users") {
                val users = transaction {
                    Users.selectAll().map { row ->
                        User(row[Users.id], row[Users.name])
                    }
                }
                call.respond(users)
            }
            }
            post( "/login"){
                val request = call.receive<LoginRequest>()

                val userRow = transaction {
                    Users.selectAll().where { Users.email eq request.email }.firstOrNull()
                }
                if (userRow == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Неверный email или пароль")
                    return@post
                }
                val storedHash = userRow[Users.password]
                if (!BCrypt.checkpw(request.password, storedHash)) {
                    call.respond(HttpStatusCode.Unauthorized, "Неверный пароль")
                    return@post
                }
                val userId = userRow[Users.id]

                val token = JWT.create()
                    .withAudience(JWT_AUDIENCE)
                    .withIssuer(JWT_ISSUER)
                    .withClaim("userId", userId) // кладём id пользователя внутрь токена
                    .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // живёт 24 часа
                    .sign(Algorithm.HMAC256(JWT_SECRET))

                call.respond(LoginResponse(token))




            }
            post ("/register") {
                val poluchiti_data = call.receive<RegisterRequest>()

                val obrabotati_data = transaction {
                    Users.selectAll().where { Users.email eq poluchiti_data.email }.firstOrNull()
                }
                if (obrabotati_data != null){
                    call.respond(HttpStatusCode.Conflict, "Пользователь уже есть")
                    return@post
                }
                val hassPasword = hashPassword(poluchiti_data.password)

                val createdTable = transaction {
                    val id = Users.insert {
                        it[name] = poluchiti_data.name
                        it[email] = poluchiti_data.email
                        it[password] = hassPasword
                    } get Users.id
                    RegisterResponse(id, poluchiti_data.email)
                    }
                call.respond(HttpStatusCode.Created, createdTable)
                }
            authenticate("auth-jwt") {
            post("/users") {
                val newUser = call.receive<User>()
                val created = transaction {
                    val id = Users.insert {
                        it[name] = newUser.name
                        it[email] = "${newUser.name.lowercase()}@mail.ru"
                        it[password] = hashPassword("default123")
                    } get Users.id
                    User(id, newUser.name)
                }
                call.respond(created)
            }
            } // <--- ОДНА СКОБКА закрывает post
        } // <--- ОДНА СКОБКА закрывает routing
    }.start(wait = true) // <--- Закрывает embeddedServer
}