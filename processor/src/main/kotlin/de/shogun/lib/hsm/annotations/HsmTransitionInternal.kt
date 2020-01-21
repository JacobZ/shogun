package de.shogun.lib.hsm.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class HsmTransitionInternal(
        val transitions: Array<HsmTransition>
)