# KEventBus
JVM Eventbus focused on thread-safety and performance. Including update to a newer kotlin version, race condition fix, and superclass subscriber inheritance.

Nebla Klint private technologia

## Registering
**Kotlin**
```kotlin
// Create eventbus
private val eventBus = eventbus {
    invoker { LMFInvoker() }
    exceptionHandler { exception -> println("Error occurred in method: ${exception.message}")  }
}

// Method you would like to subscribe to an event
// Param #1 is MessagedReceivedEvent therefore this method will be subscribed to that class
@Subscribe
fun `subscribed method`(event: MessageReceivedEvent) {
    // do something
    println(event.message)
}
...
// Register all the @Subscribe 'd methods inside of an instance
eventBus.register(this)

```
**Java**
```java
// Create eventbus
private EventBus eventBus = new EventBus(new LMFInvoker(), e -> {
    System.out.println("Error occurred in method: " + e.getMessage());
});

// Method you would like to subscribe to an event
// Param #1 is MessagedReceivedEvent therefore this method will be subscribed to that class
@Subscribe
public void subscribedMethod(MessageReceivedEvent event) {
    System.out.println(event.getMessage());
}
...
// Register all the @Subscribe 'd methods inside of an instance        
eventBus.register(this)
```
## Posting
**Kotlin**
```kotlin
// Post all methods subscribed to the event `MessageReceivedEvent`
eventBus.post(MessageReceivedEvent("Hello world"))
```
**Java**
```java 
// Post all methods subscribed to the event `MessageReceivedEvent`
eventBus.post(new MessageReceivedEvent("Hello world"));
```
## Unregistering
**Kotlin**
```kotlin
// Remove all @Subscribe 'd methods from an instance
eventBus.unregister(this)
```
**Java**
```java
// Remove all @Subscribe 'd methods from an instance
eventBus.unregister(this)
```

## Inheritance Support
When registering a class, `@Subscribe` methods from superclasses are automatically registered. All `@Subscribe` methods must be `final`.

**Kotlin**
```kotlin
open class ParentListener {
    @Subscribe
    final fun onEvent(event: MyEvent) {
        // Parent handles event
    }
}

class ChildListener : ParentListener() {
    @Subscribe
    final fun onOtherEvent(event: OtherEvent) {
        // Child handles different event
    }
}

// Registering ChildListener will register both onEvent and onOtherEvent
eventBus.register(ChildListener())
```

**Java**
```java
public class ParentListener {
    @Subscribe
    public final void onEvent(MyEvent event) {
        // Parent handles event
    }
}

public class ChildListener extends ParentListener {
    @Subscribe
    public final void onOtherEvent(OtherEvent event) {
        // Child handles different event
    }
}

// Registering ChildListener will register both onEvent and onOtherEvent
eventBus.register(new ChildListener());
```

