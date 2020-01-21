package de.shogun.lib.hsm.sample

import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.StateMachine
import de.shogun.lib.hsm.TransitionKind
import de.shogun.lib.hsm.sample.toaster.*

fun main(args: Array<String>) {
    val toasterStateMachine = ToasterStateMachineHsmFactory.stateMachine
    toasterStateMachine.initialize()
    toasterStateMachine.handleEvent("DOOR_CLOSE")
    toasterStateMachine.handleEvent("DO_TOASTING")
}

fun test() {
    val stateMachine = SampleHsmFactory.stateMachine
    stateMachine.initialize()
    println("send externalEvent2")
    stateMachine.handleEvent("externalEvent2")
    println("send externalEvent")
    stateMachine.handleEvent("externalEvent")
    println("send sampleTransition")
    stateMachine.handleEvent("sampleTransition") // should not switch to SampleState
    println("send bla")
    stateMachine.handleEvent("bla")
    println("send sampleTransition")
    stateMachine.handleEvent("sampleTransition")
}

object Toaster {

    fun start() {
        val states = mutableSetOf(DoorOpenState(), BakingState(), ToastingState(), IdleState())
        states.add(HeatingState(states.findState("IdleState"), states.findState("BakingState"), states.findState("ToastingState")))

        states.findState("DoorOpenState").apply {
            this as DoorOpenState
            addHandler(
                    "DOOR_CLOSE",
                    states.findState("HeatingState"),
                    TransitionKind.External,
                    guard = ::onDoorCloseGuard
            )
        }
        states.findState("HeatingState").addHandler("DOOR_OPEN", states.findState("DoorOpenState"), TransitionKind.External)
        states.findState("HeatingState").apply {
            this as HeatingState
            addHandler(
                    "DO_TOASTING",
                    states.findState("ToastingState"),
                    TransitionKind.Local,
                    ::onDoToasting
            )
        }
        states.findState("HeatingState").addHandler("DO_BAKING", states.findState("BakingState"), TransitionKind.Local)

        val stateMachine = StateMachine(states.first(), *states.toTypedArray())
        stateMachine.initialize()
        stateMachine.handleEvent("DOOR_CLOSE")
        stateMachine.handleEvent("DO_BAKING")
        stateMachine.handleEvent("DOOR_OPEN")
        stateMachine.handleEvent("DOOR_CLOSE")
        stateMachine.handleEvent("DO_TOASTING")
        stateMachine.handleEvent("DOOR_OPEN")
    }


    private fun Set<State>.findState(targetState: String): State = find { state ->
        state::class.java.simpleName == targetState
    }!!
}


object Foo {

    fun bar() {
        val states: Set<State> = mutableSetOf(FooState(), BarState(), SampleState())

        states.findState("FooState").addHandler("externalEvent", states.findState("BarState"), TransitionKind.External)
        states.findState("BarState").addHandler("externalEvent2", states.findState("FooState"), TransitionKind.External)
        states.findState("BarState").addHandler("bla", states.findState("SampleState"), TransitionKind.External)
        states.findState("SampleState").apply {
            this as SampleState
            addHandler(
                    "sampleTransition",
                    states.findState("SampleState"),
                    TransitionKind.External,
                    ::onSampleTransition,// TODO action does not get fired
                    ::guardSampleTransition
            )
        }

        val stateMachine = StateMachine(states.first(), *states.toTypedArray())
        stateMachine.initialize()
        stateMachine.handleEvent("externalEvent2")
        stateMachine.handleEvent("externalEvent")
        stateMachine.handleEvent("sampleTransition") // should not switch to SampleState
        stateMachine.handleEvent("bla")
        stateMachine.handleEvent("sampleTransition")
    }


    private fun Set<State>.findState(targetState: String): State = find { state ->
        state::class.java.simpleName == targetState
    }!!
}