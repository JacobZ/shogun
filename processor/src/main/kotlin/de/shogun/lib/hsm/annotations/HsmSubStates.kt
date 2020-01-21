package de.shogun.lib.hsm.annotations

import de.shogun.lib.hsm.State
import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class HsmSubStates(vararg val subStates: KClass<out State>)