package jsm

import jsm.java.reactor.netty.ReactorNettyServerWriter
import jsm.java.spring.mvc.JavaSrpingMvcServerWriter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.yaml.snakeyaml.Yaml
import java.io.File

internal class OpenApiTestCases {

    class TestCase(
            val writer: Writer<OpenApiSchema>,
            val schemaDir: File,
            val outputDir: File
    )

    @ParameterizedTest
    @MethodSource("inputSource")
    fun testCases(testCase: TestCase) {
        val yaml = Yaml()
        val schemaFiles = testCase.schemaDir.listFiles() ?: error("Schema files not found in ${testCase.schemaDir}")
        val rootFragments = schemaFiles.map { schemaFile ->
            val map = schemaFile.inputStream().use { yaml.load<Map<*, *>>(it) }
            RootFragment(schemaFile.name, map)
        }

        val fragmentRegistry = FragmentRegistry(rootFragments)
        val actualSchemas = rootFragments.map { OpenApiSchema(fragmentRegistry.get(Reference.root(it.path)), null) }
        val actualOutputFiles = testCase.writer.write(actualSchemas)

        val outputDirPath = testCase.outputDir.toPath()
        val expectedOutputFiles = testCase.outputDir.walk().filter { it.isFile }.map {
            val relativePath = outputDirPath.relativize(it.toPath())
            OutputFile(relativePath.toString(), it.readText())
        }.toList()

        TestUtils.assertOutputFilesEquals(
                "Failed test case '${testCase.schemaDir}'",
                expectedOutputFiles,
                actualOutputFiles
        )
    }

    companion object {
        @JvmStatic
        private fun inputSource() = listOf(
                TestCase(
                        ReactorNettyServerWriter("com.example"),
                        File("test-cases/reactor-netty-simple-server/src/schema"),
                        File("test-cases/reactor-netty-simple-server/src/expected/java")
                )
                ,
                TestCase(
                        JavaSrpingMvcServerWriter("com.example"),
                        File("test-cases/spring-mvc-server/src/schema"),
                        File("test-cases/spring-mvc-server/src/expected/java")
                )
        )

    }
}
