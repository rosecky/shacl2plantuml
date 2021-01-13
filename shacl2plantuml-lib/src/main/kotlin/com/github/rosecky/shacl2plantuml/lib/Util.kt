package com.github.rosecky.shacl2plantuml.lib

import com.github.rosecky.shacl2plantuml.lib.model.DiagramShapeSpecificClassModel
import org.apache.jena.enhanced.EnhNode
import org.apache.jena.rdf.model.*
import org.apache.jena.shacl.vocabulary.SHACLM
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.SKOS


class Util {
    companion object {
        fun Resource.listPropertyResourceValues(property: Property): Iterable<Resource> {
            return listProperties(property).toList().map { it.`object`.asResource() }
        }

        fun Model.listPropertyResourceValues(property: Property): Iterable<Resource> {
            return listObjectsOfProperty(property).toList().map { it.asResource() }
        }

        fun Resource.getListsWithResource(): Iterable<RDFList> {
            return model.listResourcesWithProperty(RDF.first, this).toList().mapNotNull { it.asRdfList()?.getListRoot() }
        }

        private fun RDFList.getListRoot(): RDFList {
            return model.listResourcesWithProperty(RDF.rest, this).toList().firstOrNull()?.asRdfList()?.getListRoot() ?: this
        }

        fun Resource.asRdfList(): RDFList? {
            return if (this is EnhNode) {
                if (this.canAs(RDFList::class.java)) {
                    this.`as`(RDFList::class.java)
                } else null
            } else null
        }

        fun Resource.asJavaList(): Iterable<Resource>? {
            return asRdfList()?.asJavaList()?.map(RDFNode::asResource)
        }

        fun Resource.getRelatedShapes(): Iterable<Resource> {
            val parentShapesAndMe = getRelatedParentShapes() + listOf(this)
            return (parentShapesAndMe.flatMap { it.getRelatedChildShapes() } + parentShapesAndMe).toSet()
        }

        fun Resource.getRelatedParentShapes(): Iterable<Resource> {
            val shapes = mutableSetOf<Resource> ()
            shapes += model.listResourcesWithProperty(SHACLM.qualifiedValueShape, this).toList()
            shapes += model.listResourcesWithProperty(SHACLM.node, this).toList()
            getListsWithResource().forEach { list ->
                val parents = model.listResourcesWithProperty(SHACLM.and, list).toList() +
                        model.listResourcesWithProperty(SHACLM.or, list).toList() +
                        model.listResourcesWithProperty(SHACLM.xone, list).toList()
                shapes += parents
            }
            val shapesSet = withoutAdmDebugShape(shapes)
            return (shapesSet.flatMap { it.getRelatedParentShapes() } + shapesSet).toSet()
        }

        fun Resource.getRelatedChildShapes(): Iterable<Resource> {
            val shapes = mutableSetOf<Resource> ()
            shapes += listPropertyResourceValues(SHACLM.qualifiedValueShape).toList()
            shapes += listPropertyResourceValues(SHACLM.node).toList()
            shapes += listPropertyResourceValues(SHACLM.and).toList().flatMap { list -> list.asJavaList() ?: listOf() }
            shapes += listPropertyResourceValues(SHACLM.or).toList().flatMap { list -> list.asJavaList() ?: listOf() }
            shapes += listPropertyResourceValues(SHACLM.xone).toList().flatMap { list -> list.asJavaList() ?: listOf() }
            val shapesSet = withoutAdmDebugShape(shapes).filterNot { it.isPropertyShape() }
            return (shapesSet.flatMap { it.getRelatedChildShapes() } + shapesSet).toSet()
        }

        fun Resource.isPropertyShape(): Boolean = hasProperty(RDF.type,SHACLM.PropertyShape) || hasProperty(SHACLM.path)

        fun withoutAdmDebugShape(shapes: Iterable<Resource>): Iterable<Resource> {
            return shapes.toSet().filterNot { it.uri == "http://purl.allotrope.org/shape/AFS_0000001" }
        }

        fun Resource.getLabel(): String {
            return getProperty(SKOS.prefLabel)?.`object`?.asLiteral()?.string
                ?: getProperty(RDFS.label)?.`object`?.asLiteral()?.string
                ?: localName
                ?: uri
                ?: "[]"
        }

        fun isPureClassWithoutUseCaseSpecificConstraints(shape: Resource): Boolean {
            return shape.hasProperty(SHACLM.targetClass)
                    || !( shape.getRelatedShapes().any {
                        it.hasProperty(SHACLM.sparql)
                                || it.hasProperty(SHACLM.property)
                                || it.hasProperty(SHACLM.pattern)
                    } )
        }

        fun Resource.getReferencedClassUri(origClassUri: String): String {
            return if (isPureClassWithoutUseCaseSpecificConstraints(this))
                origClassUri
            else
                DiagramShapeSpecificClassModel.composeUri(this, origClassUri)
        }

        fun Resource.hasObject(): Boolean =
                hasProperty(SHACLM.class_)
                        || hasProperty(SHACLM.targetClass)
                        || hasProperty(SHACLM.datatype)
                        || hasProperty(SHACLM.hasValue)

        fun Resource.getObjectsFromRelatedShapes(): Iterable<String> {
            val relatedShapesWithObject = getRelatedShapes().filter { it.hasObject() }
            return relatedShapesWithObject.flatMap { relatedShapeWithObject ->
                relatedShapeWithObject.listPropertyResourceValues(SHACLM.targetClass) . map { relatedShapeWithObject.getReferencedClassUri(it.uri) } +
                        relatedShapeWithObject.listPropertyResourceValues(SHACLM.class_) . map { relatedShapeWithObject.getReferencedClassUri(it.uri) } +
                        relatedShapeWithObject.listPropertyResourceValues(SHACLM.datatype) . map { it.uri } +
                        relatedShapeWithObject.listPropertyResourceValues(SHACLM.hasValue) . map { it.toString() }
            }
        }

        fun isShapeSpecific(uri: String): Boolean {
            return uri.contains("---")
        }
    }
}