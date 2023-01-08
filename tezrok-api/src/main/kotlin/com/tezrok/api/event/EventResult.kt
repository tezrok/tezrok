package com.tezrok.api.event

/**
 * Handler result type of [NodeEvent]
 */
data class EventResult(val type: ResultType, val message: String? = null) {
    companion object {
        @JvmField
        val Continue = of(ResultType.CONTINUE)

        @JvmStatic
        fun stop(message: String? = null) = of(ResultType.STOP, message)

        @JvmStatic
        fun cancel(message: String? = null) = of(ResultType.CANCEL, message)

        @JvmStatic
        fun of(type: ResultType, message: String? = null): EventResult = EventResult(type, message)
    }
}
