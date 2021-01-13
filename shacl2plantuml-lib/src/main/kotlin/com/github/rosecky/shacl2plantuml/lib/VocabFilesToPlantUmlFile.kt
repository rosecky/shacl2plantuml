package com.github.rosecky.shacl2plantuml.lib

import com.github.rosecky.shacl2plantuml.lib.composer.PlantUmlComposer
import com.github.rosecky.shacl2plantuml.lib.definition.DiagramDefinitionComposer
import com.github.rosecky.shacl2plantuml.lib.model.DiagramModelComposer
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream

@Component
class VocabFilesToPlantUmlFile(
    private val diagramDefLoader: DiagramDefinitionComposer,
    private val diagramModelComposer: DiagramModelComposer,
    private val plantUmlComposer: PlantUmlComposer
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Loads a diagram definition from given file and an OWL ontology from given file or directory
     * and outputs a corresponding PlantUML class diagram file to given location
     */
    fun run(options: VocabFilesToPlantUmlFileOptions) {
        try {
            val vocabFile = options.getVocabLoc().let(::File)
            val shapesFile = options.getShapesLoc().let(::File)

            val vocabModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF)
            FileInputStream(vocabFile).use { stream ->
                vocabModel.read(stream, vocabFile.path, FileUtils.langTurtle)
            }
            val shapesModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF)
            FileInputStream(shapesFile).use { stream ->
                shapesModel.read(stream, shapesFile.path, FileUtils.langTurtle)
            }

            log.info("Loading diagram definition")
            val diagramDefinition = options.getDiagramDefLoc()?.let { diagramDefLoader.fromFile(it, vocabModel) }
            log.info("Composing diagram model")
            val model = diagramModelComposer.compose(shapesModel, vocabModel, diagramDefinition)
            log.info("Composing PlantUML")
            val plantUml = plantUmlComposer.compose(model)
            log.info("Writing to out file")

            when (val outputFormat = options.getOutputFormat()) {
                "plantuml" -> plantUml.dump(options.getOutputLoc())
                else -> plantUml.generate(outputFormat, options.getOutputLoc())
            }
        } catch (e: Exception) {
            log.error("Failed: ${e.message}", e)
        }
    }
}