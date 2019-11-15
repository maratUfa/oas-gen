package jsm.java

import jsm.FragmentRegistry
import jsm.OpenApiSchema
import jsm.Reference
import jsm.RootFragment
import jsm.java.reactor.netty.ReactorNettyServerWriter
import jsm.java.spring.mvc.JavaSrpingMvcServerWriter
import org.apache.commons.cli.*
import org.yaml.snakeyaml.Yaml
import java.io.File
import kotlin.system.exitProcess


const val SCHEMA = "schema"
const val OUTPUT_DIR = "output-dir"
const val PACKAGE = "package"
const val GENERATOR = "generator"

private val writerFactories = mapOf(
        "java-spring-mvc" to ::JavaSrpingMvcServerWriter,
        "reactor-netty-server" to ::ReactorNettyServerWriter
)

fun main(args: Array<String>) {
    val options = Options()
    options.addRequiredOption("s", SCHEMA, true, "schema file")
    options.addRequiredOption("o", OUTPUT_DIR, true, "output directory")
    options.addRequiredOption("p", PACKAGE, true, "package name")
    options.addRequiredOption("g", GENERATOR, true, "generator identifier")

    val parser = DefaultParser()
    val commandLine: CommandLine
    try {
        commandLine = parser.parse(options, args)
    } catch (exp: ParseException) {
        println(exp.message)
        val formatter = HelpFormatter()
        formatter.printHelp("java", options)
        exitProcess(1)
    }

    val schemaFileArg = commandLine.getOptionValue(SCHEMA)
    val outputDirArg = commandLine.getOptionValue(OUTPUT_DIR)
    val packageName = commandLine.getOptionValue(PACKAGE)
    val generatorId = commandLine.getOptionValue(GENERATOR)

    val yaml = Yaml()
    val schemaFile = File(schemaFileArg)
    val map = schemaFile.inputStream().use { yaml.load<Map<*, *>>(it) }
    val rootFragment = RootFragment(schemaFile.name, map)
    val fragmentRegistry = FragmentRegistry(listOf(rootFragment))
    val openApiSchema = OpenApiSchema(fragmentRegistry.get(Reference.root(schemaFile.name)), null)

    val writerFactory = writerFactories[generatorId] ?: error("Can't find generator $generatorId")
    val writer = writerFactory(packageName)

    val outputFiles = writer.write(listOf(openApiSchema))
    val outputDir = File(outputDirArg)
    outputDir.mkdirs()
    outputFiles.forEach { outputFile ->
        val generatedFile = File(outputDir, outputFile.path)
        generatedFile.parentFile.mkdirs()
        generatedFile.writeText(outputFile.content)
    }

}
