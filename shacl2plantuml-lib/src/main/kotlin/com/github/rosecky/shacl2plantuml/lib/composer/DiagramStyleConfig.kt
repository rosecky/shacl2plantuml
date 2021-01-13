package com.github.rosecky.shacl2plantuml.lib.composer

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Configuring PlantUML diagram styles
 */
@Component
@ConfigurationProperties("shacl2plantuml.diagram.style")
class DiagramStyleConfig {

    /**
     * Global (initial) PlantUML diagram control config
     * E.g. skinparams, hiding aspects of the model, GraphViz direction etc.
     * Using "left to right direction" works very well.
     */
    var global = """
                    left to right direction
                    hide circle
                    hide empty members
                    skinparam backgroundColor white
                    skinparam ClassStereotypeFontColor blue
                    skinparam nodesep 40
                    skinparam ranksep 40
                    skinparam ClassFontSize 13
                    skinparam Shadowing false""".trimIndent()

    /**
     * Style (e.g. color) applied to proper classes
     */
    var properClass = "#line:black"

    /**
     * Style (e.g. color) applied to outbound property links of proper classes
     */
    var properClassOutLink = "#black"

    /**
     * Style (e.g. color) applied to superclass links of proper classes
     */
    var properClassSuperClassLink = "#blue"

    /**
     * Style (e.g. color) applied to shape-specific class occurrences
     */
    var shapeSpecificClass = "#EEE;line:green"

    /**
     * Style (e.g. color) applied to outbound property links of shape-specific class occurrences
     */
    var shapeSpecificClassOutLink = "#text:999;line:999"

    /**
     * Style (e.g. color) applied to inbound property links of shape-specific class occurrences
     * Note that this is the a crucial relation for the shape-specific class occurrence: defining the context of its existence
     */
    var shapeSpecificClassInLink = "#line:green;text:green;line.bold"

    /**
     * Style (e.g. color) applied to superclass links of shape-specific class occurrences
     */
    var shapeSpecificClassSuperClassLink = "#line:99F"

    /**
     * Template for displaying inner properties. Can include various color, styling expressions etc.
     * Recognized keywords {icon} {property} {range} and {cardinality} (including curly brackets)
     * will be replaced by the corresponding thing
     */
    var innerPropertyTemplate = "{icon} <color:royalBlue>{property}</color>  {range}  <color:green>[{cardinality}]</color>"

    var forbiddenLink = "#line:red;text:red;line.bold"
}