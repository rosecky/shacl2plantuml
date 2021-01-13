package com.github.rosecky.shacl2plantuml.lib.model

import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getObjectsFromRelatedShapes
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getRelatedChildShapes
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getRelatedParentShapes
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.getRelatedShapes
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.isPropertyShape
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.isShapeSpecific
import com.github.rosecky.shacl2plantuml.lib.Util.Companion.listPropertyResourceValues
import com.github.rosecky.shacl2plantuml.lib.definition.DiagramClassDefinition
import org.apache.jena.ontology.OntClass
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.vocabulary.SHACLM
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS

/**
 * Model of a class included in the diagram
 */
open class DiagramClassModel(
        val classModel: OntClass,
        val shapesModel: OntModel,
        var definition: DiagramClassDefinition,
        shapes: Collection<Resource> = setOf()
) {
    val shapes: MutableCollection<Resource>

    init {
        if (classModel.uri == null) throw Error("Class uri cannot be null $classModel")
        this.shapes = shapes.toMutableSet()
    }

    fun addShape(shape: Resource) { shapes.add(shape) }

    protected lateinit var diagram: DiagramModel

    /**
     * @return uri for this class
     * Works even before init() is called
     */
    fun getUri(): String = classModel.uri

    /**
     * @return label for this class
     * Works even before init() is called
     */
    fun getLabel(): String = classModel.getLabel(null) ?: classModel.getPropertyValue(SKOS.prefLabel)?.asLiteral()?.string ?: classModel.localName ?: "[]"

    /**
     * @return collection of uris of all direct super classes
     * Works even before init() is called
     */
    fun getSuperClasses(): Iterable<OntClass> = classModel.listSuperClasses(true).toList()

    /**
     * @return collection of uris of all direct sub classes
     * Works even before init() is called
     */
    fun getSubClasses(): Iterable<OntClass> = classModel.listSubClasses(true).toList()

    /**
     * @return collection of uris of all direct super classes
     * Works even before init() is called
     */
    fun getTransitiveSuperClasses(): Iterable<OntClass> = classModel.listSuperClasses(false).toList()

    /**
     * @return collection of uris of all direct sub classes
     * Works even before init() is called
     */
    fun getTransitiveSubClasses(): Iterable<OntClass> = classModel.listSubClasses(false).toList()

    /**
     * @return collection of properties that are included (are to be displayed somehow)
     * Works even before init() is called
     */
    fun getIncludedProperties(): Iterable<DiagramPropertyModel> {
        val related = shapes.flatMap { it.getRelatedShapes() }
        val relatedChildren = shapes.flatMap { it.getRelatedChildShapes() }
        val relatedProperties =
            relatedChildren.filter { it.isPropertyShape() } +
            related.flatMap { it.listPropertyResourceValues(SHACLM.property) }
        return relatedProperties.map { DiagramPropertyModel(it, classModel.ontModel) }.toSet().sortedBy { it.hashCode() }
    }

    /**
     * @return collection of properties that will be displayed as links to other classes if those are included
     * Works even before init() is called
     */
    fun getIncludedOutLinks(): Iterable<DiagramPropertyModel> = getIncludedProperties()
            .filter { p -> p.getObjects().any() && p.shouldInclude(definition) }

    fun getIncludedInLinkUris(): Iterable<String> {
        val related = shapes.flatMap { it.getRelatedShapes() }
        return related
                .flatMap { shape ->
                    val p = DiagramPropertyModel(shape, classModel.ontModel)
                    shapesModel.listResourcesWithProperty(SHACLM.property, shape).toList()
                            .flatMap {
                                if (p.shouldInclude(definition)) {
                                    it.getObjectsFromRelatedShapes()
                                } else listOf()
                            }
                }.toSortedSet().filterNot(::isShapeSpecific)
    }

    /**
     * Initiates the class model - this is crucial for the model to know what other classes are included in the diagram
     */
    fun init(diagram: DiagramModel) {
        this.diagram = diagram
    }

    /**
     * @return collection of properties that will be displayed as inner properties / class attributes
     * !!Works ONLY AFTER init() is called!!
     */
    val innerProperties: Iterable<DiagramPropertyModel>
        get() = if (definition.hideInnerProperties == true) listOf() else getIncludedProperties()-propertyLinks

    /**
     * @return collection of properties that will be displayed as links to other classes (it's guaranteed that they are included)
     * !!Works ONLY AFTER init() is called!!
     */
    val propertyLinks: Iterable<DiagramPropertyModel>
        get() = if (definition.hideOutLinks == true) listOf() else getIncludedOutLinks().filter { p -> p.getObjects().all { o -> diagram.isClassIncluded(o) } }

    /**
     * @return collection of uris of all direct super classes to be displayed as class stereotypes
     * !!Works ONLY AFTER init() is called!!
     */
    open val superClassStereotypes: Iterable<OntClass>
        get() = if (definition.hideSuperClassStereotypes == true) listOf() else getSuperClasses().filterNot { it.uri == null }.sortedBy { it.uri } - superClassLinks

    /**
     * @return collection of uris of all direct super classes to be displayed as superclass links (it's guaranteed that they are included)
     * !!Works ONLY AFTER init() is called!!
     */
    val superClassLinks: Iterable<OntClass>
        get() = if (definition.hideSuperClassLinks == true) listOf()
        else getSuperClasses().filter { diagram.isClassIncluded(it.uri) }.sortedBy { it.uri }

    val indirectSuperClassLinks: Iterable<OntClass>
        get() = if (definition.hideSuperClassLinks == true || definition.hideIndirectSuperClassLinks == true) listOf()
        else {
            val all = getSuperClasses()
            val known = all.filter { diagram.isClassIncluded(it.uri) }
            (all - known).flatMap(::computeMissingTransitiveSuperClassLinks)
        }.toSet().sortedBy { it.uri }

    private fun computeMissingTransitiveSuperClassLinks(cls: OntClass): Iterable<OntClass> {
        return cls.listSuperClasses(true).toList().flatMap { sc ->
            if (diagram.isClassIncluded(sc.uri)) listOf(sc) else computeMissingTransitiveSuperClassLinks(sc)
        }
    }

    override fun equals(other: Any?): Boolean {
        return hashCode() == other.hashCode()
    }

    override fun hashCode(): Int {
        return classModel.hashCode()
    }
}
