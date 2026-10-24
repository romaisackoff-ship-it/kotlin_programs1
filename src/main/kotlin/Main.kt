package com.riokartus

import com.riokartus.Users.id
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
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

@Serializable
data class User(val id: Int, val name: String)

fun main() {
    connectToDatabase()   // подключаемся к БД
    setupDatabase()
    printUsers()

    embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
        install(ContentNegotiation) { json() }
        install(StatusPages) {}
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

            get("/users") {
                val users = transaction {
                    Users.selectAll().map { row ->
                        User(row[Users.id], row[Users.name])
                    }
                }
                call.respond(users)
            }
            delete ("/users/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
                val deleted = transaction {
                    Users.deleteWhere { Users.id eq id }
                }
                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                call.respond(HttpStatusCode.NoContent)
            }
            put("/users/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }

                val updated = call.receive<User>()

                val updatedRows = transaction {
                    Users.update({ Users.id eq id }) {
                        it[name] = updated.name
                    }
                }

                if (updatedRows == 0) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                call.respond(User(id, updated.name))
            }

            // 2. POST /users — в БД
            post("/users") {
                val newUser = call.receive<User>() // приходит id: 0, name: "Иван"
                val created = transaction {
                    val id = Users.insert {
                        it[name] = newUser.name
                        it[email] = "${newUser.name.lowercase()}@mail.ru" // временно
                    } get Users.id
                    User(id, newUser.name)
                }
                call.respond(created)
            }}


    }.start(wait = true)
}