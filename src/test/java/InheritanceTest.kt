import me.kbrewster.eventbus.Subscribe
import me.kbrewster.eventbus.eventbus
import me.kbrewster.eventbus.invokers.LMFInvoker
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class TestEvent(val value: String)
class AnotherEvent(val value: Int)

open class ParentListener {
    var parentCalled = false
    var parentValue = ""

    @Subscribe
    private fun onTestEvent(event: TestEvent) {
        parentCalled = true
        parentValue = event.value
    }
}

class ChildListener : ParentListener() {
    var childCalled = false
    var childValue = 0

    @Subscribe
    private fun onAnotherEvent(event: AnotherEvent) {
        childCalled = true
        childValue = event.value
    }
}

open class BadParentListener {
    @Subscribe
    open fun nonFinalMethod(event: TestEvent) {
    }
}

class BadChildListener : BadParentListener()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InheritanceTest {

    @Test
    fun `should register and call parent methods`() {
        val eventBus = eventbus {
            invoker { LMFInvoker() }
            threadSaftey { false }
        }

        val listener = ChildListener()
        eventBus.register(listener)

        eventBus.post(TestEvent("from parent"))

        assertTrue(listener.parentCalled, "Parent method should be called")
        assertEquals("from parent", listener.parentValue)

        eventBus.unregister(listener)
    }

    @Test
    fun `should register and call child methods`() {
        val eventBus = eventbus {
            invoker { LMFInvoker() }
            threadSaftey { false }
        }

        val listener = ChildListener()
        eventBus.register(listener)

        eventBus.post(AnotherEvent(42))

        assertTrue(listener.childCalled, "Child method should be called")
        assertEquals(42, listener.childValue)

        eventBus.unregister(listener)
    }

    @Test
    fun `should register both parent and child methods`() {
        val eventBus = eventbus {
            invoker { LMFInvoker() }
            threadSaftey { false }
        }

        val listener = ChildListener()
        eventBus.register(listener)

        assertNotNull(eventBus.getSubscribedEvents(TestEvent::class.java))
        assertNotNull(eventBus.getSubscribedEvents(AnotherEvent::class.java))

        eventBus.post(TestEvent("parent event"))
        eventBus.post(AnotherEvent(123))

        assertTrue(listener.parentCalled)
        assertEquals("parent event", listener.parentValue)
        assertTrue(listener.childCalled)
        assertEquals(123, listener.childValue)

        eventBus.unregister(listener)
    }

    @Test
    fun `should unregister both parent and child methods`() {
        val eventBus = eventbus {
            invoker { LMFInvoker() }
            threadSaftey { false }
        }

        val listener = ChildListener()
        eventBus.register(listener)
        eventBus.unregister(listener)

        assertNull(eventBus.getSubscribedEvents(TestEvent::class.java))
        assertNull(eventBus.getSubscribedEvents(AnotherEvent::class.java))

        listener.parentCalled = false
        listener.childCalled = false

        eventBus.post(TestEvent("should not trigger"))
        eventBus.post(AnotherEvent(99))

        assertFalse(listener.parentCalled, "Parent method should not be called after unregister")
        assertFalse(listener.childCalled, "Child method should not be called after unregister")
    }

    @Test
    fun `should throw exception for non-final methods`() {
        val eventBus = eventbus {
            invoker { LMFInvoker() }
            threadSaftey { false }
        }

        val listener = BadChildListener()

        val exception = assertThrows<IllegalArgumentException> {
            eventBus.register(listener)
        }

        assertTrue(
            exception.message?.contains("non-final") == true,
            "Exception should mention non-final methods: ${exception.message}"
        )
    }
}