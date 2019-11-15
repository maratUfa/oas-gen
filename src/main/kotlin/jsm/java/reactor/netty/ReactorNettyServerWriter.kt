package jsm.java.reactor.netty

import jsm.*
import jsm.java.*
import jsm.java.jackson.JacksonParserWriter
import jsm.java.jackson.JacksonWriterWriter
import java.util.*

class ReactorNettyServerWriter(
        private val basePackage: String
) : Writer<OpenApiSchema> {
    override fun write(items: Iterable<OpenApiSchema>): List<OutputFile> {
        val outputFiles = mutableListOf<OutputFile>()

        val javaDtoWriter = JavaDtoWriter(basePackage, mutableSetOf(), listOf(JacksonParserWriter(), JacksonWriterWriter()))

        items.forEach { openApiSchema ->
            val clientClassName = toClassName(basePackage, openApiSchema, "routes")
            val filePath = getFilePath(clientClassName)

            val importDeclarations = TreeSet<String>()

            importDeclarations.addAll(listOf(
                    "import java.util.function.Consumer;",
                    "import jsm.ReactorUtils;",
                    "import jsm.WriterUtils;",
                    "import reactor.core.publisher.Mono;",
                    "import reactor.netty.http.server.HttpServerRoutes;"
            ))

            val paths = openApiSchema.paths()
            val javaOperations = toJavaOperations(basePackage, paths)

            val operationMethods = javaOperations.map { javaOperation ->
                val args = (javaOperation.parameters.map { it.javaVariable } + javaOperation.requestVariable?.let { JavaVariable("Mono<${it.type}>", "requestBodyMono") })
                        .filterNotNull()
                        .joinToString(", ") {
                            "${it.type} ${it.name}"
                        }

                """|Mono<${javaOperation.responseVariable.type}> ${javaOperation.methodName}($args);
                   |
                """.trimMargin()
            }

            val routes = javaOperations.map { javaOperation ->
                val parameterDeclarations = javaOperation.parameters.mapIndexed { index, javaParameter ->
                    """${javaParameter.javaVariable.type} param$index = request.param("${javaParameter.name}");"""
                }
                val requestMonoDeclaration = javaOperation.requestVariable?.let { requestVariable ->
                    "Mono<${requestVariable.type}> requestMono = ReactorUtils.decode(request.receive().asByteArray(), new ${requestVariable.parserType}());"
                } ?: ""
                val parameterArgs = javaOperation.parameters.mapIndexed { index, _ -> "param$index" }
                val requestArg = javaOperation.requestVariable?.let { "requestMono" }
                val args = (parameterArgs + requestArg).filterNotNull().joinToString(", ")
                val responseMonoDeclaration =
                        "Mono<${javaOperation.responseVariable.type}> responseMono = ${javaOperation.methodName}($args);"
                val returnStatement = when (javaOperation.responseVariable.schema.type) {
                    JsonType.STRING -> "return response.sendString(responseMono);"
                    JsonType.OBJECT -> "return response.send(responseMono.map(it -> WriterUtils.toByteBuf(${javaOperation.responseVariable.type}.Writer.INSTANCE, it)));"
                    else -> TODO()
                }

                val routeMethod = javaOperation.operation.operationType.name.toLowerCase()
                """|.$routeMethod("${javaOperation.pathTemplate}", (request, response) -> {
                   |    ${parameterDeclarations.indentWithMargin(1)}
                   |    ${requestMonoDeclaration.indentWithMargin(1)}
                   |    $responseMonoDeclaration
                   |    $returnStatement
                   |})
                """.trimMargin()

            }

            val content = """
               |package ${getPackage(clientClassName)};
               |
               |${importDeclarations.indentWithMargin("")}
               |
               |public interface ${getSimpleName(clientClassName)} extends Consumer<HttpServerRoutes> {
               |
               |    ${operationMethods.indentWithMargin(1)}
               |
               |    @Override
               |    default void accept(HttpServerRoutes httpServerRoutes) {
               |        httpServerRoutes
               |            ${routes.indentWithMargin(3)}
               |        ;
               |    }
               |}
               |
            """.trimMargin()

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
