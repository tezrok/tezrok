package io.tezrok.auth

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.ModuleElem
import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.addNewSettings

internal class AuthFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val moduleElem = context.getProject().modules.find { it.name == module.getName() }
        val appPackageRoot = module.source.main.java.applicationPackageRoot

        if (appPackageRoot != null) {
            val pom = module.pom
            pom.addDependency("org.springframework.boot:spring-boot-starter-thymeleaf:${'$'}{spring-boot.version}")

            val apiDir = appPackageRoot.getOrAddJavaDirectory("dto").getOrAddJavaDirectory("api")
            context.addFile(apiDir, "/templates/auth/LoginApiResult.java.vm")
            context.addFile(apiDir, "/templates/auth/PasswordRecoverDto.java.vm")
            context.addFile(apiDir, "/templates/auth/PasswordRecoverFinishDto.java.vm")
            context.addFile(apiDir, "/templates/auth/UserLoginDto.java.vm")
            context.addFile(apiDir, "/templates/auth/UserRegisterDto.java.vm")

            val serviceDir = appPackageRoot.getOrAddJavaDirectory("service")
            context.addFile(serviceDir, "/templates/auth/AuthService.java.vm")
            context.addFile(serviceDir, "/templates/auth/CurrentAuthService.java.vm")

            val webDir = appPackageRoot.getOrAddJavaDirectory("web")
            context.addFile(webDir, "/templates/auth/AuthController.java.vm")

            val restDir = webDir.getOrAddJavaDirectory("rest")
            context.addFile(restDir, "/templates/auth/AuthApiController.java.vm")

            val thymeleafDir = module.source.main.resources.getOrAddDirectory("templates/thymeleaf")
            val values = mapOf("productName" to context.getProject().productName.ifBlank { context.getProject().name })
            context.addFile(thymeleafDir, "/templates/auth/templates/index.html.vm", values)
            context.addFile(thymeleafDir, "/templates/auth/templates/error.html.vm", values)
            context.addFile(thymeleafDir, "/templates/auth/templates/login.html.vm", values)
            context.addFile(thymeleafDir, "/templates/auth/templates/register.html.vm", values)
            context.addFile(thymeleafDir, "/templates/auth/templates/recover.html.vm", values)
            context.addFile(thymeleafDir, "/templates/auth/templates/recover-finish.html.vm", values)

            val staticDir = module.source.main.resources.getOrAddDirectory("static")
            context.addFile(staticDir, "/templates/auth/static/style.css")
            context.addFile(staticDir, "/templates/auth/static/favicon.png")
            context.addFile(staticDir, "/templates/auth/static/favicon.ico")
            //context.addFile(staticDir, "/templates/auth/static/service-worker.js") TODO: add service worker

            addAppProperties(module, moduleElem)
        }

        return true
    }

    private fun addAppProperties(
        module: ModuleNode,
        moduleElem: ModuleElem?
    ) {
        val moduleName = module.getName()
        val appProps = module.source.main.resources.getOrAddFile("application.properties")
        appProps.addNewSettings(
            moduleElem,
            "spring.thymeleaf.prefix=classpath:/templates/thymeleaf/",
            "# ${moduleName} properties",
            "${moduleName}.main-host=${'$'}{MAIN_HOST:http://localhost:8080}",
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
}
