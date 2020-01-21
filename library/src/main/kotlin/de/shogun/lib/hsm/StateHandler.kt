package de.shogun.lib.hsm

data class StateHandler(val targetState: State,
                        val transitionKind: TransitionKind,
                        val action: ((event: Event) -> Unit)? = null,
                        val guard: ((event: Event) -> Boolean)? = null) {

    fun evaluate(event: Event): Boolean {
        val isTransactionAllowed = guard?.invoke(event)
        return isTransactionAllowed ?: true
    }
}