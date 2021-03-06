package de.shogun.lib.hsm.sample.toaster

import de.shogun.lib.hsm.Event
import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.annotations.HsmState

/*
 * Copyright 2020 by AVM GmbH, All Rights Reserved
 * Contact: <info@avm.de>
 */
@HsmState("ToasterStateMachine")
class IdleState : State() {

    companion object {
        const val TAG = "IdleState"
    }

    override fun onEnter(event: Event) {
        println("$TAG: onEnter event: ${event.eventName}")
    }

    override fun onExit(event: Event) {
        println("$TAG: onExit event: ${event.eventName}")
    }
}