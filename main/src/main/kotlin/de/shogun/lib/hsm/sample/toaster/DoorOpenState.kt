package de.shogun.lib.hsm.sample.toaster

import de.shogun.lib.hsm.Event
import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.annotations.HsmGuard
import de.shogun.lib.hsm.annotations.HsmState
import de.shogun.lib.hsm.annotations.HsmTransition
import de.shogun.lib.hsm.annotations.HsmTransitionExternal

@HsmState("ToasterStateMachine", true)
@HsmTransitionExternal([HsmTransition("DOOR_CLOSE", HeatingState::class)])
class DoorOpenState : State() {

    companion object {
        const val TAG = "DoorOpenState"
    }

    var isKittenInOven = false

    override fun onEnter(event: Event) {
        println("$TAG: onEnter event: ${event.eventName}")
    }

    override fun onExit(event: Event) {
        println("$TAG: onExit event: ${event.eventName}")
    }

    @HsmGuard("DOOR_CLOSE")
    fun onDoorCloseGuard(@Suppress("UNUSED_PARAMETER") event: Event): Boolean {
        println("$TAG: onDoorCloseGuard event: ${event.eventName}")
        return !isKittenInOven
    }

}