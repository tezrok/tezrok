package com.tezrok.api.event

/**
 * Handler result type of [NodeEvent]
 */
data class EventResult(val type: ResultType, val message: String? = null)
