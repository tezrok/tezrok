package io.tezrok.api.sql

import io.tezrok.api.schema.Schema
import io.tezrok.api.TezrokGenerator
import io.tezrok.api.model.SqlScript

interface SqlGenerator : TezrokGenerator<Schema, SqlScript>
