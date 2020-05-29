package io.tezrok.api

import io.tezrok.api.service.Service

/**
 * Generator generates several files
 *
 * @author Ruslan Absalyamov
 * @since 1.0
 */
interface Generator : Service {
    fun execute(context: ExecuteContext)
}
