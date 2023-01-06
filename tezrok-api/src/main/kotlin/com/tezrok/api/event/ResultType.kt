package com.tezrok.api.event

import com.tezrok.api.feature.Feature

/**
 * Handler result type of [NodeEvent]
 *
 * See [Feature.onNodeEvent] for more information
 */
enum class ResultType {
    /**
     * Continue to handle the event with other [Feature]s
     */
    CONTINUE,

    /**
     * Stop handling the event for other [Feature]s
     */
    STOP,

    /**
     * Stop handling the event and cancel action at all
     *
     * Used mostly for PreXXX events
     */
    CANCEL
}
