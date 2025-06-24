package me.kbrewster.eventbus.exception

fun interface ExceptionHandler {
    fun handle(event: Any, exception: Exception)
}
