package me.kbrewster.eventbus

import me.kbrewster.eventbus.collection.ConcurrentSubscriberArrayList
import me.kbrewster.eventbus.collection.SubscriberArrayList
import me.kbrewster.eventbus.exception.ExceptionHandler
import me.kbrewster.eventbus.invokers.InvokerType
import me.kbrewster.eventbus.invokers.ReflectionInvoker
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Subscribe(val priority: Int = 0)

class EventBus @JvmOverloads constructor(
    private val invokerType: InvokerType = ReflectionInvoker(),
    private val exceptionHandler: ExceptionHandler = ExceptionHandler { _, exception ->
        throw exception
    },
    private val threadSaftey: Boolean = true
) {
    class Subscriber(val obj: Any, val priority: Int, private val invoker: InvokerType.SubscriberMethod?) {
        @Throws(Exception::class)
        operator fun invoke(arg: Any?) {
            invoker!!.invoke(arg)
        }

        override fun equals(other: Any?): Boolean {
            return other === this || other.hashCode() == this.hashCode()
        }

        override fun hashCode(): Int {
            return obj.hashCode()
        }
    }

    private val subscribers: AbstractMap<Class<*>, MutableList<Subscriber>> =
        if (threadSaftey) ConcurrentHashMap() else HashMap()

    /**
     * Subscribes all `@Subscribe` annotated methods in the object and its superclasses.
     * All @Subscribe methods must be final.
     */
    fun register(obj: Any) {
        var currentClass: Class<*>? = obj.javaClass
        while (currentClass != null && currentClass != Any::class.java) {

            for (method in currentClass.declaredMethods) {
                val sub: Subscribe = method.getAnnotation(Subscribe::class.java) ?: continue

                // verification
                val parameterClazz = method.parameterTypes[0]
                when {
                    method.parameterCount != 1 -> throw IllegalArgumentException("Subscribed method must only have one parameter.")
                    method.returnType != Void.TYPE -> throw IllegalArgumentException("Subscribed method must be of type 'Void'. ")
                    parameterClazz.isPrimitive -> throw IllegalArgumentException("Cannot subscribe method to a primitive.")
                    parameterClazz.modifiers and (Modifier.ABSTRACT or Modifier.INTERFACE) != 0 -> throw IllegalArgumentException("Cannot subscribe method to a polymorphic class.")
                    !Modifier.isFinal(method.modifiers) -> throw IllegalArgumentException("Cannot subscribe non-final method: ${method.name}")
                }

                val subscriberMethod = invokerType.setup(obj, currentClass, parameterClazz, method)
                val subscriber = Subscriber(obj, sub.priority, subscriberMethod)
                subscribers.getOrPut(parameterClazz) {
                    if (threadSaftey) ConcurrentSubscriberArrayList() else SubscriberArrayList()
                }.add(subscriber)
            }

            currentClass = currentClass.superclass
        }
    }

    /**
     * Unsubscribes all `@Subscribe` methods in the object and its superclasses.
     */
    fun unregister(obj: Any) {
        var currentClass: Class<*>? = obj.javaClass
        while (currentClass != null && currentClass != Any::class.java) {
            for (method in currentClass.declaredMethods) {
                if (method.getAnnotation(Subscribe::class.java) == null)
                    continue
                subscribers[method.parameterTypes[0]]?.remove(Subscriber(obj, -1, null))
            }
            currentClass = currentClass.superclass
        }
    }

    /**
     * Posts the event to all subscribed listeners.
     */
    fun post(event: Any) {
        val events = getSubscribedEvents(event.javaClass) ?: return
        events.forEach {
            try {
                it.invoke(event)
            } catch (ex: Exception) {
                exceptionHandler.handle(event, ex)
            }
        }
    }

    /**
     * Posts an event using a supplier. The event is only constructed if there are active subscribers.
     */
    inline fun <reified T> post(supplier: () -> T) {
        val events = getSubscribedEvents(T::class.java) ?: return
        val event = supplier()
        events.forEach {
            it.invoke(event)
        }
    }

    fun getSubscribedEvents(clazz: Class<*>) = subscribers[clazz]?.takeIf { it.isNotEmpty() }
}
