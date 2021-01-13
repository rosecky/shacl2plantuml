package com.github.rosecky.shacl2plantuml.lib.definition

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.jena.ontology.OntModel
import org.apache.jena.shared.PrefixMapping
import java.lang.Exception
import kotlin.reflect.KMutableProperty1

/**
 * Defines all common configuration properties
 */
abstract class DiagramDefinitionTemplate {

    protected var inheritFrom: DiagramDefinitionTemplate? = null

    private fun inheritBool(getter: KMutableProperty1<DiagramDefinitionTemplate, Boolean?>): Boolean {
        return inheritFrom?.let { getter.get(it) } ?: false
    }

    private fun inheritStringCollection(getter: KMutableProperty1<DiagramDefinitionTemplate, Collection<String>?>): Collection<String>? {
        return inheritFrom?.let { getter.get(it) }
    }

    /**
     * Range class of each link of the affected class is added to the diagram
     */
    var expandOutLinks: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::expandOutLinks)

    /**
     * Domain class of each link of the affected class is added to the diagram
     */
    var expandInLinks: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::expandInLinks)

    /**
     * Direct superclasses of the affected class are added to the diagram
     * expandTransitiveSuperClasses: true always overrides expandSuperClasses: false, even if specified on higher level
     */
    var expandSuperClasses: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::expandSuperClasses)

    /**
     * Direct and transitive superclasses of the affected class are added to the diagram
     * expandTransitiveSuperClasses: true always overrides expandSuperClasses: false, even if specified on higher level
     */
    var expandTransitiveSuperClasses: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::expandTransitiveSuperClasses)

    /**
     * Direct subclasses of the affected class are added to the diagram
     * expandTransitiveSubClasses: true always overrides expandSubClasses: false, even if specified on higher level
     */
    var expandSubClasses: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::expandSubClasses)

    /**
     * Direct and transitive subclasses of the affected class are added to the diagram
     * expandTransitiveSubClasses: true always overrides expandSubClasses: false, even if specified on higher level
     */
    var expandTransitiveSubClasses: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::expandTransitiveSubClasses)

    /**
     * No properties (attributes) are displayed in the class box
     */
    var hideInnerProperties: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::hideInnerProperties)

    /**
     * No properties are displayed as outbound links from the class, just as inner properties (attributes)
     */
    var hideOutLinks: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::hideOutLinks)

    /**
     * No properties are displayed as outbound links from the class, just as stereotypes
     */
    var hideSuperClassLinks: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::hideSuperClassLinks)

    /**
     * No properties are displayed as outbound links from the class, just as stereotypes
     */
    var hideIndirectSuperClassLinks: Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::hideIndirectSuperClassLinks)

    /**
     * No superclasses are displayed as stereotypes in the class box
     */
    var hideSuperClassStereotypes: Boolean? = null
        get() = field?: inheritBool(DiagramDefinitionTemplate::hideSuperClassStereotypes)

    /**
     * Short form URIs of classes are shown instead of names
     */
    var displayClassUri : Boolean? = null
        get() = field?: inheritBool(DiagramDefinitionTemplate::displayClassUri)

    /**
     * Short form URIs of properties are shown instead of names
     */
    var displayPropertyUri : Boolean? = null
        get() = field ?: inheritBool(DiagramDefinitionTemplate::displayPropertyUri)

    /**
     * List of property URIs that should only be present in the diagram - all other are entirely omitted
     * Either use onlyProperties or excludeProperties, not both (none is fine)
     */
    var onlyProperties : Collection<String>? = null
        get() = field ?: inheritStringCollection(DiagramDefinitionTemplate::onlyProperties)

    /**
     * List of property URIs that should only be entirely omitted from the diagram
     * Either use onlyProperties or excludeProperties, not both (none is fine)
     */
    var excludeProperties : Collection<String>? = null
        get() = field ?: inheritStringCollection(DiagramDefinitionTemplate::excludeProperties)

    /**
     * List of property URIs that should only be displayed as links, all other will be only presented as attributes
     * Either use onlyOutLinks or excludeOutLinks, not both (none is fine)
     */
    var onlyOutLinks : Collection<String>? = null
        get() = field ?: inheritStringCollection(DiagramDefinitionTemplate::onlyOutLinks)

    /**
     * List of property URIs that should not be displayed as links, just as attributes
     * Either use onlyOutLinks or excludeOutLinks, not both (none is fine)
     */
    var excludeOutLinks : Collection<String>? = null
        get() = field ?: inheritStringCollection(DiagramDefinitionTemplate::excludeOutLinks)

    /**
     * Prefix mapping - public so that it can be inherited to child definitions
     */
    @JsonIgnore
    lateinit var prefixMapping: PrefixMapping

    /**
     * Initializes this definition using a parent DiagramDefinitionTemplate to inherit configuration from
     */
    open fun init(inheritFrom: DiagramDefinitionTemplate, prefixMapping: PrefixMapping = inheritFrom.prefixMapping) {
        this.inheritFrom = inheritFrom
        init(prefixMapping)
    }

    /**
     * Initializes this definition.
     * By default, it expands all URIs to their long form
     */
    open fun init(prefixMapping: PrefixMapping) {
        this.prefixMapping = prefixMapping
        onlyProperties = onlyProperties?.map { prefixMapping.expandPrefix(it) }
        excludeProperties = excludeProperties?.map { prefixMapping.expandPrefix(it) }
        onlyOutLinks = onlyOutLinks?.map { prefixMapping.expandPrefix(it) }
        excludeOutLinks = excludeOutLinks?.map { prefixMapping.expandPrefix(it) }
    }

    /**
     * Checks that only sensible configuration is provided and references correspond to given model / spec
     * @throws Exception on error
     */
    open fun validate(model: OntModel) {
        if (!::prefixMapping.isInitialized) {
            throw Exception("prefixMapping is not set")
        }
        if (onlyProperties != null && excludeProperties != null) {
            throw Exception("Specify either onlyProperties or excludeProperties, not both")
        }
        if (onlyOutLinks != null && excludeOutLinks != null) {
            throw Exception("Specify either onlyOutLinks or excludeOutLinks, not both")
        }
    }
}
