package com.tezrok.api.service

/**
 * Basic interface for all services
 */
interface TezrokService {
    companion object {
        /**
         * Empty service implementation to be used as default (not null)
         */
        @JvmField
        val Empty: TezrokService = object : TezrokService {}
    }
}

internal fun TezrokService.isEmpty(): Boolean = this == TezrokService.Empty
