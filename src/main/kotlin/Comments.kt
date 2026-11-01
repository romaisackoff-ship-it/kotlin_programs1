package com.riokartus

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Comments : Table("comments") {
    val id = integer("id").autoIncrement()
    val content = text("content")
    val postId = integer("post_id").references(Posts.id)
    val authorId = integer("author_id").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}