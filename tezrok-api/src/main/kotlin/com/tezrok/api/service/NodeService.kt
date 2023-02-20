package com.tezrok.api.service

import com.tezrok.api.node.Nodeable
import com.tezrok.api.type.TypeFolder

/**
 * Service which can be attached to the node
 *
 * @see TypeFolder
 */
interface NodeService : TezrokService, Nodeable {}
