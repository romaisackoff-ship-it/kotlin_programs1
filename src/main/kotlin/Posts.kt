

package com.riokartus

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Posts : Table("posts") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val content = text("content")
    val authorId = integer("author_id").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}