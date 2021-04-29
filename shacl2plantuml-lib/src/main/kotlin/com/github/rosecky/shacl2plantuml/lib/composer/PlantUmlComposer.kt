package com.github.rosecky.shacl2plantuml.lib.composer

import com.github.rosecky.shacl2plantuml.lib.model.DiagramNode
import com.github.rosecky.shacl2plantuml.lib.model.Diagram
import com.github.rosecky.shacl2plantuml.lib.model.DiagramProperty
import com.github.rosecky.shacl2plantuml.lib.model.DiagramShapeSpecificNode
import com.github.rosecky.shacl2plantuml.lib.renderer.DiagramRenderer
import org.apache.jena.ontology.OntClass
import org.springframework.stereotype.Component

@Component
class PlantUmlComposer(
        private val diagramConfig: DiagramConfig,
        private val renderer: DiagramRenderer,
        private val diagramStyle: DiagramStyleConfig
) {
    /**
     * Composes a PlantUML string from given DiagramModel
     */
    fun compose(
        diagramModel: Diagram
    ): com.github.rosecky.shacl2plantuml.lib.PlantUml {
        val stringBuilder = StringBuilder("@startuml \n")//${diagramModel.definition.name} \n")
        stringBuilder.appendLine(diagramConfig.style.global)
        composeClassesContent(diagramModel).forEach { stringBuilder.appendLine(it) }
        composeLinks(diagramModel).forEach { stringBuilder.appendLine(it) }
        stringBuilder.appendLine("@enduml")
        return com.github.rosecky.shacl2plantuml.lib.PlantUml(renderer, stringBuilder.toString())
    }

    private fun composeClassesContent(diagramModel: Diagram): Iterable<String> {
        return diagramModel.classes.flatMap {
            composeClassContent(diagramModel, it)
        }
    }

    private fun composeClassContent(diagramModel: Diagram, c: DiagramNode): Iterable<String> {
        val classStyle = if (c is DiagramShapeSpecificNode) diagramStyle.shapeSpecificClass else diagramStyle.properClass
        val stereotypes = composeClassStereotypes(diagramModel, c)

        val ret = mutableListOf("class \"${c.getLabel()}\" as ${c.getUri().hashCode()} $stereotypes $classStyle {")
        ret += c.innerProperties.map {
            composeInnerProperty(diagramModel, c, it)
        }
        ret.add("}")

        return ret
    }

    private fun composeInnerProperty(
        diagramModel: Diagram,
        c: DiagramNode,
        p: DiagramProperty
    ): String {
        val cardinality = getCardinalityString(p)
        val icon = getIcon(p)
        val range = p.getObjects().joinToString(", ") { diagramModel.getClassLabel(it) }
        return diagramStyle.innerPropertyTemplate
            .replaceFirst("{icon}", icon)
            .replaceFirst("{property}", getPropertyLabel(p))
            .replaceFirst("{range}", range)
            .replaceFirst("{cardinality}", cardinality)
    }

    private fun getIcon(p: DiagramProperty): String {
        return if (p.isForbidden())
            "-{method}"
        else {
            val fieldOrMethod = if (p.isRequired()) "{method}" else "{field}"
            val visibility = if (p.isMultiValued()) "#" else "+"
            "$fieldOrMethod$visibility"
        }
    }

    private fun getCardinalityString(p: DiagramProperty): String {
        return if (p.isForbidden()) "0"
        else if (p.isMultiValued()) {
            if (p.isRequired()) "1..*" else "*"
        } else {
            if (p.isRequired()) "1" else "0..1"
        } + if (p.hasQualifiedValueCardinalities())
            " (+)"
        else ""
    }

    private fun getPropertyLabel(p: DiagramProperty): String {
        return p.getLabel()
//        return if (p.isOr())
//            "|or| ${p.getLabel()}"
//        else p.getLabel()
    }

    private fun composeClassStereotypes(diagramModel: Diagram, c: DiagramNode): String {
        return if (c.superClassStereotypes.any()) {
            c.superClassStereotypes.joinToString(", ", "<<", ">>" ) {
                diagramModel.getClassLabel(it)
            }
        } else ""
    }

    fun composeLinks(diagramModel: Diagram): Iterable<String> {
        val layout = GraphLayout(diagramModel, diagramConfig.layout).also { it.computeLayout() }
        return diagramModel.classes.flatMap {
            composeSuperClassLinks(diagramModel,it, layout)
        } + diagramModel.classes.flatMap {
            composePropertyLinks(diagramModel,it, layout)
        }
    }

    private fun composeSuperClassLinks(
        diagramModel: Diagram,
        c: DiagramNode,
        layout: GraphLayout
    ): Iterable<String> {
        return c.superClassLinks.map {
            composeSuperClassLink(diagramModel, c, it, layout, diagramStyle.properClassSuperClassLink)
        } + c.indirectSuperClassLinks.map {
            composeSuperClassLink(diagramModel, c, it, layout, diagramStyle.shapeSpecificClassSuperClassLink)
        }
    }

    private fun composeSuperClassLink(
        diagramModel: Diagram,
        c: DiagramNode,
        superClass: OntClass,
        layout: GraphLayout,
        relStyle: String
    ): String {
        val direction = layout.getDirection(c.getUri(), superClass.uri)
        return "\"${c.getUri().hashCode()}\" -$direction-|> \"${superClass.uri.hashCode()}\" $relStyle"
    }

    private fun composePropertyLinks(
        diagramModel: Diagram,
        c: DiagramNode,
        layout: GraphLayout
    ): Iterable<String> {
        return c.propertyLinks.flatMap { p ->
            p.getObjects().map { o ->
                composePropertyLink(diagramModel, c, p, o, layout)
            }
        }
    }

    private fun composePropertyLink(
        diagramModel: Diagram,
        c: DiagramNode,
        p: DiagramProperty,
        range: String,
        layout: GraphLayout
    ): String {
        val rangeC = diagramModel.getClass(range)
        val relStyle = when {
            (p.isForbidden()) -> diagramStyle.forbiddenLink
            (rangeC is DiagramShapeSpecificNode) -> diagramStyle.shapeSpecificClassInLink
            (c is DiagramShapeSpecificNode) -> diagramStyle.shapeSpecificClassOutLink
            else -> diagramStyle.properClassOutLink
        }
        val direction = layout.getDirection(c.getUri(), range)
        val label = getPropertyLabel(p)
        return "\"${c.getUri().hashCode()}\" -$direction-> \"${getCardinalityString(p)}\" \"${range.hashCode()}\" $relStyle : $label"
    }
}