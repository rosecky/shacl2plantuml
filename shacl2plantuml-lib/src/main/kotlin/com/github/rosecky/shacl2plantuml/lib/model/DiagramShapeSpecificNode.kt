package com.github.rosecky.shacl2plantuml.lib.model

import com.github.rosecky.shacl2plantuml.lib.definition.DiagramClassDefinition
import org.apache.jena.ontology.OntClass
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.AnonId
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.OWL

class DiagramShapeSpecificNode(
    classModel: OntClass,
    definition: DiagramClassDefinition,
    val shape: Resource,
    val parent: DiagramNode? = null,
) : DiagramNode(classModel, shape.model as OntModel, definition, mutableListOf(shape)) {

    companion object {
        fun composeUri(shape: Resource, origClassUri: String? = null): String =
            if (shape.uri != null)
                "${origClassUri ?: ""}---uri:${shape.uri}"
            else
                "${origClassUri ?: ""}---id:${shape.id}"

        fun composeLabel(shape: Resource, origClassLabel: String?): String =
            when {
                origClassLabel == null -> " "
                shape.localName != null -> "${origClassLabel ?: ""} (${shape.localName})"
                else -> "${origClassLabel ?: ""} (...)"
            }

        fun getShapeFromComposedUri(composedUri: String, shapes: OntModel): Resource {
            val shapeUri = composedUri.split("---").last()
            return if (shapeUri.startsWith("uri:")) {
                shapes.createResource(shapeUri.removePrefix("uri:"))
            } else {
                shapes.createResource(AnonId(shapeUri.removePrefix("id:")))
            }
        }

        fun isComposedUri(composedUri: String): Boolean =
            composedUri.contains("---uri:") || composedUri.contains("---id:")

        fun isComposedNonClassUri(composedUri: String): Boolean =
            composedUri.startsWith("---uri:") || composedUri.startsWith("---id:")
    }

    fun getOrigClass(): OntClass? = getSuperClasses().firstOrNull()

    override val superClassStereotypes: Iterable<OntClass>
        get() = if (definition.hideSuperClassStereotypes == true || getOrigClass() in superClassLinks)
            listOf()
        else
            getOrigClass()?.listSuperClasses(true)?.toList()?.filterNot { it.uri == null }?.sortedBy { it.uri } ?: listOf()

    override fun equals(other: Any?): Boolean {
        return (other is DiagramShapeSpecificNode) && other.shape == shape
    }

    override fun hashCode(): Int {
        return shape.hashCode()
    }
}
