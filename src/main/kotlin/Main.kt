package com.riokartus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

fun task1() = runBlocking {
    val potok1 = launch(Dispatchers.IO) {
        println("Имя потока ${Thread.currentThread().name}")
    }
    val potok2 = launch(Dispatchers.Default) {
        println("Имя потока ${Thread.currentThread().name}")
    }
    potok1.join()
    potok2.join()
}

suspend fun fetchData(): String = withContext(Dispatchers.IO) {
    delay(1000)
    "данные"
}

fun task3() {
    runBlocking {
        val job = launch {
            var i = 0
            while (isActive) {
                println("Итерация ${i}")
                delay(100)
                i++
            }
        }
        delay(350)
        job.cancel()
        println("Отменено")

    }
}

suspend fun task4() = supervisorScope {
    launch {
        delay(100)
        throw RuntimeException("Упала 1")
    }
    launch {
        delay(200)
        println("Готово")
    }
}


fun main(): Unit = runBlocking {
    task1()
    println(fetchData())
    task3()
    try {
        task4()
    } catch (e: Exception) {
        println("Поймали: ${e.message}")
    }

}
