package de.shogun.lib.hsm.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class HsmTransitionLocal(
        val transitions: Array<HsmTransition>
)