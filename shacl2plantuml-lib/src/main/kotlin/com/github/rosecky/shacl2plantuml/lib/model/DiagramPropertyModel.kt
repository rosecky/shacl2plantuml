package com.github.rosecky.shacl2plantuml.lib.model

import com.github.rosecky.shacl2plantuml.lib.Util.Companion.asJavaList
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getLabel
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getObjectsFromRelatedShapes
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getRelatedShapes
import com.github.rosecky.shacl2plantuml.lib.definition.DiagramClassDefinition
import org.apache.jena.enhanced.EnhNode
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.RDFList
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.vocabulary.SHACLM
import java.util.*

class DiagramPropertyModel(
        val shape: Resource,
        val vocab: OntModel
) {
    /**
     * @return label for the given property
     * Works even before init() is called
     */
    fun getLabel(): String = labelPath(shape.getPropertyResourceValue(SHACLM.path))

    private fun labelPath(path: Resource): String {
        return when {
            path.hasProperty(SHACLM.inversePath) -> {
                "^ ${labelPath(path.getPropertyResourceValue(SHACLM.inversePath))}"
            }
            path.hasProperty(SHACLM.alternativePath) -> {
                val alternatives = (path.getPropertyResourceValue(SHACLM.alternativePath) as EnhNode).`as`(RDFList::class.java).asJavaList()
                alternatives.joinToString(" | ") { labelPath(it.asResource()) }
            }
            else -> {
                vocab.getResource(path.uri)?.getLabel() ?: path.localName
            }
        }
    }

    fun getMinCount(): Int? {
        return shape.getRelatedShapes().mapNotNull {
            if (hasMinCount(it))
                it.getProperty(SHACLM.minCount)?.`object`?.asLiteral()?.int
                    ?: it.getProperty(SHACLM.qualifiedMinCount)?.`object`?.asLiteral()?.int
            else null
        }.maxOrNull()
    }

    private fun hasMinCount(shape: Resource): Boolean =
        shape.hasProperty(SHACLM.minCount)
                || shape.hasProperty(SHACLM.qualifiedMinCount)

    fun isRequired(): Boolean {
        return getMinCount() ?. let { it > 0 } ?: false
    }

    fun getMaxCount(): Int? {
        return shape.getRelatedShapes().mapNotNull {
            if (hasMaxCount(it))
                it.getProperty(SHACLM.maxCount)?.`object`?.asLiteral()?.int
                    ?: it.getProperty(SHACLM.qualifiedMaxCount)?.`object`?.asLiteral()?.int
            else null
        }.minOrNull()
    }

    private fun hasMaxCount(shape: Resource): Boolean =
        shape.hasProperty(SHACLM.maxCount)
                || shape.hasProperty(SHACLM.qualifiedMaxCount)

    fun isMultiValued(): Boolean {
        return getMaxCount() ?. let { it > 1 } ?: true
    }

    fun isForbidden(): Boolean {
        return getMaxCount() == 0
    }

    fun isInverse(): Boolean {
        return getPath().hasProperty(SHACLM.inversePath)
    }

    fun hasQualifiedValueCardinalities(): Boolean {
        return shape.getRelatedShapes().any { it.hasProperty(SHACLM.qualifiedValueShape) }
    }

//    fun isOr(): Boolean {
//        return shape.getRelatedShapes().any { it.hasProperty(SHACLM.or) || it.hasProperty(SHACLM.xone) }
//    }

    fun getObjects(): SortedSet<String> = shape.getObjectsFromRelatedShapes().toSortedSet()

    override fun equals(other: Any?): Boolean {
        return if (other is DiagramPropertyModel) shape == other.shape else false
    }

    override fun hashCode(): Int {
        return shape.hashCode()
    }

    private fun getPath(): Resource {
        return shape.getPropertyResourceValue(SHACLM.path)
    }

    private fun getPathProperties(path: Resource): Iterable<Resource> {
        return when {
            path.hasProperty(SHACLM.inversePath) -> listOf(path.getPropertyResourceValue(SHACLM.inversePath)).flatMap(::getPathProperties)
            path.hasProperty(SHACLM.alternativePath) -> path.getPropertyResourceValue(SHACLM.alternativePath).asJavaList()?.flatMap(::getPathProperties) ?: listOf(path)
            else -> listOf(path)
        }
    }

    fun shouldInclude(definition: DiagramClassDefinition): Boolean {
        val pathPropertyUris = getPathProperties(getPath()).mapNotNull { it.uri }
        return definition.onlyOutLinks ?. any { it in pathPropertyUris }
                ?: definition.excludeOutLinks ?. none { it in pathPropertyUris }
                ?: true
    }
}