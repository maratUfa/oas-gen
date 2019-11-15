package jsm

enum class JsonType(val scalar: Boolean) {
    STRING(true), BOOLEAN(true), ARRAY(false), OBJECT(false),
}

class JsonSchema(override val fragment: Fragment, override val parent: TypedFragment?) : TypedFragment() {
    val type = JsonType.valueOf(fragment["type"].asString().toUpperCase())

    fun properties() = fragment.getOptional("properties")?.map { propertyName, propertyFragment ->
        propertyName to JsonSchema(propertyFragment, this)
    }?.toMap() ?: emptyMap()

    fun items() = fragment.getOptional("items")?.let { JsonSchema(it, this) }
}
