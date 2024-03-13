package io.tezrok.task

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory

/**
 * Add task service related classes to the project.
 */
internal class TaskServiceFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val moduleElem = context.getProject().modules.find { it.name == module.getName() } ?: error("Module not found")
        if (moduleElem.task?.enable != true) {
            return false
        }

        val javaRoot = module.source.main.java
        val applicationPackageRoot = javaRoot.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val taskDir = applicationPackageRoot.getOrAddJavaDirectory("task")
            context.writeTemplate(
                taskDir.getOrAddFile("TaskService.java"),
                "/templates/task/TaskService.java.vm"
            )
            context.writeTemplate(
                taskDir.getOrAddFile("TaskContext.java"),
                "/templates/task/TaskContext.java.vm"
            )
            context.writeTemplate(
                taskDir.getOrAddFile("TaskItem.java"),
                "/templates/task/TaskItem.java.vm"
            )
            context.writeTemplate(
                taskDir.getOrAddFile("TaskState.java"),
                "/templates/task/TaskState.java.vm"
            )
            context.writeTemplate(
                taskDir.getOrAddFile("TaskStatus.java"),
                "/templates/task/TaskStatus.java.vm"
            )
        } else {
            log.debug("Application package root is not set")
        }

        return true
    }

    private companion object {
        val log = LoggerFactory.getLogger(TaskServiceFeature::class.java)!!
    }
}
