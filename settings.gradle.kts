rootProject.name = "json-schema-mapper"

include(":java:jackson-parser")
include(":java:reactor-handler")

include(":test-cases:reactor-netty-simple-server")
include(":test-cases:reactor-netty-simple-client")

include(":test-cases:spring-mvc-server")
include(":test-cases:spring-mvc-client")
