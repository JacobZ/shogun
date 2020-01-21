package de.shogun.lib.hsm

import java.util.concurrent.ConcurrentLinkedQueue

open class StateMachine(
        private var initialState: State,
        vararg states: State
) {

    private val states: MutableSet<State> = mutableSetOf(initialState).apply { addAll(states) }
    private val path = mutableListOf<StateMachine>()
    protected var currentState: State? = initialState
    internal lateinit var container: State
    internal var descendantStates: Set<State>
    private var eventQueueInProgress = false
    private val eventQueue = ConcurrentLinkedQueue<Event>()

    init {
        val descendantStates = mutableListOf<State>()
        this.states.add(initialState)
        this.states.forEach {
            it.owner = this
            descendantStates.addAll(it.getDescendantStates())
        }
        this.descendantStates = descendantStates.toSet()
        generatePath()
    }

    fun foo() {
        val descendantStates = mutableListOf<State>()
        if (!states.contains(initialState)) this.states.add(initialState)
        this.states.forEach {
            it.owner = this
            descendantStates.addAll(it.getDescendantStates())
        }
        this.descendantStates = descendantStates.toSet()
        generatePath()
    }

    private fun generatePath() {
        path.add(0, this)
        states.forEach {
            it.addParent(this)
        }
    }

    fun initialize(payload: MutableMap<String, Any> = mutableMapOf()) {
        val event = Event("Hsm-initial-event", payload)
        eventQueueInProgress = true
        enterState(null, initialState, event)
        eventQueueInProgress = false
        processEventQueue()
    }

    fun teardown(payload: MutableMap<String, out Any> = mutableMapOf()) {
        exitState(currentState!!, null, Event("Hsm-teardown-event", payload))
        currentState = null
    }

    fun handleEvent(eventName: String, payload: MutableMap<String, Any> = mutableMapOf()) {
        handleEvent(Event(eventName, payload))
    }

    fun handleEvent(event: Event) {
        eventQueue.add(event)
        processEventQueue()
    }

    private fun processEventQueue() {
        if (eventQueueInProgress) return
        eventQueueInProgress = true
        while (eventQueue.peek() != null) {
            val event = eventQueue.poll()
            if (currentState?.handleWithOverride(event) == false) {
                // TODO log that nobody handled the event
            }
        }
        eventQueueInProgress = false
    }

    fun handleWithOverride(event: Event): Boolean {
        return currentState?.handleWithOverride(event) ?: false
    }

    private fun switchState(
            previousState: State,
            nextState: State,
            action: ((event: Event) -> Unit)?,
            event: Event
    ) {
        exitState(previousState, nextState, event)
        executeAction(action, event)
        enterState(previousState, nextState, event)
    }

    internal fun enterState(previousState: State?, targetState: State, event: Event) {
        val targetLevel = targetState.owner!!.path.size
        val localLevel = path.size
        val nextState: State = when {
            targetLevel < localLevel -> initialState
            targetLevel == localLevel -> targetState
            // targetLevel > localLevel
            else -> findNextStateOnPathTo(targetState)
        }

        currentState = if (states.contains(nextState)) {
            nextState
        } else {
            initialState
        }
        currentState?.enter(previousState, nextState, event) ?: throw RuntimeException("CurrentState can't be null here. Yet you see this exception :(")
    }

    internal fun exitState(previous: State, next: State?, event: Event) {
        currentState?.exit(previous, next, event) ?: throw IllegalStateException("current state is null")
    }

    private fun findNextStateOnPathTo(targetState: State): State = findNextStateMachineOnPathTo(targetState).container

    internal fun executeHandler(handler: StateHandler, event: Event) {
        when (handler.transitionKind) {
            TransitionKind.External -> doExternalTransition(currentState!!, handler.targetState, handler.action, event)
            TransitionKind.Local -> doLocalTransition(currentState!!, handler.targetState, handler.action, event)
            TransitionKind.Internal -> executeAction(handler.action, event)
        }
    }

    private fun executeAction(action: ((event: Event) -> Unit)?, event: Event) {
        action?.invoke(event)
    }

    private fun doExternalTransition(
            previousState: State,
            targetState: State,
            action: ((event: Event) -> Unit)?,
            event: Event) {

        val lowestCommonAncestor: StateMachine = findLowestCommonAncestor(targetState)
        lowestCommonAncestor.switchState(previousState, targetState, action, event)
    }

    private fun doLocalTransition(
            previousState: State,
            targetState: State,
            action: ((event: Event) -> Unit)?,
            event: Event
    ): Unit = when {

        previousState.getDescendantStates().contains(targetState) -> {
            val stateMachine = findNextStateMachineOnPathTo(targetState)
            stateMachine.switchState(previousState, targetState, action, event)
        }
        targetState.getDescendantStates().contains(previousState) -> {
            val targetLevel = targetState.owner!!.path.size
            val stateMachine = path[targetLevel]
            stateMachine.switchState(previousState, targetState, action, event)
        }
        previousState == targetState -> {
            // TODO clarify desired behavior for local transition on self currently behaves like an internal transition
            executeAction(action, event)
        }
        else -> doExternalTransition(previousState, targetState, action, event)
    }

    private fun findLowestCommonAncestor(targetState: State): StateMachine {
        checkNotNull(targetState.owner) { "$targetState is not contained in state machine model." }
        val targetPath = targetState.owner!!.path

        (1..targetPath.size).forEach { index ->
            try {
                val targetAncestor = targetPath[index]
                val localAncestor = path[index]
                if (targetAncestor != localAncestor) {
                    return path[index - 1]
                }
            } catch (e: IndexOutOfBoundsException) {
                return path[index - 1]
            }
        }
        return this
    }

    private fun findNextStateMachineOnPathTo(targetState: State): StateMachine {
        val localLevel = path.size
        val targetOwner = targetState.owner!!
        return targetOwner.path[localLevel]
    }

    internal fun addParent(parent: StateMachine) {
        path.add(0, parent)
        states.forEach {
            it.addParent(parent)
        }
    }

    fun getAllActiveStates(): Set<State> {
        currentState ?: return emptySet()
        val activeStates: MutableSet<State> = mutableSetOf(currentState!!)
        activeStates.addAll(currentState!!.getAllActiveStates())
        return activeStates.toSet()
    }
}