package com.github.rosecky.shacl2plantuml.lib.definition

import org.apache.jena.ontology.OntModel
import org.apache.jena.shared.PrefixMapping
import org.slf4j.LoggerFactory
import java.lang.Exception

/**
 * Definition of a diagram loaded from the diagram definition file.
 * This has no knowledge of the underlying model / spec
 */
class DiagramDefinition: DiagramDefinitionTemplate() {
    var name: String? = ""

    /**
     * Definitions of listed classes in the diagram
     */
    var classes: Collection<DiagramListedClassDefinition> = listOf()
        get() = field.toSet().sortedBy { it.uri }

    /**
     * Configuration applied to all listed classes in the diagram
     */
    var listedClasses: DiagramClassGroupDefinition = DiagramClassGroupDefinition()

    /**
     * Configuration applied to all classes added to the diagram through expansion
     */
    var expandedClasses: DiagramClassGroupDefinition = DiagramClassGroupDefinition()

    override fun init(prefixMapping: PrefixMapping) {
        super.init(prefixMapping)
        listedClasses.init(this, prefixMapping)
        expandedClasses.init(this, prefixMapping)
        classes.forEach {
            it.init(listedClasses, prefixMapping)
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)

    override fun validate(model: OntModel) {
        super.validate(model)
        try {
            classes.forEach { it.validate(model) }
        } catch (e: UnresolvableUriException) {
            log.error(e.message)
            classes = classes.filterNot { it.uri == e.uri }
        }
    }

    /**
     * Creates a new class definition for a class added to the diagram through expansion
     */
    fun newExpandedClass(uri: String): DiagramExpandedClassDefinition =
        DiagramExpandedClassDefinition(uri, expandedClasses)

    /**
     * Creates a new class definition for a class added to the diagram through expansion
     */
    fun newListedClass(uri: String): DiagramListedClassDefinition {
        return if (classes.any { it.uri == uri })
            classes.first { it.uri == uri }
        else {
            val cls = DiagramListedClassDefinition().apply {
                this.uri = uri
                init(listedClasses)
            }
            classes += listOf(cls)
            cls
        }
    }
}
