package com.github.rosecky.shacl2plantuml.lib.model

import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getLabel
import org.apache.jena.ontology.OntClass
import org.apache.jena.ontology.OntModel
import org.apache.jena.ontology.OntProperty

/**
 * Model of a diagram
 */
class DiagramModel(
        val model: OntModel,
        val classes: Collection<DiagramClassModel>,
        private val allShapeSpecificClassModels: Map<String, DiagramShapeSpecificClassModel>? = null
) {
    fun isClassIncluded(uri: String?): Boolean =
        uri != null && classes.any { it.getUri() == uri }

    fun getClass(uri: String): DiagramClassModel =
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