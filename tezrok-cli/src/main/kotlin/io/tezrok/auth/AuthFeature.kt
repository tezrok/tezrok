package io.tezrok.auth

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

internal class AuthFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val apiDir = applicationPackageRoot.getOrAddJavaDirectory("dto").getOrAddJavaDirectory("api")
            context.addFile(apiDir, "/templates/auth/LoginApiResult.java.vm")
            context.addFile(apiDir, "/templates/auth/PasswordRecoverDto.java.vm")
            context.addFile(apiDir, "/templates/auth/PasswordRecoverFinishDto.java.vm")
            context.addFile(apiDir, "/templates/auth/UserLoginDto.java.vm")
            context.addFile(apiDir, "/templates/auth/UserRegisterDto.java.vm")

            val serviceDir = applicationPackageRoot.getOrAddJavaDirectory("service")
            context.addFile(serviceDir, "/templates/auth/AuthService.java.vm")
            context.addFile(serviceDir, "/templates/auth/CurrentAuthService.java.vm")

            val webDir = applicationPackageRoot.getOrAddJavaDirectory("web")
            context.addFile(webDir, "/templates/auth/AuthController.java.vm")

            val restDir = webDir.getOrAddJavaDirectory("rest")
            context.addFile(restDir, "/templates/auth/AuthApiController.java.vm")
        }

        return true
    }
}