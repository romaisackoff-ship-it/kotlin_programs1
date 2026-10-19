package com.riokartus

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

suspend fun checkStock(orderId: Int): Boolean {
    delay(500)
    return orderId % 2 == 0
}

suspend fun checkPayment(orderId: Int): Boolean {
    delay(800)
    return orderId > 0
}

suspend fun checkAddress(orderId: Int): Boolean {
    delay(300)
    return orderId != 13
}

suspend fun processOrder(orderId: Int): String = coroutineScope {
    val a = async { checkStock(orderId) }
    val b = async { checkPayment(orderId) }
    val v = async { checkAddress(orderId) }

    val stock = a.await()
    val payment = b.await()
    val address = v.await()

    if (stock && payment && address) {
        "Заказ $orderId: OK"
    } else {
        "Заказ $orderId: FAIL (stock=$stock, payment=$payment, address=$address)"
    }
}

fun main() = runBlocking {
    val start = System.currentTimeMillis()

    println(processOrder(4))
    println(processOrder(13))
    println(processOrder(-1))

    val end = System.currentTimeMillis()
    println("Общее время: ${end - start} мс")
}