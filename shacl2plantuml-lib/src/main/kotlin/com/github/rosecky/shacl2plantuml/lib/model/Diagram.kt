package com.github.rosecky.shacl2plantuml.lib.model

import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getLabel
import org.apache.jena.ontology.OntClass
import org.apache.jena.ontology.OntModel

/**
 * Model of a diagram
 */
class Diagram(
    val model: OntModel,
    val classes: Collection<DiagramNode>,
    private val allShapeSpecificClassModels: Map<String, DiagramShapeSpecificNode>? = null
) {
    fun isClassIncluded(uri: String?): Boolean =
        uri != null && classes.any { it.getUri() == uri }

    fun getClass(uri: String): DiagramNode =
        classes.first { it.getUri() == uri }

    fun getClassLabel(uri: String): String =
        when {
            allShapeSpecificClassModels?.containsKey(uri) == true -> allShapeSpecificClassModels[uri]!!.getLabel()
            isClassIncluded(uri) -> getClass(uri).getLabel()
            else -> model.getResource(uri)?.getLabel() ?: uri
        }

    fun getClassLabel(classModel: OntClass): String =
        classModel.getLabel()
}