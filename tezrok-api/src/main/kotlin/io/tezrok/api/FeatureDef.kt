package io.tezrok.api

data class FeatureDef(val name: String,
                      val description: String,
                      val provider: String,
                      val dependsOn: List<FeatureDef>)

data class FeatureRef(val name: String,
                      val mandatory: Boolean)
