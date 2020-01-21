package de.shogun.lib.hsm.sample.toaster

import de.shogun.lib.hsm.Event
import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.SubState
import de.shogun.lib.hsm.annotations.*

@HsmState("ToasterStateMachine")
@HsmSubStates(ToastingState::class, BakingState::class)
@HsmTransitionExternal([HsmTransition("DOOR_OPEN", DoorOpenState::class)])
@HsmTransitionLocal([
    HsmTransition("DO_TOASTING", ToastingState::class),
    HsmTransition("DO_BAKING", BakingState::class)
])
class HeatingState(vararg states: State) : SubState(*states) {

    companion object {
        const val TAG = "HeatingState"
    }

    override fun onEnter(event: Event) {
        println("$TAG: onEnter event: ${event.eventName}")
    }

    override fun onExit(event: Event) {
        println("$TAG: onExit event: ${event.eventName}")
    }

    @Suppress("unused")
    @HsmAction("DO_TOASTING")
    fun onDoToasting(@Suppress("UNUSED_PARAMETER") event: Event) {
        println("$TAG: onDoToasting event: ${event.eventName}")
        println("toasty toasty yummy yum")
    }
}