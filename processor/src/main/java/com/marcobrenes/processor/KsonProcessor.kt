package com.marcobrenes.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic.Kind.ERROR

private const val KSON_CANONICAL = "com.marcobrenes.api.Kson"
private const val KSON_SIMPLE = "Kson"
private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

internal class KsonClass(val typeElement: TypeElement,
                         val properties: List<Pair<String, VariableElement>>)

internal class ClassPackageNotFoundException(className: String)
    : Exception("The package of $className class has no name")

@AutoService(Processor::class)
@SupportedAnnotationTypes(KSON_CANONICAL)
class KsonProcessor: AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val annotatedTypes = roundEnv.getElementsAnnotatedWith(
                processingEnv.elementUtils.getTypeElement(KSON_CANONICAL))

        if (annotatedTypes.isEmpty()) return false

        val kaptGeneratedDirPath = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?.replace("kaptKotlin", "kapt") ?: run {
            val message = "Can't find the target directory for generated Kotlin files."
            processingEnv.messager.printMessage(ERROR, message)
            return false
        }

        generateAsExtensionFunctionFile(annotatedTypes, kaptGeneratedDirPath)
        generateAsKsonClassFile(annotatedTypes, kaptGeneratedDirPath)
        return true
    }

    private fun generateAsExtensionFunctionFile(annotatedTypes: MutableSet<out Element>, kaptGeneratedDirPath: String) {
        val fileSpecBuilder = FileSpec.builder("com.marcobrenes.annotationprocessor", "KsonExt")
        annotatedTypes.asSequence()
                .filterIsInstance<TypeElement>()
                .filter(::isValidClass)
                .map(::buildAnnotatedClass)
                .map(CodeGenerator::generateExtensionFunction)
                .map(fileSpecBuilder::addFunction)
                .toList()
        fileSpecBuilder.build().writeTo(File(kaptGeneratedDirPath))
    }

    private fun generateAsKsonClassFile(annotatedTypes: MutableSet<out Element>, kaptGeneratedDirPath: String) {
        annotatedTypes.asSequence()
                .filterIsInstance<TypeElement>()
                .filter(::isValidClass)
                .map(::buildAnnotatedClass)
                .map(::generateClassWithCompanion)
                .forEach { it.writeTo(File(kaptGeneratedDirPath)) }
    }

    private fun isValidClass(annotatedType: TypeElement): Boolean {
        // if class is not public
        if (!annotatedType.modifiers.contains(PUBLIC)) {
            val message = "Classes annotated with @$KSON_SIMPLE must be public."
            processingEnv.messager.printMessage(ERROR, message, annotatedType)
            return false
        }

        // if class is an abstract class
        if (annotatedType.modifiers.contains(ABSTRACT)) {
            val message = "Classes annotated with @$KSON_SIMPLE must not be abstract."
            processingEnv.messager.printMessage(ERROR, message, annotatedType)
            return false
        }

        return true
    }

    private fun buildAnnotatedClass(typeElement: TypeElement): KsonClass {
        val propertyNames = typeElement.enclosedElements
                .filterIsInstance<VariableElement>()
                .map { it.simpleName.toString() to it }
        return KsonClass(typeElement, propertyNames)
    }

    private fun generateClassWithCompanion(annotatedClass: KsonClass): FileSpec {
        val packageName = getPackageName(processingEnv.elementUtils, annotatedClass.typeElement)
        val generatedClass = CodeGenerator.generateClassWithCompanion(annotatedClass)
        return FileSpec.builder(packageName, generatedClass.name!!)
                .addType(generatedClass).build()
    }

}

internal fun getPackageName(elementUtils: Elements, typeElement: TypeElement): String {
    val pkg = elementUtils.getPackageOf(typeElement)
    if (pkg.isUnnamed){
        throw ClassPackageNotFoundException(typeElement.simpleName.toString())
    }
    return pkg.qualifiedName.toString()
}