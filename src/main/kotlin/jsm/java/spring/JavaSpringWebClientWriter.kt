package jsm.java.spring

import jsm.*
import jsm.OperationType.GET
import jsm.OperationType.POST
import jsm.java.*
import jsm.java.jackson.JacksonParserWriter
import jsm.java.jackson.JacksonWriterWriter
import java.util.*

class JavaSpringWebClientWriter(
        private val basePackage: String
) : Writer<OpenApiSchema> {
    override fun write(items: Iterable<OpenApiSchema>): List<OutputFile> {
        val outputFiles = mutableListOf<OutputFile>()

        val javaDtoWriter = JavaDtoWriter(basePackage, mutableSetOf(), listOf(JacksonParserWriter(), JacksonWriterWriter()))

        items.forEach { openApiSchema ->
            val clientClassName = toClassName(basePackage, openApiSchema, "client")
            val filePath = getFilePath(clientClassName)

            val importDeclarations = TreeSet<String>()
            val staticFiledDeclarationBuilder = StringBuilder()
            val memberFieldDeclarationBuilder = StringBuilder()
            val constructorFieldInitializationBuilder = StringBuilder()
            val methodBuilder = StringBuilder()

            importDeclarations.add("import org.springframework.web.client.RestOperations;")
            memberFieldDeclarationBuilder
                    .appendln("private final RestOperations restOperations;")

            constructorFieldInitializationBuilder
                    .appendln("this.restOperations = restOperations;")

            val paths = openApiSchema.paths()
            val dtoSchemas = mutableListOf<JsonSchema>()

            paths.pathItems().forEach { (pathTemplate, pathItem) ->
                pathItem.operations().forEach { operation ->
                    val contentSchemas = buildOperation(
                            operation,
                            pathTemplate,
                            importDeclarations,
                            staticFiledDeclarationBuilder,
                            memberFieldDeclarationBuilder,
                            constructorFieldInitializationBuilder,
                            methodBuilder
                    )
                    dtoSchemas.addAll(contentSchemas)
                }
            }

            val dtoFiles = javaDtoWriter.write(dtoSchemas)
            outputFiles.addAll(dtoFiles)

            val content = """
                |package ${getPackage(clientClassName)};
                |
                |${importDeclarations.indentWithMargin("")}
                |
                |public class ${getSimpleName(clientClassName)} {
                |
                |    ${staticFiledDeclarationBuilder.toString().indentWithMargin("    ")}
                |    ${memberFieldDeclarationBuilder.toString().indentWithMargin("    ")}
                |
                |    public ${getSimpleName(clientClassName)}(RestOperations restOperations, String baseUrl) {
                |        ${constructorFieldInitializationBuilder.toString().indentWithMargin("        ")}
                |    }
                |
                |    ${methodBuilder.toString().indentWithMargin("    ")}
                |
                |}
                |
            """.trimMargin()

            outputFiles.add(OutputFile(filePath, content))
        }

        return outputFiles
    }

    private fun buildOperation(
            operation: Operation,
            pathTemplate: String,
            importDeclarations: TreeSet<String>,
            staticFiledDeclarationBuilder: StringBuilder,
            memberFieldDeclarationBuilder: StringBuilder,
            constructorFieldInitializationBuilder: StringBuilder,
            methodBuilder: StringBuilder
    ): List<JsonSchema> {
        val operationId = operation.operationId
        val publicMethodName = toMethodName(operationId)
        val privateMethodName = toMethodName(operationId, "impl")
        val uriTemplateFieldName = toVariableName(operationId, "uri", "template")
        val parameters = operation.parameters()

        val requestBody = operation.requestBody()

        val parameterVariables = parameters.map { parameter ->
            val variableName = toVariableName(parameter.name)
            val type = toType(basePackage, parameter.schema())
            Variable(variableName, type)
        }

        val requestBodyVariable = requestBody?.let {
            val mediaTypeObject = it.content()["application/json"] ?: error("json media type is required")
            val requestSchema = mediaTypeObject.schema()
            val type = toType(basePackage, requestSchema)
            Variable("requestBody", type)
        }

        val requestSchema = requestBody?.let {
            val mediaTypeObject = it.content()["application/json"] ?: error("json media type is required")
            mediaTypeObject.schema()
        }

        val publicMethodDeclarationParameterArgs = parameterVariables
                .joinToString(", ") {
                    "${it.type} ${it.name}"
                }.nullIfEmpty()
        val requestBodyDeclarationArg = requestBodyVariable?.let { "${it.type} ${it.name}" }
        val requestBodyInvocationArg = requestBodyVariable?.name
        val publicMethodDeclarationArgs = listOfNotNull(publicMethodDeclarationParameterArgs, requestBodyDeclarationArg)
                .joinToString(", ")
        val privateMethodDeclarationParameterArgs = parameterVariables
                .mapIndexed { index, variable ->
                    "${variable.type} arg${index}"
                }.joinToString(", ")
                .nullIfEmpty()
        val privateMethodDeclarationArgs = listOfNotNull(privateMethodDeclarationParameterArgs, requestBodyDeclarationArg)
                .joinToString(", ")
        val privateMethodInvocationParameterArgs = parameterVariables
                .joinToString(", ") { it.name }
                .nullIfEmpty()
        val privateMethodInvocationArgs = listOfNotNull(privateMethodInvocationParameterArgs, requestBodyInvocationArg)
                .joinToString(", ")
        val uriTemplateInvocationArgs = parameterVariables.mapIndexed { index, _ ->
            "arg${index}"
        }.joinToString(", ")

        val response200 = operation.responses().byCode()[HttpResponseCode.CODE_200]
                ?: error("There is no response 200 in $operation")
        val jsonMediaType = response200.content()["application/json"]
                ?: error("There is no application/json media type in $response200")
        val contentSchema = jsonMediaType.schema()

        val contentType = toType(basePackage, contentSchema)

        memberFieldDeclarationBuilder.appendln(
                "private final UriTemplate $uriTemplateFieldName;"
        )
        importDeclarations.add("import java.net.URI;")
        importDeclarations.add("import org.springframework.http.ResponseEntity;")
        importDeclarations.add("import org.springframework.http.RequestEntity;")
        importDeclarations.add("import org.springframework.web.util.UriTemplate;")
        constructorFieldInitializationBuilder.appendln(
                """this.$uriTemplateFieldName = new UriTemplate(baseUrl + "$pathTemplate");"""
        )

        val typeReferenceFieldName = toStaticFinalFieldName(operationId, "return", "type")
        staticFiledDeclarationBuilder.appendln(
                "private static final ParameterizedTypeReference<$contentType> $typeReferenceFieldName = new ParameterizedTypeReference<>() {};"
        )

        val bodyInvocation = if (requestBodyVariable != null) ".body(requestBody)" else ""
        val requestType = requestBodyVariable?.type ?: "Void"
        val requestMethod = when (operation.operationType) {
            GET -> "get"
            POST -> "post"
        }

        methodBuilder.append("""
            public ResponseEntity<$contentType> $publicMethodName($publicMethodDeclarationArgs) {
                return $privateMethodName($privateMethodInvocationArgs); 
            }

            private ResponseEntity<$contentType> $privateMethodName($privateMethodDeclarationArgs) {
                URI uri = $uriTemplateFieldName.expand($uriTemplateInvocationArgs);
                RequestEntity<$requestType> requestEntity = RequestEntity.$requestMethod(uri)$bodyInvocation.build();
                return restTemplate.exchange(requestEntity, $typeReferenceFieldName);
            }

            """.trimIndent()
        )

        return listOfNotNull(contentSchema, requestSchema)
    }

}
