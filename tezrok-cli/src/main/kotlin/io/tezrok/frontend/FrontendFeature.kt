package io.tezrok.frontend

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.ProjectNode

internal class FrontendFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val projectElem = context.getProject()
        if (projectElem.frontend != true) {
            return false
        }

        val module = project.getSingleModule()
        val gitignoreFile = project.getFile(".gitignore")

        val moduelName = module.getName()
        if (gitignoreFile != null) {
            val text = gitignoreFile.asString()
            gitignoreFile.setString(
                "$text\n" + """
##############################
## Frontend
##############################
/$moduelName/src/main/resources/public/
/$moduelName/src/main/resources/static/assets/
/$moduelName/src/main/resources/static/themes/
/$moduelName/src/main/resources/static/test/
/$moduelName/src/main/resources/static/favicon.ico
/$moduelName/src/main/resources/templates/thymeleaf/index.html
"""
            )
        }

        // add frontend module to the beginning of the list
        val modulesRefNode = project.pom.getModulesRefNode()
        modulesRefNode.addModule(moduelName + "-frontend")
        val modules = modulesRefNode.getModules().toMutableList()
        modules.add(0, modules.last())
        modules.removeAt(modules.size - 1)
        modulesRefNode.setModules(modules)
        // change maven
        addMavenResourcePlugin(module)
        return true
    }

    private fun addMavenResourcePlugin(module: ModuleNode) {
        module.pom.addPluginDependency("org.apache.maven.plugins:maven-resources-plugin:3.3.1")
            .node
            .getOrAdd("executions")
            .add("execution")
            .add("id", "copy Vue.js frontend content").and()
            .add("phase", "generate-resources").and()
            .add("goals")
            .add("goal", "copy-resources").and().and()
            .add("configuration")
            .add("outputDirectory", "src/main/resources/public").and()
            .add("overwrite", "true").and()
            .add("resources")
            .add("resource")
            .add("directory", "\${project.parent.basedir}/${module.getName()}-frontend/target/dist").and()
            .add("includes")
            .add("include", "assets/").and()
            .add("include", "index.html").and()
            .add("include", "favicon.ico")
    }
}
