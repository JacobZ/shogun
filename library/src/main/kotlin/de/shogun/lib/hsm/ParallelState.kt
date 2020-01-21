package de.shogun.lib.hsm

abstract class ParallelState(private vararg val stateMachines: StateMachine) : State() {

    override fun enter(previous: State?, next: State, event: Event) {
        super.enter(previous, next, event)
        stateMachines.forEach {
            it.enterState(previous, next, event)
        }
    }

    override fun exit(previous: State, next: State?, event: Event) {
        super.exit(previous, next, event)
        stateMachines.forEach {
            it.teardown(event.payload)
        }
    }

    override fun handleWithOverride(event: Event): Boolean {
        var isConsumed = false
        stateMachines.forEach {
            if (it.handleWithOverride(event)) {
                isConsumed = true
            }
        }
        if (!isConsumed) {
            return super.handleWithOverride(event)
        }
        return true
    }

    override fun addParent(parent: StateMachine) {
        stateMachines.forEach {
            it.addParent(parent)
        }
    }

    override fun getDescendantStates(): Set<State> = stateMachines.map {
        it.descendantStates
    }.reduce { sum, element ->
        sum.toMutableSet().addAll(element)
        sum.toSet()
    }

    override fun getAllActiveStates(): Set<State> = stateMachines.map {
        it.getAllActiveStates()
    }.reduce { sum, element ->
        sum.toMutableSet().addAll(element)
        sum.toSet()
    }
}