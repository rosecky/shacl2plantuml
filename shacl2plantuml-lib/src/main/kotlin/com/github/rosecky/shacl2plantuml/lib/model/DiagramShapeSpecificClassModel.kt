package com.github.rosecky.shacl2plantuml.lib.model

import com.github.rosecky.shacl2plantuml.lib.definition.DiagramClassDefinition
import org.apache.jena.ontology.OntClass
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Resource

class DiagramShapeSpecificClassModel(
        classModel: OntClass,
        definition: DiagramClassDefinition,
        val shape: Resource,
        val parent: DiagramClassModel? = null,
) : DiagramClassModel(classModel, shape.model as OntModel, definition, mutableListOf(shape)) {

    companion object {
        fun composeUri(shape: Resource, origClassUri: String): String =
            "$origClassUri---${shape.uri ?: shape.id}"

        fun composeLabel(shape: Resource, origClassLabel: String): String =
            if (shape.localName != null)
                "$origClassLabel (${shape.localName})"
            else
                "$origClassLabel (...)"
    }

    fun getOrigClass(): OntClass = getSuperClasses().first()

    override val superClassStereotypes: Iterable<OntClass>
        get() = if (definition.hideSuperClassStereotypes == true || getOrigClass() in superClassLinks)
            listOf()
        else
            getOrigClass().listSuperClasses(true).toList().filterNot { it.uri == null }.sortedBy { it.uri }

    override fun equals(other: Any?): Boolean {
        return (other is DiagramShapeSpecificClassModel) && other.shape == shape
    }

    override fun hashCode(): Int {
        return shape.hashCode()
    }
}
