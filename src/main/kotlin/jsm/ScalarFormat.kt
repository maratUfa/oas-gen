package jsm

enum class ScalarFormat(val jsonType: JsonType, val format: String?) {
    STRING(JsonType.STRING, null),
    LOCAL_DATE_TIME(JsonType.STRING, "local-date-time"),
    NUMBER(JsonType.NUMBER, null),
    BOOLEAN(JsonType.BOOLEAN, null), ;
}

fun getScalarFormat(jsonType: JsonType, format: String?): ScalarFormat {
    return ScalarFormat.values()
            .find { it.jsonType == jsonType && it.format == format }
            ?: error("can't find scalar format for type = $jsonType and format = $format")
}
