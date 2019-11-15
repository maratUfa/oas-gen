package jsm

import java.lang.StringBuilder

fun String.indentWithMargin(indent: String, marginPrefix: String = "|") =
        this.lines().indentWithMargin(indent, marginPrefix)

fun String.indentWithMargin(indentLevel: Int, marginPrefix: String = "|") =
        this.lines().indentWithMargin(indentLevel, marginPrefix)

fun Iterable<String>.indentWithMargin(indent: String, marginPrefix: String = "|"): String {
    return this
            .flatMap { it.lines() }
            .joinToString("\n$marginPrefix$indent")
            .removeSuffix("\n$marginPrefix$indent")
            .lines()
            .joinToString("\n") { it.trimEnd() }
}

fun Iterable<String>.indentWithMargin(indentLevel: Int, marginPrefix: String = "|"): String {
    val indent = (0 until indentLevel).fold(StringBuilder()) {sb, _ ->
        sb.append("    ")
        sb
    }.toString()
    return this.indentWithMargin(indent)
}

fun String.nullIfEmpty() = if (this.isEmpty()) null else this