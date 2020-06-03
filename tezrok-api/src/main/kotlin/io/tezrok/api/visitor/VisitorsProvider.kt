package io.tezrok.api.visitor

import io.tezrok.api.service.Service

/**
 * Providing multiple visitors by one object
 */
interface VisitorsProvider : Service {
    fun getVisitors(): List<Service>
}
