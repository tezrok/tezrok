package io.tezrok.core.service

import io.tezrok.api.visitor.MainAppVisitor
import io.tezrok.api.visitor.MavenVisitor

class MavenVisitorsProvider(val visitors: List<MavenVisitor>)

class MainAppVisitorsProvider(val visitors: List<MainAppVisitor>)
