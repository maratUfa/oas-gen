package jsm.java.jackson

import jsm.JsonType
import jsm.indentWithMargin
import jsm.java.ExtraMemberWriter
import jsm.java.JavaProperty

val jacksonScalarWriteMethods = mapOf(
        JsonType.STRING to "writeString",
        JsonType.NUMBER to "writeNumber"
)

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
                val writeMethod = jacksonScalarWriteMethods[jsonType]
                        ?: error("Can't find write method for $jsonType")
                "jsonGenerator.$writeMethod(value.${javaProperty.variableName});"
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