package io.tezrok.core.service

import io.tezrok.api.visitor.EachClassVisitor
import io.tezrok.api.visitor.EntityClassVisitor
import io.tezrok.api.visitor.MainAppVisitor
import io.tezrok.api.visitor.MavenVisitor

internal class MavenVisitorsProvider(val visitors: Set<MavenVisitor>)

internal class MainAppVisitorsProvider(val visitors: Set<MainAppVisitor>)

internal class EachClassVisitorsProvider(val visitors: Set<EachClassVisitor>)

internal class EntityClassVisitorsProvider (val visitors: Set<EntityClassVisitor>)
