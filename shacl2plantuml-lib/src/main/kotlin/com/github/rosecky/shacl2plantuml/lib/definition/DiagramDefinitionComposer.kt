package com.github.rosecky.shacl2plantuml.lib.definition

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.jena.ontology.OntModel
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.io.File

@Component
@Configuration
class DiagramDefinitionComposer {

    private val mapper = YAMLFactory().let(::ObjectMapper).registerKotlinModule()

    /**
     * Loads a DiagramDefinition from given file and validates it using given model / spec adapter
     * @param diagramDefFile: Location of a diagram definition file
     * @param model: Model to use prefix mapping from and validate against
     */
    fun fromFile(diagramDefFile: String, model: OntModel): DiagramDefinition {
        return mapper.readValue(File(diagramDefFile), DiagramDefinition::class.java)
            .also { def -> initDefinition(def, model) }
    }

    private fun initDefinition(definition: DiagramDefinition, model: OntModel) {
        definition.init(model.graph.prefixMapping)
        definition.validate(model)
    }
}
