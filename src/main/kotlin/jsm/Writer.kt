package jsm

data class OutputFile(
        val path: String,
        val content: String
)

abstract class TypedFragment {
    abstract val fragment: Fragment
    abstract val parent: TypedFragment?
    override fun toString() = "${javaClass.simpleName}(${fragment.reference})"
}

interface Writer<T> {
    fun write(
            items: Iterable<T>
    ): List<OutputFile>
}
