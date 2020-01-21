package de.shogun.lib.hsm.sample

import de.shogun.lib.hsm.Event
import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.annotations.*

@HsmState(stateMachineName = "Sample")
@HsmTransitionLocal([HsmTransition("sampleTransition", SampleState::class)])
class SampleState : State() {


    companion object {
        const val TAG = "SampleState"
    }

    override fun onEnter(event: Event) {
        println("$TAG: onEnter event: ${event.eventName}")
    }

    override fun onExit(event: Event) {
        println("$TAG: onExit event: ${event.eventName}")
    }

    @HsmAction("sampleTransition")
    fun onSampleTransition(@Suppress("UNUSED_PARAMETER") event: Event) {
        println("$TAG: onSampleTransitionAction event: ${event.eventName}")
    }

    @HsmGuard("sampleTransition")
    fun guardSampleTransition(@Suppress("UNUSED_PARAMETER") event: Event): Boolean {
        println("$TAG: guardSampleTransition event: ${event.eventName}")
        return true
    }

}