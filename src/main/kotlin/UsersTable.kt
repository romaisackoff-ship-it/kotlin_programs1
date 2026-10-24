package com.riokartus

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val email = varchar("email", 255).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}