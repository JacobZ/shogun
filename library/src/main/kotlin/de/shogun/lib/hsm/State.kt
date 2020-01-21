package de.shogun.lib.hsm

abstract class State {

    // TODO private would be better (still writable via reflection?)
    val handlers = mutableMapOf<String, MutableSet<StateHandler>>()

    var owner: StateMachine? = null

    abstract fun onEnter(event: Event)

    abstract fun onExit(event: Event)

    internal open fun enter(previous: State?, next: State, event: Event) {
        onEnter(event)
    }

    internal open fun exit(previous: State, next: State?, event: Event) {
        onExit(event)
    }

    fun addHandler(eventName: String,
                   targetState: State,
                   transitionKind: TransitionKind,
                   action: ((event: Event) -> Unit)? = null,
                   guard: ((event: Event) -> Boolean)? = null) {
        addHandler(eventName, StateHandler(targetState, transitionKind, action, guard))
    }

    fun addHandler(eventName: String, handler: StateHandler) {
        handlers.putIfAbsent(eventName, mutableSetOf())
        handlers[eventName]!!.add(handler)
    }

    internal open fun handleWithOverride(event: Event): Boolean {
        findHandler(event)?.let {
            owner!!.executeHandler(it, event)
            return true
        }
        return false
    }

    private fun findHandler(event: Event): StateHandler? = handlers[event.eventName]?.find {
        it.evaluate(event)
    }

    internal open fun getDescendantStates(): Set<State> = setOf()

    internal open fun addParent(parent: StateMachine) = Unit

    internal open fun getAllActiveStates(): Collection<State> = setOf()

    override fun equals(other: Any?): Boolean {
        other ?: return false
        return other::class == this::class
    }
}