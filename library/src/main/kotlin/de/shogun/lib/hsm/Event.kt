package de.shogun.lib.hsm

data class Event(val eventName: String, val payload: MutableMap<String, out Any> = mutableMapOf())