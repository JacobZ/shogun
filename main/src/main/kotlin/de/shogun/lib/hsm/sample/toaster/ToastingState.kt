package de.shogun.lib.hsm.sample.toaster

import de.shogun.lib.hsm.Event
import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.annotations.HsmState

@HsmState("ToasterStateMachine")
class ToastingState : State() {

    companion object {
        const val TAG = "ToastingState"
    }

    override fun onEnter(event: Event) {
        println("$TAG: onEnter event: ${event.eventName}")
    }

    override fun onExit(event: Event) {
        println("$TAG: onExit event: ${event.eventName}")
    }
}




