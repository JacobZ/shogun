package de.shogun.lib.hsm.sample

import de.shogun.lib.hsm.Event
import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.annotations.HsmState
import de.shogun.lib.hsm.annotations.HsmTransition
import de.shogun.lib.hsm.annotations.HsmTransitionExternal
import de.shogun.lib.hsm.annotations.HsmTransitionInternal
import org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS

@HsmState("Sample", true)
@HsmTransitionExternal([
    HsmTransition("externalEvent2", BarState::class),
    HsmTransition("bla", SampleState::class)
])
class FooState : State() {

    companion object {
        const val TAG = "FooState"
    }

    override fun onEnter(event: Event) {
        println("$TAG: onEnter event: ${event.eventName}")
    }

    override fun onExit(event: Event) {
        println("$TAG: onExit event: ${event.eventName}")
    }
}