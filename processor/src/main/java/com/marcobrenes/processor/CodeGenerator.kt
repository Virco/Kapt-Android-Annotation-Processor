package com.marcobrenes.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.PUBLIC

internal object CodeGenerator {
    private const val PREFIX = "Kson_"

    fun generateClassWithCompanion(ksonClass: KsonClass): TypeSpec {
        val ksonClassName = ksonClass.typeElement.asClassName()
        val simpleName = ksonClassName.simpleName()
        val properties = ksonClass.properties

        return TypeSpec.classBuilder("$PREFIX$simpleName")
                .addModifiers(PUBLIC)
                .primaryConstructor(FunSpec.constructorBuilder().apply {
                       properties.forEach { (name, type) ->
                           addParameter(name, type.asType().asTypeName())
                       }
                }.build()).apply {
                    properties.forEach { (name, type) ->
                        addProperty(PropertySpec.builder(name, type.asType().asTypeName())
                                .initializer(name)
                                .build())
                    }
                }
                .addType(TypeSpec.companionObjectBuilder()
                        .addModifiers(PUBLIC)
                        .addFunction(generateAsJsonFun(ksonClassName, properties.map { it.first }, false)
                                .toBuilder()
                                .addParameter(ParameterSpec.builder(
                                        ksonClassName.simpleName().toLowerCase(), ksonClassName)
                                        .build())
                                .build())
                        .build())
                .build()
    }

    fun generateExtensionFunction(ksonClass: KsonClass): FunSpec {
        return generateAsJsonFun(
                ksonClassName = ksonClass.typeElement.asClassName(),
                propertyNames = ksonClass.properties.map { it.first },
                isExtension = true)
                .toBuilder()
                .receiver(ksonClass.typeElement.asClassName())
                .build()
    }

    private fun generateAsJsonFun(ksonClassName: ClassName, propertyNames: List<String>, isExtension: Boolean): FunSpec {
        val JSONObject = ClassName("org.json", "JSONObject")
        val JSONException = ClassName("org.json", "JSONException")
        return FunSpec.builder("asJson").apply {
            addModifiers(PUBLIC)
            addStatement("val jsonObject = $JSONObject()")
            returns(JSONObject)
            if (propertyNames.isNotEmpty()) {
                addCode("try {\n")
                propertyNames.forEach {
                    if (isExtension) {
                        addStatement("""
                        |   jsonObject.put("$it", $it)""".trimMargin())
                    } else {
                        addStatement("""
                        |   jsonObject.put("$it", ${ksonClassName.simpleName().toLowerCase()}.$it)
                        """.trimMargin())
                    }
                }
                addCode("""
                        |} catch(e: $JSONException) {
                        |   e.printStackTrace()
                        |}
                        |""".trimMargin())
            }
            addStatement("return jsonObject")
        }.build()
    }
}