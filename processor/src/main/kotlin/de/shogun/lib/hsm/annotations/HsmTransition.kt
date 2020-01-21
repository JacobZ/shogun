package de.shogun.lib.hsm.annotations

import de.shogun.lib.hsm.State
import de.shogun.lib.hsm.TransitionKind
import kotlin.reflect.KClass

annotation class HsmTransition(
        val eventName: String,
        val targetState: KClass<out State>
)