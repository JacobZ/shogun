package de.shogun.lib.hsm.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class HsmGuard(val eventName: String)