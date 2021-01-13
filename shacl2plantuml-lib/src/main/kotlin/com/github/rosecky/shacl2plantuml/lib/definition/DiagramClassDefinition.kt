package com.github.rosecky.shacl2plantuml.lib.definition

import org.apache.jena.graph.NodeFactory
import org.apache.jena.ontology.OntModel
import org.apache.jena.shared.PrefixMapping

abstract class DiagramClassDefinition: DiagramDefinitionTemplate() {

    lateinit var uri: String

    fun getLabel(): String {
        return if (displayClassUri!!) prefixMapping.shortForm(uri) else NodeFactory.createURI(uri).localName
    }

    override fun init(prefixMapping: PrefixMapping) {
        super.init(prefixMapping)
        uri = prefixMapping.expandPrefix(uri)
    }

    override fun validate(model: OntModel) {
        super.validate(model)
        if(!::uri.isInitialized) {
            throw Exception("Not initialized")
        }
        if (model.getOntClass(uri) == null) {
            throw UnresolvableUriException(uri, "Model does not contain class with URI $uri, which was provided in the diagram definition file")
        }
    }
}

class DiagramListedClassDefinition: DiagramClassDefinition()

class DiagramExpandedClassDefinition(
    uri: String,
    inheritFrom: DiagramDefinitionTemplate
): DiagramClassDefinition() {
    init {
        this.uri = uri
        init(inheritFrom)
    }
}