package jsm.java.spring.mvc

import jsm.*
import jsm.java.*
import jsm.java.jackson.JacksonParserWriter
import jsm.java.jackson.JacksonWriterWriter
import java.util.*

class JavaSrpingMvcServerWriter(
        val basePackage: String
) : Writer<OpenApiSchema> {
    override fun write(items: Iterable<OpenApiSchema>): List<OutputFile> {
        val outputFiles = mutableListOf<OutputFile>()

        val javaDtoWriter = JavaDtoWriter(
                basePackage,
                mutableSetOf(),
                listOf(JacksonParserWriter(), JacksonWriterWriter())
        )
        items.forEach { openApiSchema ->
            val routesClassName = toClassName(basePackage, openApiSchema, "routes")
            val filePath = getFilePath(routesClassName)

            val importDeclarations = TreeSet<String>()

            importDeclarations.addAll(listOf(
                    "import org.springframework.http.ResponseEntity;",
                    "import org.springframework.web.bind.annotation.*;"
            ))

            val paths = openApiSchema.paths()
            val javaOperations = toJavaOperations(basePackage, paths)

            val operationMethods = javaOperations.map { javaOperation ->
                val mappingAnnotationName = when (val operationType = javaOperation.operation.operationType) {
                    OperationType.GET -> "GetMapping"
                    OperationType.POST -> "PostMapping"
                    else -> error("Unsupported operation type $operationType")
                }
                val consumesPart = when {
                    javaOperation.requestVariable != null -> """, consumes = "${javaOperation.responseVariable.contentType}""""
                    else -> ""
                }
                val requestBodyArg = javaOperation.requestVariable?.let { requestVariable ->
                    "@RequestBody ${requestVariable.type} ${toVariableName(getSimpleName(requestVariable.type))}"
                }
                val parameterArgs = javaOperation.parameters.map { javaParameter ->
                    """@PathVariable("${javaParameter.name}") ${javaParameter.javaVariable.type} ${javaParameter.javaVariable.name}"""
                }
                val methodArgs = (parameterArgs + requestBodyArg).filterNotNull().joinToString(", ")
                """|@$mappingAnnotationName(path = "${javaOperation.pathTemplate}", produces = "${javaOperation.responseVariable.contentType}"$consumesPart)
                   |ResponseEntity<${javaOperation.responseVariable.type}> ${javaOperation.methodName}($methodArgs);
                   |
                """.trimMargin()
            }

            val content = """
               |package ${getPackage(routesClassName)};
               |
               |${importDeclarations.indentWithMargin(0)}
               |
               |public interface ${getSimpleName(routesClassName)} {
               |
               |    ${operationMethods.indentWithMargin(1)}
               |
               |}
               |
            """.trimMargin()

            val configurationWriter = ConfigurationWriter()
            val configuraionClassSimpleName = getSimpleName(toClassName(basePackage, openApiSchema, "web-mvc-configuration"))
            val configurationOutputFile = configurationWriter.write(basePackage, configuraionClassSimpleName, javaOperations)
            outputFiles.add(configurationOutputFile)

            val dtoSchemas = mutableListOf<JsonSchema>()
            dtoSchemas.addAll(javaOperations.map { it.responseVariable.schema })
            dtoSchemas.addAll(javaOperations.mapNotNull { it.requestVariable?.schema })
            val dtoFiles = javaDtoWriter.write(dtoSchemas)
            outputFiles.addAll(dtoFiles)
            outputFiles.add(OutputFile(filePath, content))
        }

        return outputFiles
    }
}