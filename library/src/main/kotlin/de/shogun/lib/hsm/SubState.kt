package de.shogun.lib.hsm

abstract class SubState(vararg states: State) : State() {
    private val subStateMachine: StateMachine

    init {
        val stateList = states.toMutableList()
        val initialState = stateList.removeAt(0)
        subStateMachine = StateMachine(initialState, *stateList.toTypedArray())
        subStateMachine.container = this
    }

    override fun enter(previous: State?, next: State, event: Event) {
        super.enter(previous, next, event)
        subStateMachine.enterState(previous, next, event)
    }

    override fun exit(previous: State, next: State?, event: Event) {
        subStateMachine.teardown(event.payload)
        super.exit(previous, next, event)
    }

    override fun handleWithOverride(event: Event): Boolean = when {
        subStateMachine.handleWithOverride(event) -> true
        else -> super.handleWithOverride(event)
    }

    override fun addParent(parent: StateMachine) {
        subStateMachine.addParent(parent)
    }

    fun foo() = subStateMachine.foo()

    override fun getDescendantStates(): Set<State> = subStateMachine.descendantStates

    override fun getAllActiveStates(): Collection<State> = subStateMachine.getAllActiveStates()
}