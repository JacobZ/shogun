package de.shogun.lib.hsm.sample

import de.shogun.lib.hsm.Event
import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.annotations.HsmState
import de.shogun.lib.hsm.annotations.HsmTransition
import de.shogun.lib.hsm.annotations.HsmTransitionExternal

@HsmState("Sample")
@HsmTransitionExternal([
    HsmTransition("externalEvent", FooState::class)
])
class BarState : State() {

    companion object {
        const val TAG = "BarState"
    }

    override fun onEnter(event: Event) {
        println("$TAG: onEnter event: ${event.eventName}")
    }

    override fun onExit(event: Event) {
        println("$TAG: onExit event: ${event.eventName}")
    }
}