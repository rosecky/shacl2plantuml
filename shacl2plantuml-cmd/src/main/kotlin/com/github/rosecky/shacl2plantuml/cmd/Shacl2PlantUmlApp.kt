package com.github.rosecky.shacl2plantuml.cmd

import com.github.rosecky.shacl2plantuml.lib.VocabFilesToPlantUmlFile
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.io.IOException

@SpringBootApplication(scanBasePackages = ["com.github.rosecky.shacl2plantuml"])
class Shacl2PlantUmlApp(
    private val optParser: CmdOptionsParser,
    private val shacl2PlantUml: VocabFilesToPlantUmlFile
): ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun handleException(e: Exception) {
        log.error("Failed", e)
        optParser.help()
    }

    override fun run(args: ApplicationArguments?) {
        if (args == null) {
            handleException(IOException("Invalid application parameters!"))
        } else {
            log.info("Starting SHACL -> PlantUML transformer.")
            try {
                val options = optParser.parse(*args.sourceArgs)
                shacl2PlantUml.run(options)
            } catch (e: Exception) {
                handleException(e)
            }
            log.info("SHACL -> PlantUML transformer finished.")
        }
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Shacl2PlantUmlApp::class.java, *args)
}
