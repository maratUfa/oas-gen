package jsm.java.jackson

import jsm.JsonSchema
import jsm.JsonType
import jsm.OutputFile
import jsm.indentWithMargin
import jsm.java.*
import java.util.*

public fun JsonSchema.jointProperties(): Map<String, JsonSchema> {
    if (this.type != JsonType.OBJECT) error("This method can be called only for objects")
    val jointProperties = mutableMapOf<String, JsonSchema>() //this.properties().toMutableMap()

    allOf()?.forEach { includedSchema ->
        if (includedSchema.type != JsonType.OBJECT) error("Included schema should be an object")
        includedSchema.jointProperties().forEach { (includedPropertyName, includedPropertySchema) ->
            addJointProperty(jointProperties, includedPropertyName, includedPropertySchema)
        }
    }

    this.properties().forEach { (includedPropertyName, includedPropertySchema) ->
        addJointProperty(jointProperties, includedPropertyName, includedPropertySchema)
    }
    return jointProperties
}

private fun JsonSchema.addJointProperty(
        jointProperties: MutableMap<String, JsonSchema>,
        includedPropertyName: String,
        includedPropertySchema: JsonSchema) {
    if (jointProperties.containsKey(includedPropertyName))
        error("Found duplicated property $includedPropertyName in schema $this")
    jointProperties[includedPropertyName] = includedPropertySchema
}

class ObjectConverterMatcher(val basePackage: String) : ConverterMatcher {
    data class JavaProperty(
            val name: String,
            val variableName: String,
            val type: String,
            val jsonSchema: JsonSchema,
            val internalVariableName: String
    )

    class JacksonParserWriter(private val converterRegistry: ConverterRegistry) {
        private data class ParserPair(
                val valueType: String,
                val parserCreateExpression: String
        ) : Comparable<ParserPair> {
            override fun compareTo(other: ParserPair) =
                    this.parserCreateExpression.compareTo(other.parserCreateExpression)
        }

        fun write(
                jsonSchema: JsonSchema,
                importDeclarations: MutableSet<String>,
                dtoClassName: String
        ): String {
            importDeclarations.addAll(listOf(
                    "import com.fasterxml.jackson.core.JsonGenerator;",
                    "import com.fasterxml.jackson.core.JsonToken;",
                    "import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;",
                    "import java.io.IOException;",
                    "import jsm.NonBlockingParser;",
                    "import jsm.ObjectParserState;",
                    "import jsm.ParseResult;"
            ))

            val jointProperties = jsonSchema.jointProperties()
            val builderPropertyDeclarations = jointProperties.entries.mapIndexed { index, (propertyName, propertySchema) ->
                val converterWriter = converterRegistry[propertySchema]
                "private ${converterWriter.valueType(converterRegistry)} p$index; // $propertyName"
            }

            val parserPairs = jointProperties.map { (_, propertySchema) ->
                val converterWriter = converterRegistry[propertySchema]
                ParserPair(converterWriter.valueType(converterRegistry), converterWriter.parserCreateExpression(converterRegistry))
            }.toSortedSet()

            val propertyParserDeclarations = parserPairs.mapIndexed { index, parserPair ->
                "private final jsm.NonBlockingParser<${parserPair.valueType}> parser$index = ${parserPair.parserCreateExpression};"
            }

            val parseValueCases = jointProperties.entries.mapIndexed { index, (propertyName, propertySchema) ->
                val converterWriter = converterRegistry[propertySchema]

                val parserIndex = parserPairs.indexOfFirst { it.parserCreateExpression == converterWriter.parserCreateExpression(converterRegistry) }
                """|case "$propertyName":
               |    if (parser$parserIndex.parseNext(jsonParser)) {
               |        ParseResult<${converterWriter.valueType(converterRegistry)}> parseResult = parser${parserIndex}.build();
               |        this.p${index} = parseResult.getValue();
               |        objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
               |    }
               |    break;
            """.trimMargin()
            }

            val constructorArgs = jointProperties.entries
                    .mapIndexed { index, _ -> index }
                    .joinToString(", ") { "this.p${it}" }

            val resetFieldExpressions = jointProperties.entries.mapIndexed { index, _ -> "this.p$index = null;" }

            return """
               |public static class Parser implements NonBlockingParser<$dtoClassName> {
               |
               |    private ObjectParserState objectParserState = ObjectParserState.PARSE_START_OBJECT_OR_END_ARRAY_OR_NULL;
               |    private java.lang.String currentField;
               |    ${builderPropertyDeclarations.indentWithMargin(1)}
               |    ${propertyParserDeclarations.indentWithMargin(1)}
               |
               |    @Override
               |    public boolean parseNext(NonBlockingJsonParser jsonParser) throws IOException {
               |        while (jsonParser.currentToken() == null || jsonParser.currentToken() != JsonToken.NOT_AVAILABLE) {
               |            JsonToken token;
               |            switch (objectParserState) {
               |                case PARSE_START_OBJECT_OR_END_ARRAY_OR_NULL:
               |                    if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
               |                        switch (token) {
               |                            case START_OBJECT:
               |                                ${resetFieldExpressions.indentWithMargin(8)}
               |                                objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
               |                                break;
               |                            case END_ARRAY:
               |                                objectParserState = ObjectParserState.FINISHED_ARRAY;
               |                                return true;
               |                            case VALUE_NULL:
               |                                objectParserState = ObjectParserState.FINISHED_NULL;
               |                                return true;
               |                            default:
               |                                throw new RuntimeException("Unexpected token " + token);
               |                        }
               |                    }
               |                    break;
               |                case PARSE_FIELD_NAME_OR_END_OBJECT:
               |                    if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
               |                        switch (token) {
               |                            case FIELD_NAME:
               |                                currentField = jsonParser.getCurrentName();
               |                                objectParserState = ObjectParserState.PARSE_FIELD_VALUE;
               |                                break;
               |                            case END_OBJECT:
               |                                objectParserState = ObjectParserState.FINISHED_VALUE;
               |                                return true;
               |                            default:
               |                                throw new RuntimeException("Unexpected token " + token);
               |                        }
               |                    }
               |                    break;
               |                case PARSE_FIELD_VALUE:
               |                    switch (currentField) {
               |                        ${parseValueCases.indentWithMargin(6)}
               |                        default:
               |                            throw new UnsupportedOperationException("Unexpected field " + currentField);
               |                    }
               |                    break;
               |                default:
               |                    throw new RuntimeException("unexpected state " + objectParserState);
               |            }
               |        }
               |        return false;
               |    }
               |
               |    @Override
               |    public ParseResult<$dtoClassName> build() {
               |        switch (objectParserState) {
               |            case FINISHED_VALUE:
               |                objectParserState = ObjectParserState.PARSE_START_OBJECT_OR_END_ARRAY_OR_NULL;
               |                return new ParseResult.Value<>(new $dtoClassName($constructorArgs));
               |            case FINISHED_ARRAY:
               |                objectParserState = ObjectParserState.PARSE_START_OBJECT_OR_END_ARRAY_OR_NULL;
               |                return ParseResult.endArray();
               |            case FINISHED_NULL:
               |                objectParserState = ObjectParserState.PARSE_START_OBJECT_OR_END_ARRAY_OR_NULL;
               |                return ParseResult.nullValue();
               |            default:
               |                throw new IllegalStateException("Parsing is not completed");
               |        }
               |    }
               |
               |}
               |
            """.trimMargin()
        }
    }

    class JacksonWriterWriter(private val converterRegistry: ConverterRegistry) {
        private data class WriterPair(
                val valueType: String,
                val writerCreateExpression: String
        ) : Comparable<WriterPair> {
            override fun compareTo(other: WriterPair) =
                    this.writerCreateExpression.compareTo(other.writerCreateExpression)
        }

        fun write(
                jsonSchema: JsonSchema,
                importDeclarations: MutableSet<String>,
                dtoClassName: String
        ): String {
            importDeclarations.addAll(listOf(
                    "import com.fasterxml.jackson.core.JsonGenerator;"
            ))

            val jointProperties = jsonSchema.jointProperties()
            val writerPairs = jointProperties.map { (_, propertySchema) ->
                val converterWriter = converterRegistry[propertySchema]
                WriterPair(converterWriter.valueType(converterRegistry), converterWriter.writerCreateExpression(converterRegistry))
            }.toSortedSet()

            val writerDeclarations = writerPairs.mapIndexed { index, parserPair ->
                "private static final jsm.Writer<${parserPair.valueType}> WRITER_$index = ${parserPair.writerCreateExpression};"
            }

            val propertyBlocks = jointProperties.map { (propertyName, propertySchema) ->
                val converterWriter = converterRegistry[propertySchema]

                val fieldName = toVariableName(propertyName)
                val writerIndex = writerPairs.indexOfFirst { it.writerCreateExpression == converterWriter.writerCreateExpression(converterRegistry) }
                """|if (value.$fieldName != null) {
               |    jsonGenerator.writeFieldName("$propertyName");
               |    WRITER_$writerIndex.write(jsonGenerator, value.$fieldName);
               |}
            """.trimMargin()
            }

            return """
           |public static class Writer implements jsm.Writer<$dtoClassName> {
           |    public static final Writer INSTANCE = new Writer();
           |    ${writerDeclarations.indentWithMargin(1)}
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

    override fun match(jsonSchema: JsonSchema): ConverterWriter? {
        return when (jsonSchema.type) {
            is JsonType.OBJECT -> object : ConverterWriter {
                override val jsonSchema = jsonSchema
                override fun valueType(converterRegistry: ConverterRegistry) = toJavaClassName(basePackage, jsonSchema)
                override fun parserCreateExpression(converterRegistry: ConverterRegistry) = "new ${valueType(converterRegistry)}.Parser()"
                override fun writerCreateExpression(converterRegistry: ConverterRegistry) = "${valueType(converterRegistry)}.Writer.INSTANCE"
                override fun generate(converterRegistry: ConverterRegistry): ConverterWriter.Result {
                    val filePath = getFilePath(valueType(converterRegistry))

                    val jointProperties = jsonSchema.jointProperties()
                    val javaProperties = jointProperties.entries.mapIndexed { index, (propertyName, propertySchema) ->
                        val propertyConverterWriter = converterRegistry[propertySchema]
                        val propertyType = propertyConverterWriter.valueType(converterRegistry)
                        JavaProperty(
                                propertyName,
                                toVariableName(propertyName),
                                propertyType,
                                propertySchema,
                                "p$index"
                        )
                    }

                    val fieldDeclarations = javaProperties.map { javaProperty ->
                        val javaDoc = javaProperty.jsonSchema.title?.let { title ->
                            """|/**
                               | * $title
                               | */""".trimMargin()
                        } ?: ""
                        """|$javaDoc
                           |public final ${javaProperty.type} ${javaProperty.variableName};""".trimMargin()
                    }

                    val importDeclarations = TreeSet<String>()
                    val constructorArgs = javaProperties.joinToString(",\n") { "${it.type} ${it.variableName}" }
                    val constructorAssignments = javaProperties.map { javaProperty ->
                        "this.${javaProperty.variableName} = ${javaProperty.variableName};"
                    }

                    val jacksonParserWriter = JacksonParserWriter(converterRegistry)
                    val parserContent = jacksonParserWriter.write(jsonSchema, importDeclarations, valueType(converterRegistry))
                    val jacksonWriterWriter = JacksonWriterWriter(converterRegistry)
                    val writerContent = jacksonWriterWriter.write(jsonSchema, importDeclarations, valueType(converterRegistry))
                    val classJavaDoc = jsonSchema.title?.let { title ->
                        """|/**
                           | * $title
                           | */""".trimMargin()
                    } ?: ""

                    val content = """
                       |package ${getPackage(valueType(converterRegistry))};
                       |
                       |${importDeclarations.indentWithMargin(0)}
                       |
                       |$classJavaDoc
                       |public final class ${getSimpleName(valueType(converterRegistry))} {
                       |
                       |    ${fieldDeclarations.indentWithMargin(1)}
                       |
                       |    public ${getSimpleName(valueType(converterRegistry))}(
                       |            ${constructorArgs.indentWithMargin(3)}
                       |    ) {
                       |        ${constructorAssignments.indentWithMargin(2)}
                       |    }
                       |
                       |    ${parserContent.indentWithMargin(1)}
                       |
                       |    ${writerContent.indentWithMargin(1)}
                       |}
                       |
                    """.trimMargin()

                    val propertySchemas = jointProperties.map { it.value }
                    return ConverterWriter.Result(OutputFile(filePath, content), propertySchemas)
                }
            }
            else -> null
        }
    }
}
