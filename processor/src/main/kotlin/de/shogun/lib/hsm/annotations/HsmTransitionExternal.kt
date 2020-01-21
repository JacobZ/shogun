package de.shogun.lib.hsm.annotations


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class HsmTransitionExternal(
        val transitions: Array<HsmTransition>
)