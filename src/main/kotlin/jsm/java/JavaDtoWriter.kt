package jsm.java

import jsm.*
import java.util.*

data class JavaProperty(
        val name: String,
        val variableName: String,
        val type: String,
        val jsonSchema: JsonSchema,
        val internalVariableName: String
)

interface ExtraMemberWriter {
    fun write(
            importDeclarations: MutableSet<String>,
            javaProperties: List<JavaProperty>,
            dtoClassName: String
    ) :String
}

class JavaDtoWriter(
        private val basePackage: String,
        private val index: MutableSet<Fragment>,
        private val extraMemberWriters: List<ExtraMemberWriter>
) : Writer<JsonSchema> {
    override fun write(items: Iterable<JsonSchema>): List<OutputFile> {
        return items.flatMap { jsonSchema ->
            when (val type = jsonSchema.type) {
                JsonType.STRING -> emptyList()
                JsonType.NUMBER -> emptyList()
                JsonType.BOOLEAN -> emptyList()
                JsonType.OBJECT -> writeObject(jsonSchema)
                JsonType.ARRAY -> write(listOf(jsonSchema.items()
                        ?: error("there is not items for schema $jsonSchema")))
                else -> error("unknown type $type in $jsonSchema")
            }
        }
    }

    private fun writeObject(jsonSchema: JsonSchema): List<OutputFile> {
        if (index.contains(jsonSchema.fragment)) {
            return emptyList()
        }
        index.add(jsonSchema.fragment)

        val outputFiles = mutableListOf<OutputFile>()
        val propertySchemas = jsonSchema.properties().map { (_, propertySchema) -> propertySchema }
        val propertyFiles = write(propertySchemas)
        outputFiles.addAll(propertyFiles)

        val dtoClassName = toType(basePackage, jsonSchema)
        val filePath = getFilePath(dtoClassName)

        val javaProperties = jsonSchema.properties().entries.mapIndexed { index, (propertyName, propertySchema) ->
            val propertyType = toType(basePackage, propertySchema)
            JavaProperty(
                    propertyName,
                    toVariableName(propertyName),
                    propertyType,
                    propertySchema,
                    "p$index"
            )
        }

        val fieldDeclarations = javaProperties.map { javaProperty ->
            "public final ${javaProperty.type} ${javaProperty.variableName};"
        }

        val importDeclarations = TreeSet<String>()
        val constructorArgs = javaProperties.joinToString(", ") { "${it.type} ${it.variableName}" }
        val constructorAssignments = javaProperties.map { javaProperty ->
            "this.${javaProperty.variableName} = ${javaProperty.variableName};"
        }

        val extraMembers = extraMemberWriters.map { it.write(importDeclarations, javaProperties, dtoClassName) }

        val content = """
                       |package ${getPackage(dtoClassName)};
                       |
                       |${importDeclarations.indentWithMargin(0)}
                       |
                       |public class ${getSimpleName(dtoClassName)} {
                       |
                       |    ${fieldDeclarations.indentWithMargin("    ")}
                       |
                       |    public ${getSimpleName(dtoClassName)}($constructorArgs) {
                       |        ${constructorAssignments.indentWithMargin(2)}
                       |    }
                       |
                       |    ${extraMembers.indentWithMargin(1)}
                       |}
                       |
                    """.trimMargin()

        outputFiles.add(OutputFile(filePath, content))
        return outputFiles
    }

}
