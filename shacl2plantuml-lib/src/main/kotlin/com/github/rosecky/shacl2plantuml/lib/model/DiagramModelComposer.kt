package com.github.rosecky.shacl2plantuml.lib.model

import com.github.rosecky.shacl2plantuml.lib.Util.Companion.withoutAdmDebugShape
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getLabel
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.isPureClassWithoutUseCaseSpecificConstraints
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.isShapeSpecific
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.listPropertyResourceValues
import com.github.rosecky.shacl2plantuml.lib.definition.*
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.vocabulary.SHACLM
import org.springframework.stereotype.Component

@Component
class DiagramModelComposer {

    /**
     * Composes a diagram model.
     * Discovers which classes are used and builds an underlying model spec
     */
    fun compose(
            shapes: OntModel,
            vocab: OntModel,
            diagramDefinition: DiagramDefinition? = null
    ): DiagramModel {
        val definition = defaultDefinition(diagramDefinition, vocab)
        val allShapeSpecificClassModels = getShapeSpecificClassModels(shapes, vocab, definition).associateBy(DiagramClassModel::getUri)
        val includedClasses = enumerateIncludedClasses(shapes, vocab, definition, allShapeSpecificClassModels).toSet().sortedBy(DiagramClassModel::getUri)
        val diagram = DiagramModel(vocab, includedClasses, allShapeSpecificClassModels)
        includedClasses.forEach { it.init(diagram) }
        return diagram
    }

    private fun defaultDefinition(definition: DiagramDefinition?, vocab: OntModel): DiagramDefinition {
        val def = definition ?: DiagramDefinition().apply { init(vocab.graph.prefixMapping) }
        if (def.classes.isEmpty()) def.expandOutLinks = true
        return def
    }

    private fun enumerateIncludedClasses(
            shapes: OntModel,
            vocab: OntModel,
            definition: DiagramDefinition,
            allShapeSpecificClassModels: Map<String, DiagramShapeSpecificClassModel>
    ): Collection<DiagramClassModel> {
        return if (definition.classes.isEmpty())
            allClassModels(shapes, vocab, definition, allShapeSpecificClassModels)
        else
            classModelsFromDefinition(definition, shapes, vocab, allShapeSpecificClassModels)
    }

    private fun allClassModels(
            shapes: OntModel,
            vocab: OntModel,
            definition: DiagramDefinition,
            allShapeSpecificClassModels: Map<String, DiagramShapeSpecificClassModel>
    ): Collection<DiagramClassModel> {

        ( shapes.listResourcesWithProperty(SHACLM.targetClass).toList() + shapes.listResourcesWithProperty(SHACLM.class_).toList() )
            .let { withoutAdmDebugShape(it) }
            .forEach { shape ->
                // this all actually only works correctly when a single sh:class is specified (multiple targetClasses allowed)
                val shapeClasses = shape.listPropertyResourceValues(SHACLM.targetClass) + shape.listPropertyResourceValues(SHACLM.class_)
                if (isPureClassWithoutUseCaseSpecificConstraints(shape)) {
                    val shapeClassesUris = shapeClasses.mapNotNull { it.uri }.filter { vocab.getOntClass(it) != null }
                    shapeClassesUris.forEach { definition.newListedClass(it) }
                }
            }

        return classModelsFromDefinition(definition, shapes, vocab, allShapeSpecificClassModels)
    }

    private fun classModelsFromDefinition(definition: DiagramDefinition, shapes: OntModel, vocab: OntModel, allShapeSpecificClassModels: Map<String, DiagramShapeSpecificClassModel>): Collection<DiagramClassModel> {
        var classModels = definition.classes
                .map {
                    createClassModel(it.uri, allShapeSpecificClassModels, shapes, vocab, it)
                }.toSet()
        val visitedClasses = classModels.map { it.getUri() }.toMutableSet()

        var newClasses = getNewRelatedClasses(visitedClasses, classModels, allShapeSpecificClassModels, shapes, vocab, definition)
        while (newClasses.isNotEmpty()) {
            classModels = classModels + newClasses
            visitedClasses.addAll(newClasses.map(DiagramClassModel::getUri))
            newClasses = getNewRelatedClasses(visitedClasses, newClasses, allShapeSpecificClassModels, shapes, vocab, definition)
        }

        return classModels
    }

    private fun createClassModel(
            uri: String,
            allShapeSpecificClassModels: Map<String, DiagramShapeSpecificClassModel>,
            shapes: OntModel,
            vocab: OntModel,
            definition: DiagramClassDefinition
    ): DiagramClassModel {
        return allShapeSpecificClassModels[uri] ?: DiagramClassModel(vocab.getOntClass(uri), shapes, definition, getRelevantShapes(uri, shapes))
    }

    private fun getShapeSpecificClassModels(shapes: OntModel, vocab: OntModel, definition: DiagramDefinition): Collection<DiagramShapeSpecificClassModel> {
        val classModels = mutableListOf<DiagramShapeSpecificClassModel>()
        shapes.listResourcesWithProperty(SHACLM.class_).toList()
            .forEach { shape ->
                // this all actually only works correctly when a single sh:class is specified ("has to be an instance of both" does not work)
                val shapeClasses = shape.listPropertyResourceValues(SHACLM.targetClass) + shape.listPropertyResourceValues(SHACLM.class_)
                if (!isPureClassWithoutUseCaseSpecificConstraints(shape)) {
                    shapeClasses.forEach { origClsRes ->
                        origClsRes.uri ?. let { origUri ->
                            val origCls = vocab.getOntClass(origUri) ?: throw Error("Class $origClsRes not found in ontology.")
                            val newUri = DiagramShapeSpecificClassModel.composeUri(shape, origUri)
                            val newClass = vocab.createClass(newUri)
                            newClass.addSuperClass(origCls)
                            vocab.createLiteral(DiagramShapeSpecificClassModel.composeLabel(shape, origCls.getLabel())).let { newClass.addLabel(it) }
                            val clsDefinition = if (definition.classes.any { def -> def.uri == origUri })
                                definition.newListedClass(newUri)
                            else
                                definition.newExpandedClass(newUri)
                            classModels.add(DiagramShapeSpecificClassModel(newClass, clsDefinition, shape))
                        }
                    }
                }
            }
        return classModels
    }

    private fun getRelevantShapes(uri: String, shapes: OntModel): Collection<Resource> {
        val clsResource = shapes.getResource(uri)
        return ( shapes.listResourcesWithProperty(SHACLM.targetClass, clsResource).toList()
                        + shapes.listResourcesWithProperty(SHACLM.class_, clsResource).toList() )
                .filter(::isPureClassWithoutUseCaseSpecificConstraints)
    }

    private fun getNewRelatedClasses(
            existingClassUris: Set<String>,
            newClasses: Collection<DiagramClassModel>,
            allShapeSpecificClassModels: Map<String, DiagramShapeSpecificClassModel>,
            shapes: OntModel,
            vocab: OntModel,
            definition: DiagramDefinition
    ): Collection<DiagramClassModel> {
        val linkedClasses = newClasses
                .flatMap { getRelatedLinkedUrisToExpand(it) }
                .filter { allShapeSpecificClassModels.containsKey(it) || vocab.getOntClass(it) != null }
                .map {
                        createClassModel(it, allShapeSpecificClassModels, shapes, vocab,
                                definition.classes.firstOrNull { def -> def.uri == it} ?: definition.newExpandedClass(it))
                }

        val subAndSuperClasses = newClasses
                .flatMap { getRelatedSubOrSuperUrisToExpand(it) }
                .filter { vocab.getOntClass(it) != null }
                .map { uri ->
                        DiagramClassModel(vocab.getOntClass(uri), shapes,
                                definition.classes.firstOrNull { def -> def.uri == uri} ?: definition.newExpandedClass(uri),
                                getRelevantShapes(uri, shapes))
                }

        return (linkedClasses+subAndSuperClasses)
                .toSet()
                .filterNot { relatedCls -> relatedCls.getUri().let(existingClassUris::contains) }
    }


    private fun getRelatedLinkedUrisToExpand(
            c: DiagramClassModel
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
            c: DiagramClassModel
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
