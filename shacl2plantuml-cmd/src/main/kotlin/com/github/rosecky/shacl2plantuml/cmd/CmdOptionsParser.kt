package com.github.rosecky.shacl2plantuml.cmd

import com.github.rosecky.shacl2plantuml.lib.VocabFilesToPlantUmlFileOptions
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
class CmdOptionsParser(
    private val opts: Options
) {
    init {
        opts.apply {
            addRequiredOption("s", "shapes", true, "Location of the shapes file")
            addRequiredOption("v", "vocabulary", true, "Location of the vocabulary/ontology file")
            addOption("d", "diagramDef", true, "Location of the diagram definition file")
            addRequiredOption("o", "output", true, "The location of the output file. It will be created if it doesn't already exist.")
            addRequiredOption("f", "format", true, "The format of the output file (plantuml | svg).")
        }
    }

    fun parse(vararg args: String): VocabFilesToPlantUmlFileOptions {
        return DefaultParser().parse(opts, args).let(::CmdOptions)
    }

    fun help() {
        HelpFormatter().printHelp("java -jar", opts)
    }

    private inner class CmdOptions(
        private val cmd: CommandLine
    ): VocabFilesToPlantUmlFileOptions {
        override fun getShapesLoc(): String {
            return cmd.getOptionValue("s")
        }

        override fun getVocabLoc(): String {
            return cmd.getOptionValue("v")
        }

        override fun getDiagramDefLoc(): String? {
            return if (cmd.hasOption("d")) cmd.getOptionValue("d") else null
        }

        override fun getOutputLoc(): String {
            return cmd.getOptionValue("o")
        }

        override fun getOutputFormat(): String {
            return cmd.getOptionValue("f")
        }
    }

    @Configuration
    class Config {
        @Bean
        fun getOptions(): Options = Options()
    }
}
