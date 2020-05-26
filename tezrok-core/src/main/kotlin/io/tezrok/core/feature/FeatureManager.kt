package io.tezrok.core.feature

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.tezrok.api.Generator
import io.tezrok.core.factory.Factory
import java.lang.IllegalStateException

class FeatureManager(private val factory: Factory) {
    private val features = loadFeatures()

    fun features(): List<FeatureDef> = features

    fun getFeatureTree(featureName: String): FeatureTree {
        return getFeatureTree(featureName, true)!!
    }

    private fun getFeatureTree(featureName: String, mandatory: Boolean): FeatureTree? {
        val feature = features.firstOrNull { p -> p.name == featureName }

        if (feature != null) {
            return FeatureTree(feature.name,
                    feature.description,
                    mandatory = mandatory,
                    generator = factory.getGenerator(feature.generator),
                    dependsOn = (feature.dependsOn ?: emptyList())
                            .mapNotNull { getFeatureTree(it.name, it.mandatory) })
        }

        if (mandatory) {
            throw IllegalStateException("Mandatory feature '$featureName' not found")
        }

        return null
    }

    companion object {
        private val objectMapper = ObjectMapper().registerModule(KotlinModule())

        fun loadFeatures(): List<FeatureDef> {
            val json = javaClass.getResource("/features/features.json").readText()

            return objectMapper.readValue(json, FeaturesRoot::class.java).features
        }
    }

}

private data class FeaturesRoot(var features: List<FeatureDef>)

data class FeatureDef(val name: String,
                      val description: String,
                      val generator: String,
                      val dependsOn: List<FeatureRef>?)

data class FeatureRef(val name: String,
                      val mandatory: Boolean)

data class FeatureTree(val name: String,
                       val description: String,
                       val generator: Generator,
                       val mandatory: Boolean,
                       val dependsOn: List<FeatureTree>)
