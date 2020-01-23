package jsm.java.jackson

import jsm.JsonSchema
import jsm.JsonType
import jsm.LOCAL_DATE_TIME_FORMAT
import jsm.indentWithMargin
import jsm.java.ExtraMemberWriter
import jsm.java.JavaProperty

fun scalarWriteMethod(jsonSchema: JsonSchema, property: String): String {
    return when (val type = jsonSchema.type) {
        JsonType.STRING -> when (jsonSchema.format) {
            LOCAL_DATE_TIME_FORMAT -> "jsonGenerator.writeString(value.$property.toString())"
            else -> "jsonGenerator.writeString(value.$property)"
        }
        JsonType.NUMBER -> "jsonGenerator.writeNumber(value.$property)"
        else -> error("Unsupported type $type")
    }
}

class JacksonWriterWriter : ExtraMemberWriter {
    override fun write(
            importDeclarations: MutableSet<String>,
            javaProperties: List<JavaProperty>,
            dtoClassName: String
    ): String {
        importDeclarations.add("import com.fasterxml.jackson.core.JsonGenerator;")

        val propertyBlocks = javaProperties.map { javaProperty ->
            val jsonType = javaProperty.jsonSchema.type

            val writeExpression = if (javaProperty.jsonSchema.type.scalar) {
                val writeMethod = scalarWriteMethod(javaProperty.jsonSchema, javaProperty.variableName)
                "$writeMethod;"
            } else {
                "${javaProperty.type}.Writer.INSTANCE.write(jsonGenerator, value.${javaProperty.variableName});"
            }
            """|if (value.${javaProperty.variableName} != null) {
               |    jsonGenerator.writeFieldName("${javaProperty.name}");
               |    $writeExpression
               |}
            """.trimMargin()

        }

        return """
           |public static class Writer implements jsm.Writer<$dtoClassName> {
           |    public static final Writer INSTANCE = new Writer();
           |
           |    @Override
           |    public void write(JsonGenerator jsonGenerator, $dtoClassName value) throws IOException {
           |        jsonGenerator.writeStartObject();
           |        ${propertyBlocks.indentWithMargin(2)}
           |        jsonGenerator.writeEndObject();
           |    }
           |}
           |
        """.trimMargin()
    }
}