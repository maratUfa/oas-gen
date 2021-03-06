package jsm.java.jackson

import jsm.JsonSchema

class ConverterRegistry(private val converterMatchers: List<ConverterMatcher>) {
    operator fun get(jsonSchema: JsonSchema): ConverterWriter {
        converterMatchers.forEach { converterMatcher ->
            val converterWriter = converterMatcher.match(jsonSchema)
            if (converterWriter != null) return converterWriter
        }
        error("Can't find converter for schema $jsonSchema")
    }
}