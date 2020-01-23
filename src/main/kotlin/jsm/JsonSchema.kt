package jsm

const val LOCAL_DATE_TIME_FORMAT = "local-date-time"

enum class JsonType(val scalar: Boolean) {
    STRING(true), NUMBER(true), BOOLEAN(true), ARRAY(false), OBJECT(false),
}

class JsonSchema(override val fragment: Fragment, override val parent: TypedFragment?) : TypedFragment() {
    val type = JsonType.valueOf(fragment["type"].asString().toUpperCase())
    val format = fragment.getOptional("format")?.asString()

    fun properties() = fragment.getOptional("properties")?.map { propertyName, propertyFragment ->
        propertyName to JsonSchema(propertyFragment, this)
    }?.toMap() ?: emptyMap()

    fun items() = fragment.getOptional("items")?.let { JsonSchema(it, this) }
}
