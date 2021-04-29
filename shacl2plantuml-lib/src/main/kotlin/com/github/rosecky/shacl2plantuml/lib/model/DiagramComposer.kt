package com.github.rosecky.shacl2plantuml.lib.model

import com.github.rosecky.shacl2plantuml.lib.Util.Companion.withoutAdmDebugShape
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getLabel
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.isPureClassWithoutUseCaseSpecificConstraints
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.isShapeSpecific
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.listPropertyResourceValues
import com.github.rosecky.shacl2plantuml.lib.definition.*
import org.apache.jena.ontology.OntClass
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.vocabulary.SHACLM
import org.springframework.stereotype.Component

@Component
class DiagramComposer {

    /**
     * Composes a diagram model.
     * Discovers which classes are used and builds an underlying model spec
     */
    fun compose(
            shapes: OntModel,
            vocab: OntModel,
            diagramDefinition: DiagramDefinition? = null
    ): Diagram {
        val definition = defaultDefinition(diagramDefinition, vocab)
        val allShapeSpecificClassModels = getShapeSpecificClassModels(shapes, vocab, definition).associateBy(DiagramNode::getUri)
        val includedNodes = enumerateIncludedNodes(shapes, vocab, definition, allShapeSpecificClassModels).toSet().sortedBy(DiagramNode::getUri)
        val diagram = Diagram(vocab, includedNodes, allShapeSpecificClassModels)
        includedNodes.forEach { it.init(diagram) }
        return diagram
    }

    private fun defaultDefinition(definition: DiagramDefinition?, vocab: OntModel): DiagramDefinition {
        val def = definition ?: DiagramDefinition().apply { init(vocab.graph.prefixMapping) }
        if (def.classes.isEmpty()) def.expandOutLinks = true
        return def
    }

    private fun enumerateIncludedNodes(
            shapes: OntModel,
            vocab: OntModel,
            definition: DiagramDefinition,
            allShapeSpecificClassModels: Map<String, DiagramShapeSpecificNode>
    ): Collection<DiagramNode> {
        shapes.listPropertyResourceValues(SHACLM.targetClass).toList()
            .let { withoutAdmDebugShape(it) }
            .mapNotNull { it.uri }
            .filter { vocab.getOntClass(it) != null }
            .forEach { definition.newListedClass(it) }

        definition.expandOutLinks = true
        return classModelsFromDefinition(definition, shapes, vocab, allShapeSpecificClassModels)
    }
//
//    private fun allClassModels(
//            shapes: OntModel,
//            vocab: OntModel,
//            definition: DiagramDefinition,
//            allShapeSpecificClassModels: Map<String, DiagramShapeSpecificNode>
//    ): Collection<DiagramNode> {
//
//        ( shapes.listResourcesWithProperty(SHACLM.targetClass).toList() + shapes.listResourcesWithProperty(SHACLM.class_).toList() )
//            .let { withoutAdmDebugShape(it) }
//            .forEach { shape ->
//                // this all actually only works correctly when a single sh:class is specified (multiple targetClasses allowed)
//                val shapeClasses = shape.listPropertyResourceValues(SHACLM.targetClass) + shape.listPropertyResourceValues(SHACLM.class_)
//                if (isPureClassWithoutUseCaseSpecificConstraints(shape)) {
//                    val shapeClassesUris = shapeClasses.mapNotNull { it.uri }.filter { vocab.getOntClass(it) != null }
//                    shapeClassesUris.forEach { definition.newListedClass(it) }
//                }
//            }
//
//        return classModelsFromDefinition(definition, shapes, vocab, allShapeSpecificClassModels)
//    }

    private fun classModelsFromDefinition(definition: DiagramDefinition, shapes: OntModel, vocab: OntModel, allShapeSpecificClassModels: Map<String, DiagramShapeSpecificNode>): Collection<DiagramNode> {
        var classModels = definition.classes
                .map {
                    createClassModel(it.uri, allShapeSpecificClassModels, shapes, vocab, it)
                }.toSet()
        val visitedClasses = classModels.map { it.getUri() }.toMutableSet()

        var newClasses = getNewRelatedClasses(visitedClasses, classModels, allShapeSpecificClassModels, shapes, vocab, definition)
        while (newClasses.isNotEmpty()) {
            classModels = classModels + newClasses
            visitedClasses.addAll(newClasses.map(DiagramNode::getUri))
            newClasses = getNewRelatedClasses(visitedClasses, newClasses, allShapeSpecificClassModels, shapes, vocab, definition)
        }

        return classModels
    }

    private fun createClassModel(
        uri: String,
        allShapeSpecificClassModels: Map<String, DiagramShapeSpecificNode>,
        shapes: OntModel,
        vocab: OntModel,
        definition: DiagramClassDefinition
    ): DiagramNode {
        return allShapeSpecificClassModels[uri] ?: DiagramNode(vocab.getOntClass(uri), shapes, definition, getRelevantShapes(uri, shapes))
    }

    private fun getShapeSpecificClassModels(shapes: OntModel, vocab: OntModel, definition: DiagramDefinition): Collection<DiagramShapeSpecificNode> {
        val classModels = mutableListOf<DiagramShapeSpecificNode>()
        shapes.listResourcesWithProperty(SHACLM.class_).toList()
            .forEach { shape ->
                // this all actually only works correctly when a single sh:class is specified ("has to be an instance of both" does not work)
                val shapeClasses = shape.listPropertyResourceValues(SHACLM.targetClass) + shape.listPropertyResourceValues(SHACLM.class_)
                if (!isPureClassWithoutUseCaseSpecificConstraints(shape)) {
                    shapeClasses.forEach { origClsRes ->
                        origClsRes.uri ?. let { origUri ->
                            val origCls = vocab.getOntClass(origUri) ?: throw Error("Class $origClsRes not found in ontology.")
                            classModels.add(createShapeSpecificNode(vocab, definition, shape, origCls))
                        }
                    }
                }
            }
        return classModels
    }

    private fun createShapeSpecificNode(vocab: OntModel, definition: DiagramDefinition, shape: Resource, origCls: OntClass? = null): DiagramShapeSpecificNode {
        val origUri = origCls?.uri
        val newUri = DiagramShapeSpecificNode.composeUri(shape, origUri)
        val newClass = vocab.createClass(newUri)
        if (origCls != null) {
            newClass.addSuperClass(origCls)
        }
        vocab.createLiteral(DiagramShapeSpecificNode.composeLabel(shape, origCls?.getLabel())).let { newClass.addLabel(it) }
        val clsDefinition = if (definition.classes.any { def -> def.uri == origUri })
            definition.newListedClass(newUri)
        else
            definition.newExpandedClass(newUri)
        return DiagramShapeSpecificNode(newClass, clsDefinition, shape)
    }

    private fun getRelevantShapes(uri: String, shapes: OntModel): Collection<Resource> {
        val clsResource = shapes.getResource(uri)
        return ( shapes.listResourcesWithProperty(SHACLM.targetClass, clsResource).toList()
                        + shapes.listResourcesWithProperty(SHACLM.class_, clsResource).toList() )
                .filter(::isPureClassWithoutUseCaseSpecificConstraints)
    }

    private fun getNewRelatedClasses(
        existingClassUris: Set<String>,
        newClasses: Collection<DiagramNode>,
        allShapeSpecificClassModels: Map<String, DiagramShapeSpecificNode>,
        shapes: OntModel,
        vocab: OntModel,
        definition: DiagramDefinition
    ): Collection<DiagramNode> {
        val linkedClasses = newClasses
                .flatMap { getRelatedLinkedUrisToExpand(it) }
                .toSet()
                .mapNotNull {
                    if (DiagramShapeSpecificNode.isComposedNonClassUri(it)) {
                        createShapeSpecificNode(vocab, definition, DiagramShapeSpecificNode.getShapeFromComposedUri(it, shapes))
                    } else if (allShapeSpecificClassModels.containsKey(it) || vocab.getOntClass(it) != null) {
                        createClassModel(it, allShapeSpecificClassModels, shapes, vocab,
                            definition.classes.firstOrNull { def -> def.uri == it } ?: definition.newExpandedClass(it))
                    } else null
                }

        val subAndSuperClasses = newClasses
                .flatMap { getRelatedSubOrSuperUrisToExpand(it) }
                .filter { vocab.getOntClass(it) != null }
                .map { uri ->
                        DiagramNode(vocab.getOntClass(uri), shapes,
                                definition.classes.firstOrNull { def -> def.uri == uri} ?: definition.newExpandedClass(uri),
                                getRelevantShapes(uri, shapes))
                }

        return (linkedClasses+subAndSuperClasses)
                .toSet()
                .filterNot { relatedCls -> relatedCls.getUri().let(existingClassUris::contains) }
    }


    private fun getRelatedLinkedUrisToExpand(
            c: DiagramNode
    ): Iterable<String> {
        val related = mutableListOf<String>()
        if (c.definition.expandOutLinks == true) {
            related += c.getIncludedOutLinks()
                    .flatMap { it.getObjects() }
        }
        if (c.definition.expandInLinks == true) {
            related += c.getIncludedInLinkUris()
        }
        return related
    }

    private fun getRelatedSubOrSuperUrisToExpand(
            c: DiagramNode
    ): Iterable<String> {
        val related = mutableListOf<String>()
        if (c.definition.expandTransitiveSuperClasses == true) {
            related += c.getTransitiveSuperClasses().mapNotNull { it.uri }
        } else if (c.definition.expandSuperClasses == true) {
            related += c.getSuperClasses().mapNotNull { it.uri }
        }
        if (c.definition.expandTransitiveSubClasses == true) {
            related += c.getTransitiveSubClasses().mapNotNull { it.uri }
        } else if (c.definition.expandSubClasses == true) {
            related += c.getSubClasses().mapNotNull { it.uri }
        }
        return related.filterNot(::isShapeSpecific)
    }
}
