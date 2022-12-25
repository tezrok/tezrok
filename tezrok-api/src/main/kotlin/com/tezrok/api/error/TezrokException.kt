package com.tezrok.api.error

/**
 * Base exception for all Tezrok exceptions
 */
open class TezrokException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) :
            super(message, cause, enableSuppression, writableStackTrace)
}

/**
 * Exception occurs when node with given name already exists
 */
class NodeAlreadyExistsException(val name: String, val path: String) :
    TezrokException("Node with such name already exists: $name")
