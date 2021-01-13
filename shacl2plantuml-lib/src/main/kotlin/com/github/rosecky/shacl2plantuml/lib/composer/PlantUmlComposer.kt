package com.github.rosecky.shacl2plantuml.lib.composer

import com.github.rosecky.shacl2plantuml.lib.PlantUml
import com.github.rosecky.shacl2plantuml.lib.model.DiagramClassModel
import com.github.rosecky.shacl2plantuml.lib.model.DiagramModel
import com.github.rosecky.shacl2plantuml.lib.model.DiagramPropertyModel
import com.github.rosecky.shacl2plantuml.lib.model.DiagramShapeSpecificClassModel
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
        diagramModel: DiagramModel
    ): com.github.rosecky.shacl2plantuml.lib.PlantUml {
        val stringBuilder = StringBuilder("@startuml \n")//${diagramModel.definition.name} \n")
        stringBuilder.appendLine(diagramConfig.style.global)
        composeClassesContent(diagramModel).forEach { stringBuilder.appendLine(it) }
        composeLinks(diagramModel).forEach { stringBuilder.appendLine(it) }
        stringBuilder.appendLine("@enduml")
        return com.github.rosecky.shacl2plantuml.lib.PlantUml(renderer, stringBuilder.toString())
    }

    private fun composeClassesContent(diagramModel: DiagramModel): Iterable<String> {
        return diagramModel.classes.flatMap {
            composeClassContent(diagramModel, it)
        }
    }

    private fun composeClassContent(diagramModel: DiagramModel, c: DiagramClassModel): Iterable<String> {
        val classStyle = if (c is DiagramShapeSpecificClassModel) diagramStyle.shapeSpecificClass else diagramStyle.properClass
        val stereotypes = composeClassStereotypes(diagramModel, c)

        val ret = mutableListOf("class \"${c.getLabel()}\" as ${c.getUri().hashCode()} $stereotypes $classStyle {")
        ret += c.innerProperties.map {
            composeInnerProperty(diagramModel, c, it)
        }
        ret.add("}")

        return ret
    }

    private fun composeInnerProperty(
            diagramModel: DiagramModel,
            c: DiagramClassModel,
            p: DiagramPropertyModel
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

    private fun getIcon(p: DiagramPropertyModel): String {
        return if (p.isForbidden())
            "-{method}"
        else {
            val fieldOrMethod = if (p.isRequired()) "{method}" else "{field}"
            val visibility = if (p.isMultiValued()) "#" else "+"
            "$fieldOrMethod$visibility"
        }
    }

    private fun getCardinalityString(p: DiagramPropertyModel): String {
        return if (p.isForbidden()) "0"
        else if (p.isMultiValued()) {
            if (p.isRequired()) "1..*" else "*"
        } else {
            if (p.isRequired()) "1" else "0..1"
        } + if (p.hasQualifiedValueCardinalities())
            " (+)"
        else ""
    }

    private fun getPropertyLabel(p: DiagramPropertyModel): String {
        return p.getLabel()
//        return if (p.isOr())
//            "|or| ${p.getLabel()}"
//        else p.getLabel()
    }

    private fun composeClassStereotypes(diagramModel: DiagramModel, c: DiagramClassModel): String {
        return if (c.superClassStereotypes.any()) {
            c.superClassStereotypes.joinToString(", ", "<<", ">>" ) {
                diagramModel.getClassLabel(it)
            }
        } else ""
    }

    fun composeLinks(diagramModel: DiagramModel): Iterable<String> {
        val layout = GraphLayout(diagramModel, diagramConfig.layout).also { it.computeLayout() }
        return diagramModel.classes.flatMap {
            composeSuperClassLinks(diagramModel,it, layout)
        } + diagramModel.classes.flatMap {
            composePropertyLinks(diagramModel,it, layout)
        }
    }

    private fun composeSuperClassLinks(
        diagramModel: DiagramModel,
        c: DiagramClassModel,
        layout: GraphLayout
    ): Iterable<String> {
        return c.superClassLinks.map {
            composeSuperClassLink(diagramModel, c, it, layout, diagramStyle.properClassSuperClassLink)
        } + c.indirectSuperClassLinks.map {
            composeSuperClassLink(diagramModel, c, it, layout, diagramStyle.shapeSpecificClassSuperClassLink)
        }
    }

    private fun composeSuperClassLink(
        diagramModel: DiagramModel,
        c: DiagramClassModel,
        superClass: OntClass,
        layout: GraphLayout,
        relStyle: String
    ): String {
        val direction = layout.getDirection(c.getUri(), superClass.uri)
        return "\"${c.getUri().hashCode()}\" -$direction-|> \"${superClass.uri.hashCode()}\" $relStyle"
    }

    private fun composePropertyLinks(
        diagramModel: DiagramModel,
        c: DiagramClassModel,
        layout: GraphLayout
    ): Iterable<String> {
        return c.propertyLinks.flatMap { p ->
            p.getObjects().map { o ->
                composePropertyLink(diagramModel, c, p, o, layout)
            }
        }
    }

    private fun composePropertyLink(
            diagramModel: DiagramModel,
            c: DiagramClassModel,
            p: DiagramPropertyModel,
            range: String,
            layout: GraphLayout
    ): String {
        val rangeC = diagramModel.getClass(range)
        val relStyle = when {
            (p.isForbidden()) -> diagramStyle.forbiddenLink
            (rangeC is DiagramShapeSpecificClassModel) -> diagramStyle.shapeSpecificClassInLink
            (c is DiagramShapeSpecificClassModel) -> diagramStyle.shapeSpecificClassOutLink
            else -> diagramStyle.properClassOutLink
        }
        val direction = layout.getDirection(c.getUri(), range)
        val label = getPropertyLabel(p)
        return "\"${c.getUri().hashCode()}\" -$direction-> \"${getCardinalityString(p)}\" \"${range.hashCode()}\" $relStyle : $label"
    }
}