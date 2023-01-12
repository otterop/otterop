plugins {
    antlr
}

dependencies {
     antlr("org.antlr:antlr4:4.11.1") // use ANTLR version 4
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-package", "otterop.transpiler.antlr")
    outputDirectory = File(buildDir.toString() + "/generated-src/antlr/main/otterop/transpiler/antlr")
}