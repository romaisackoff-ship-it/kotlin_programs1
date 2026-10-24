package com.riokartus

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun connectToDatabase() {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5433/mydb"
        username = "postgres"
        password = "secret"
        driverClassName = "org.postgresql.Driver"

        println("Пароль: [$password]")
        println("Юзер: [$username]")
        println("URL: [$jdbcUrl]")
    }
    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)
}
fun printUsers() {
    transaction {
        Users.selectAll().forEach { row ->
            println("${row[Users.id]}: ${row[Users.name]} (${row[Users.email]})")

        }
    }
}
fun setupDatabase() {
    transaction {
        SchemaUtils.create(Users)
    }
}