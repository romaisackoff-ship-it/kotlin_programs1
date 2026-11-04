package com.riokartus

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
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
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

val JWT_SECRET = "твой_супер_секретный_ключ_ни_кому_не_говори"
val JWT_ISSUER = "http://0.0.0.0:8081/"
val JWT_AUDIENCE = "http://0.0.0.0:8081/api"
val JWT_REALM = "Access to API"

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
@Serializable
data class PostRequest(val title: String, val content: String)

@Serializable
data class PostResponse(
    val id: Int,
    val title: String,
    val content: String,
    val authorId: Int,
    val createdAt: String // Для простоты строка
)
@Serializable
data class CommentRequest(val content: String)

@Serializable
data class CommentResponse(
    val id: Int,
    val content: String,
    val postId: Int,
    val authorId: Int,
    val createdAt: String
)
fun hashPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt())

fun main() {
    connectToDatabase()
    setupDatabase()
    printUsers()

    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            println("ОШИБКА: ${cause.message}")
            cause.printStackTrace()
            call.respondText(
                text = "Ошибка сервера: ${cause.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

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
        post("/login") {
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
                .withClaim("userId", userId)
                .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                .sign(Algorithm.HMAC256(JWT_SECRET))
            call.respond(LoginResponse(token))
        }
        post("/register") {
            val data = call.receive<RegisterRequest>()
            val existing = transaction {
                Users.selectAll().where { Users.email eq data.email }.firstOrNull()
            }
            if (existing != null) {
                call.respond(HttpStatusCode.Conflict, "Пользователь уже есть")
                return@post
            }
            val hashed = hashPassword(data.password)
            val created = transaction {
                val id = Users.insert {
                    it[name] = data.name
                    it[email] = data.email
                    it[password] = hashed
                } get Users.id
                RegisterResponse(id, data.email)
            }
            call.respond(HttpStatusCode.Created, created)
        }
        get("/posts") {
            val posts = transaction {
                Posts.selectAll().map { row ->
                    PostResponse(
                        id = row[Posts.id],
                        title = row[Posts.title],
                        content = row[Posts.content],
                        authorId = row[Posts.authorId],
                        createdAt = row[Posts.createdAt].toString()
                    )
                }
            }
            call.respond(posts)
        }
        get("/posts/{id}/comments") {
            val postId = call.parameters["id"]?.toIntOrNull()
            if (postId == null) {
                call.respond(HttpStatusCode.BadRequest, "Некорректный id")
                return@get
            }
            val postExists = transaction {
                Posts.selectAll().where { Posts.id eq postId }.firstOrNull()
            }
            if (postExists == null) {
                call.respond(HttpStatusCode.NotFound, "Пост не найден")
                return@get
            }

            val comments = transaction {
                Comments.selectAll().where { Comments.postId eq postId }.map { row ->
                    CommentResponse(
                        id = row[Comments.id],
                        content = row[Comments.content],
                        postId = row[Comments.postId],
                        authorId = row[Comments.authorId],
                        createdAt = row[Comments.createdAt].toString()
                    )
                }
            }
            call.respond(comments)
        }
        get("/posts/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Некорректный id")
                return@get
            }
            val post = transaction {
                Posts.selectAll().where { Posts.id eq id }.firstOrNull()
            }
            if (post == null) {
                call.respond(HttpStatusCode.NotFound, "Пост не найден")
            } else {
                call.respond(
                    PostResponse(
                        id = post[Posts.id],
                        title = post[Posts.title],
                        content = post[Posts.content],
                        authorId = post[Posts.authorId],
                        createdAt = post[Posts.createdAt].toString()
                    )
                )
            }
        }
        authenticate("auth-jwt") {
            delete("/comments/{id}") {
                val commentId = call.parameters["id"]?.toIntOrNull()
                if (commentId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Некорректный id")
                    return@delete
                }
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal!!.payload.getClaim("userId").asInt()
                val comment = transaction {
                    Comments.selectAll().where { Comments.id eq commentId }.firstOrNull()
                }
                if (comment == null) {
                    call.respond(HttpStatusCode.NotFound, "Комментарий не найден")
                    return@delete
                }
                val authorId = comment[Comments.authorId]
                if (authorId != currentUserId) {
                    call.respond(HttpStatusCode.Forbidden, "Можно удалять только свои комментарии")
                    return@delete
                }
                transaction {
                    Comments.deleteWhere { Comments.id eq commentId }
                }
                call.respond(HttpStatusCode.NoContent)
            }

            post("/posts") {
                val request = call.receive<PostRequest>()
                val principal = call.principal<JWTPrincipal>()
                val authorId = principal!!.payload.getClaim("userId").asInt()

                val post = transaction {
                    val id = Posts.insert {
                        it[title] = request.title
                        it[content] = request.content
                        it[Posts.authorId] = authorId
                    } get Posts.id

                    PostResponse(
                        id = id,
                        title = request.title,
                        content = request.content,
                        authorId = authorId,
                        createdAt = java.time.LocalDateTime.now().toString()
                    )
                }
                call.respond(HttpStatusCode.Created, post)
            }

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
                    Users.selectAll().map { row -> User(row[Users.id], row[Users.name]) }
                }
                call.respond(users)
            }

            get("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val userRow = transaction {
                    Users.selectAll().where { Users.id eq userId }.firstOrNull()
                }
                if (userRow != null) {
                    call.respond(User(userRow[Users.id], userRow[Users.name]))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Пользователь не найден")
                }
            }
            post("/posts/{id}/comments") {
                val postId = call.parameters["id"]?.toIntOrNull()
                if (postId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Некорректный id")
                    return@post
                }
                val postExists = transaction {
                    Posts.selectAll().where { Posts.id eq postId }.firstOrNull()
                }
                if (postExists == null) {
                    call.respond(HttpStatusCode.NotFound, "Пост не найден")
                    return@post
                }

                val request = call.receive<CommentRequest>()
                val principal = call.principal<JWTPrincipal>()
                val authorId = principal!!.payload.getClaim("userId").asInt()

                val comment = transaction {
                    val id = Comments.insert {
                        it[content] = request.content
                        it[Comments.postId] = postId
                        it[Comments.authorId] = authorId
                    } get Comments.id

                    CommentResponse(
                        id = id,
                        content = request.content,
                        postId = postId,
                        authorId = authorId,
                        createdAt = java.time.LocalDateTime.now().toString()
                    )
                }
                call.respond(HttpStatusCode.Created, comment)
            }
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
        }
    }
}