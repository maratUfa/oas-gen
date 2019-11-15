package jsm.java

import jsm.HttpResponseCode
import jsm.JsonSchema
import jsm.Operation
import jsm.Paths

data class JavaVariable(
        val type: String,
        val name: String
)

data class JavaParameter(
        val name: String,
        val javaVariable: JavaVariable
)

data class RequestVariable(
        val type: String,
        val parserType: String,
        val schema: JsonSchema,
        val contentType: String
)

data class ResponseVariable(
        val type: String,
        val schema: JsonSchema,
        val contentType: String
)

data class JavaOperation(
        val operation: Operation,
        val pathTemplate: String,
        val requestVariable: RequestVariable?,
        val responseVariable: ResponseVariable,
        val methodName: String,
        val parameters: List<JavaParameter>
)

fun toJavaOperations(basePackage: String, paths: Paths): List<JavaOperation> {
    return paths.pathItems().flatMap { (pathTemplate, pathItem) ->
        pathItem.operations().map { operation ->
            val requestVariable = operation.requestBody()?.let {
                val mediaTypeObject = it.content()["application/json"]
                        ?: error("media type application/json is required")
                val requestSchema = mediaTypeObject.schema()
                val type = toType(basePackage, requestSchema)
                RequestVariable(
                        type,
                        "$type.Parser",
                        requestSchema,
                        "application/json"
                )
            }

            val response = operation.responses().byCode()[HttpResponseCode.CODE_200]
                    ?: error("response 200 is required")
            val responseMediaTypeObject = response.content()["application/json"]
                    ?: error("media type application/json is required")
            val responseSchema = responseMediaTypeObject.schema()
            val responseType = toType(basePackage, responseSchema)
            val methodName = toMethodName(operation.operationId)
            val javaParameters = operation.parameters().map {
                val parameterType = toType(basePackage, it.schema())
                JavaParameter(it.name, JavaVariable(parameterType, toVariableName(it.name)))
            }

            JavaOperation(
                    operation,
                    pathTemplate,
                    requestVariable,
                    ResponseVariable(responseType, responseSchema, "application/json"),
                    methodName,
                    javaParameters
            )
        }
    }
}
