package io.tezrok.api

enum class ProcessModelPhase {
    /**
     * First phase of the model processing.
     */
    PreProcess,
    /**
     * Phase of the model processing after normalized entities (synthetic fields added).
     */
    Process,

    /**
     * Last phase of the model processing.
     */
    PostProcess
}
