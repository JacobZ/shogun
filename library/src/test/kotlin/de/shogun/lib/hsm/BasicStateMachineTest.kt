package de.shogun.lib.hsm

import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.fail
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import kotlin.math.exp


/*
 * Copyright 2020 by AVM GmbH, All Rights Reserved
 * Contact: <info@avm.de>
 */

class TestStateMachine(initialState: State, vararg states: State) : StateMachine(initialState, *states) {
    fun getCurrentStateForTesting(): State? = currentState
}

open class TestState(private val enter: (() -> Unit)? = null, private val exit: (() -> Unit)? = null) : State() {
    override fun onEnter(event: Event) {
        enter?.invoke()
    }

    override fun onExit(event: Event) {
        exit?.invoke()
    }
}

open class TestSubState(private val enter: (() -> Unit)?, private val exit: (() -> Unit)?, vararg states: State) : SubState(*states) {
    override fun onEnter(event: Event) {
        enter?.invoke()
    }

    override fun onExit(event: Event) {
        exit?.invoke()
    }
}

open class TestParallelState(private val enter: (() -> Unit)?, private val exit: (() -> Unit)?, stateMachine: StateMachine) : ParallelState(stateMachine) {
    override fun onEnter(event: Event) {
        enter?.invoke()
    }

    override fun onExit(event: Event) {
        exit?.invoke()
    }
}

class StateA(enter: (() -> Unit)? = null, exit: (() -> Unit)? = null) : TestState(enter, exit)

class StateB(enter: (() -> Unit)? = null, exit: (() -> Unit)? = null) : TestState(enter, exit)



class BasicStateMachineTest {

    @Test
    fun initialStateIsEntered() {
        //given:
        val enter = mockk<() -> Unit>(relaxed = true)
        val exit = mockk<() -> Unit>(relaxed = true)
        val state = TestState(enter, exit)

        val sm = StateMachine(state)

        //when:
        sm.initialize()

        //then:
        verify {
            enter()
        }
    }

    @Test
    fun currentStateIsExited() {
        //given:
        val enter = mockk<() -> Unit>(relaxed = true)
        val exit = mockk<() -> Unit>(relaxed = true)
        val state = TestState(enter, exit)

        val sm = StateMachine(state)
        sm.initialize()

        //when:
        sm.teardown()

        //then:
        verify {
            enter()
            exit()
        }
    }

    @Test
    fun eventsWithoutPayloadCauseStateTransition() {
        //given:
        val enterOn = mockk<() -> Unit>(relaxed = true)
        val exitOn = mockk<() -> Unit>(relaxed = true)
        val enterOff = mockk<() -> Unit>(relaxed = true)
        val exitOff = mockk<() -> Unit>(relaxed = true)

        val stateOn = StateA(enterOn, exitOn)
        val stateOff = StateB(enterOff, exitOff)

        stateOn.addHandler("toggle", stateOff, TransitionKind.External)

        val sm = StateMachine(stateOn, stateOff)
        sm.initialize()

        //when:
        sm.handleEvent("toggle")

        //then:
        verify {
            enterOn()
            exitOn()
            enterOff()
        }
    }

    // TODO improve check for invalid transitions in StateMachine and throw propper exception
    @Test(expected = KotlinNullPointerException::class)
    fun impossibleTransitionTest() {
        // given:
        val dummy = mockk<() -> Unit>(relaxed = true)
        val stateOn = StateA(dummy, dummy)
        val stateOff = StateB(dummy, dummy)

        stateOn.addHandler("toggle", stateOff, TransitionKind.External)


        val sm = StateMachine(stateOn)
        sm.initialize()

        // when:
        sm.handleEvent("toggle")
        Assert.fail("expected NullpointerException but nothing happnend")
    }

    @Test
    fun eventsWithPayloadCauseStateTransition() {
        //given:
        val enterOn = mockk<() -> Unit>(relaxed = true)
        val exitOn = mockk<() -> Unit>(relaxed = true)
        val enterOff = mockk<() -> Unit>(relaxed = true)
        val exitOff = mockk<() -> Unit>(relaxed = true)

        val stateOn = StateA(enterOn, exitOn)
        val stateOff = StateB(enterOff, exitOff)

        stateOn.addHandler("toggle", stateOff, TransitionKind.External)


        val sm = StateMachine(stateOn, stateOff)
        sm.initialize()

        //when:
        sm.handleEvent("toggle", mutableMapOf())

        //then:
        verify {
            enterOn()
            exitOn()
            enterOff()
        }
    }

    @Test
    fun actionsAreCalledBetweenExitAndEnter() {
        class StateAA(enter: () -> Unit, exit: () -> Unit) : TestState(enter, exit)
        class StateBB(enter: () -> Unit, exit: () -> Unit) : TestState(enter, exit)
        class StateA(enter: () -> Unit, exit: () -> Unit, vararg states: State) : TestSubState(enter, exit, *states)
        class StateB(enter: () -> Unit, exit: () -> Unit, vararg states: State) : TestSubState(enter, exit, *states)

        val enterA = mockk<() -> Unit>(relaxed = true)
        val exitA = mockk<() -> Unit>(relaxed = true)
        val enterB = mockk<() -> Unit>(relaxed = true)
        val exitB = mockk<() -> Unit>(relaxed = true)
        val enterAA = mockk<() -> Unit>(relaxed = true)
        val exitAA = mockk<() -> Unit>(relaxed = true)
        val enterBB = mockk<() -> Unit>(relaxed = true)
        val exitBB = mockk<() -> Unit>(relaxed = true)

        val stateAA = StateAA(enterAA, exitAA)
        val stateBB = StateBB(enterBB, exitBB)
        val stateA = StateA(enterA, exitA, stateAA)
        val stateB = StateB(enterB, exitB, stateBB)

        stateAA.addHandler("T", stateBB, TransitionKind.External)

        val sm = TestStateMachine(stateA, stateB)
        sm.initialize()

        sm.handleEvent("T")

        verify {
            enterA()
            enterAA()
            exitAA()
            exitA()
            enterB()
            enterBB()
        }
    }

    @Test
    fun actionsAreCalledOnTransitionsWithPayload() {
        //given:
        val transition = mockk<() -> Unit>(relaxed = true)

        val stateOn = StateA()
        val stateOff = StateB()

        var didActionRun = false
        val action = { event: Event ->
            assertThat(event.payload["foo"], Matchers.`is`<Any>("bar"))
            didActionRun = true
        }

        stateOn.addHandler("toggle", stateOff, TransitionKind.External, action)

        val sm = StateMachine(stateOn, stateOff)
        sm.initialize()


        val payload = mutableMapOf("foo" to "bar")
        val event = Event("toggle", payload)

        //when:
        sm.handleEvent(event)

        //then:
        assert(didActionRun)
    }

    @Test
    fun actionsAreCalledAlwaysWithValidPayload() {
        //given:
        val transition = mockk<() -> Unit>(relaxed = true)

        val stateOn = StateA()
        val stateOff = StateB()

        var didActionRun = false
        val action = { event: Event ->
            didActionRun = true
        }

        stateOn.addHandler("toggle", stateOff, TransitionKind.External, action)

        val sm = StateMachine(stateOn, stateOff)
        sm.initialize()

        val event = Event("toggle")

        //when:
        sm.handleEvent(event)

        //then:
        assert(didActionRun)
    }

    @Test
    fun actionsCanBeInternal() {
        //given:
        val transition = mockk<() -> Unit>(relaxed = true)

        val exit = mockk<() -> Unit>(relaxed = true)
        val stateOn = StateA(exit = exit)
        val stateOff = StateB()

        var didActionRun = false
        val action = { event: Event ->
            didActionRun = true
        }

        stateOn.addHandler("toggle", stateOn, TransitionKind.Internal, action)

        val sm = StateMachine(stateOn, stateOff)
        sm.initialize()

        val event = Event("toggle")

        //when:
        sm.handleEvent(event)

        //then:
        assert(didActionRun)
        verify {
            exit() wasNot Called
        }
    }

    @Test(expected = IllegalStateException::class)
    fun noMatchingStateAvailable() {
        //given:
        val stateOn = StateA()
        val stateOff = StateB()

        stateOn.addHandler("toggle", stateOff, TransitionKind.External)

        val sm = StateMachine(stateOn)
        sm.initialize()

        //when:
        sm.handleEvent("toggle")

        //then:
        fail("expected IllegalStateException since target State was not part of StateMachine")
    }

    @Test
    fun emittedEventTestHandledFromTopStateMachine() {
        //given:
        class EmittingState : TestState() {
            override fun onEnter(event: Event) {
                super.onEnter(event)
                this.owner?.handleEvent("toggle")
            }
        }

        val stateOn = EmittingState()
        val stateOff = StateB()

        stateOn.addHandler("toggle", stateOff, TransitionKind.External)

        val sm = StateMachine(stateOn, stateOff)

        //when:
        sm.initialize()

        //then:
        assertThat(sm.getAllActiveStates().contains(stateOff), Matchers.equalTo(true))
    }

    @Test
    fun getAllActiveStates() {
        //given:

        val stateB = TestState()
        val subA = TestSubState(null, null, stateB)


        val sm = StateMachine(subA)

        //when:
        sm.initialize()

        //then:
        assertThat(sm.getAllActiveStates(), Matchers.hasItems(subA, stateB))
    }
}