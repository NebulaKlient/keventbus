package me.kbrewster.eventbus

import me.kbrewster.eventbus.exception.ExceptionHandler
import me.kbrewster.eventbus.invokers.InvokerType
import me.kbrewster.eventbus.invokers.ReflectionInvoker

fun eventbus(lambda: EventBusBuilder.() -> Unit): EventBus {
    return EventBusBuilder().apply(lambda).build()
}

class EventBusBuilder {
    var invokerType: InvokerType = ReflectionInvoker()
    var exceptionHandler: ExceptionHandler = ExceptionHandler { _, exception ->
        throw exception
    }
    var threadSaftey = false

    fun invoker(lambda: () -> InvokerType) {
        this.invokerType = lambda()
    }

    fun threadSaftey(lambda: () -> Boolean) {
        this.threadSaftey = lambda()
    }

    inline fun exceptionHandler(crossinline lambda: (Any, Exception) -> Unit) {
        this.exceptionHandler = ExceptionHandler { event, exception ->
            lambda(event, exception)
        }
    }

    fun build() = EventBus(this.invokerType, this.exceptionHandler, this.threadSaftey)

}