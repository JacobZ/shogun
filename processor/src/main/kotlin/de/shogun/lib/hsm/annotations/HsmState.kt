package de.shogun.lib.hsm.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class HsmState(
        val stateMachineName: String,
        val isInitialState: Boolean = false
)