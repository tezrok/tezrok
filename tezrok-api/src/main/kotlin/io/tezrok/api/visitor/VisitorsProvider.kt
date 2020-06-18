package io.tezrok.api.visitor

import io.tezrok.api.service.Service
import io.tezrok.api.service.Visitor

/**
 * Providing multiple visitors by one object
 */
interface VisitorsProvider : Service {
    fun getVisitors(): List<Visitor>
}
