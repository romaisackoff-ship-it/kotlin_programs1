package com.riokartus

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


suspend fun fetchUsers(): String {
    delay(1000)
    return "пользователи "
}
suspend fun fetchPosts(): String{
    delay(1500)
    return "посты"
}
suspend fun fetchComments(): String {
    delay(800)
    return "комментарии"
}
fun main() = runBlocking {
    val start = System.currentTimeMillis()
    val a = async { fetchUsers() }
    val b = async { fetchPosts() }
    val v = async { fetchComments() }


    val resultA = a.await()
    val resultB = b.await()
    val resultV = v.await()
    val end = System.currentTimeMillis()


    println("Пользователи: $resultA")
    println("Посты: $resultB")
    println("Комментарии: $resultV")
    println(end - start)
}