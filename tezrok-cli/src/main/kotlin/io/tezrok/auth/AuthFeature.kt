package io.tezrok.auth

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.addNewSettings

internal class AuthFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val moduleName = module.getName()
        val moduleElem = context.getProject().modules.find { it.name == moduleName }
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

            val appProps = module.source.main.resources.getOrAddFile("application.properties")
            appProps.addNewSettings(
                moduleElem,
                "spring.thymeleaf.prefix=classpath:/templates/thymeleaf/",
                "# ${moduleName} properties",
                "${moduleName}.main-host=http://localhost:8080",
                "${moduleName}.remember-me-key=replace-for-real-key",
                "${moduleName}.activation-enabled=true",
                "${moduleName}.re-captcha-in-login-enable=false",
                "${moduleName}.re-captcha-in-register-enable=false",
                "${moduleName}.re-captcha-site-key=${'$'}{RECAPTCHA_SITE_KEY}",
                "${moduleName}.re-captcha-secret-key=${'$'}{RECAPTCHA_SECRET_KEY}",
                "${moduleName}.re-captcha-threshold=0.5",
                "${moduleName}.email-host=${'$'}{EMAIL_HOST:localhost}",
                "${moduleName}.email-from=noreply@arusdev.me",
                "${moduleName}.email-password=${'$'}{EMAIL_PWD:${moduleName}EmailPwd}"
            )
        }

        return true
    }
}