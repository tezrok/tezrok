package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import io.tezrok.util.PathUtil.NEW_LINE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JavaFileNodeTest {
    @Test
    fun testJavaFileWithoutParent() {
        val javaFile = JavaFileNode("MainApp")

        assertNull(javaFile.getParent())
        assertEquals("MainApp.java", javaFile.getName())
        assertEquals("/MainApp.java", javaFile.getPath())
        assertEquals("/", javaFile.getPackagePath())
        assertNull(javaFile.getJavaRoot())
        assertEquals("public class MainApp {$NEW_LINE}$NEW_LINE", javaFile.asString())
        val clazz = javaFile.getRootClass()
        assertEquals("MainApp", clazz.getName())
        assertEquals(1, clazz.getParent().types.size)
        assertEquals(0, clazz.getMethods().count())

        clazz.addMethod("main")
                .withModifiers(Modifier.Keyword.PROTECTED)
                .addParameter("String[]", "args")
                .setReturnType(Int::class.java)

        assertEquals("""public class MainApp {

    protected int main(String[] args) {
    }
}
""", javaFile.asString())

        assertEquals(1, clazz.getParent().types.size)
        assertEquals(1, clazz.getMethods().count())
    }

    @Test
    fun testJavaFileSetContentWithTheSameClassName() {
        val javaFile = JavaFileNode("MainApp")
        val clazz = javaFile.getRootClass()
        clazz.addMethod("main")
                .addParameter("String[]", "args")
                .setReturnType(String::class.java)
        assertEquals("/", javaFile.getPackagePath())

        javaFile.setString("class MainApp { }")

        assertEquals("MainApp.java", javaFile.getName())
        assertEquals("/MainApp.java", javaFile.getPath())
        assertEquals("/", javaFile.getPackagePath())
        assertNull(javaFile.getJavaRoot())
        val clazzActual = javaFile.getRootClass()
        assertEquals("MainApp", clazzActual.getName())
        assertEquals(1, clazzActual.getParent().types.size)
        assertEquals(0, clazzActual.getMethods().count())
        assertEquals("class MainApp {$NEW_LINE" +
                "}$NEW_LINE", javaFile.asString())
    }

    @Test
    fun testJavaFileSetContentWithAnotherClassName() {
        val javaFile = JavaFileNode("MainApp")
        val clazz = javaFile.getRootClass()
        clazz.addMethod("main")
                .addParameter("String[]", "args")
                .setReturnType(String::class.java)
        assertEquals("/", javaFile.getPackagePath())

        javaFile.setString("abstract class Foo { }")

        assertEquals("MainApp.java", javaFile.getName())
        assertEquals("/MainApp.java", javaFile.getPath())
        assertEquals("/", javaFile.getPackagePath())
        assertNull(javaFile.getJavaRoot())
        val clazzActual = javaFile.getRootClass()
        assertEquals("Foo", clazzActual.getName())
        assertEquals(1, clazzActual.getParent().types.size)
        assertEquals(0, clazzActual.getMethods().count())
        assertEquals("abstract class Foo {$NEW_LINE}$NEW_LINE", javaFile.asString())
    }

    @Test
    fun testJavaFileSetContentWithAnotherPackage() {
        val javaFile = JavaFileNode("MainApp")
        val clazz = javaFile.getRootClass()
        clazz.addMethod("main")
                .addParameter("String[]", "args")
                .setReturnType(String::class.java)
        assertEquals("/", javaFile.getPackagePath())

        javaFile.setString("package foo.bar.xyz;${NEW_LINE} class Bar { }")

        assertEquals("MainApp.java", javaFile.getName())
        assertEquals("/MainApp.java", javaFile.getPath())
        assertEquals("/", javaFile.getPackagePath())
        assertNull(javaFile.getJavaRoot())
        val clazzActual = javaFile.getRootClass()
        assertEquals("Bar", clazzActual.getName())
        assertEquals(1, clazzActual.getParent().types.size)
        assertEquals(0, clazzActual.getMethods().count())
        assertEquals("class Bar {$NEW_LINE}$NEW_LINE", javaFile.asString())
    }

    @Test
    fun testJavaFileSetContentButPackageMustRemainOld() {
        val javaCom = JavaDirectoryNode("com")
        val javaExample = JavaDirectoryNode("example", javaCom)
        val javaFile = JavaFileNode("MainApp", javaExample)
        val clazz = javaFile.getRootClass()
        clazz.addMethod("main")
                .addParameter("String[]", "args")
                .setReturnType(String::class.java)
        assertEquals("/com/example", javaFile.getPackagePath())

        javaFile.setString("package foo.bar.xyz;${NEW_LINE} class Bar { }")

        assertEquals("MainApp.java", javaFile.getName())
        assertEquals("/com/example/MainApp.java", javaFile.getPath())
        assertEquals("/com/example", javaFile.getPackagePath())
        assertNull(javaFile.getJavaRoot())
        val clazzActual = javaFile.getRootClass()
        assertEquals("Bar", clazzActual.getName())
        assertEquals(1, clazzActual.getParent().types.size)
        assertEquals(0, clazzActual.getMethods().count())
        assertEquals("package com.example;$NEW_LINE${NEW_LINE}class Bar {$NEW_LINE}$NEW_LINE", javaFile.asString())
    }
}