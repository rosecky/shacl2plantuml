package com.github.rosecky.shacl2plantuml.lib.composer

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Configuring graph layout computing algorithm.
 * This algorithm performs a force-directed layout and then computes an approximate direction of every link:
 * left, right, down or up. Note that if "left to right direction" is applied in DiagramStyle.global,
 * "up" actually means "left" etc.
 * The algorithm only treats classes as points, not boxes.
 */
@Component
@ConfigurationProperties("shacl2plantuml.diagram.layout")
class LayoutConfig {

    var applyLayout = false

    /**
     * Number of iterations of the force-directed layout algorithm
     */
    var iterations = 50000

    /**
     * Width of the box, within which the graph nodes are laid out.
     * Note that if "left to right direction" is applied in DiagramStyle.global,
     * this actually correlates with resulting height of the diagram
     */
    var boxWidth = 800.0

    /**
     * Height of the box, within which the graph nodes are laid out.
     * Note that if "left to right direction" is applied in DiagramStyle.global,
     * this actually correlates with resulting width of the diagram
     */
    var boxHeight = 800.0

    /**
     * PlantUML (actually GraphViz) layout behaves slightly oddly when rendering things.
     * In the default (top to bottom) direction, two nodes <a> and <b> to the right / left of <c>,
     * i.e. <c> -l-> <a> and <c> -l-> <b> are not displayed on the top of each other, as one would expect,
     * but in a horizontal line with <c>, meaning the link from <c> to <b> goes oddly around <a>.
     * To avoid this weird behaviour, we're prioritizing up and down links, meaning that unless a node is "really left"
     * or right to another (linked) one, we'll display it under or above it. This controls the level of this prioritization.
     *
     * When "left to right direction" applies, the "oddness" is now in up / down direction, however still applies to
     * left / right links (remember "up" actually means "left" etc.). So increasing this still does the same.
     *
     * In practice, complicated diagrams can get less tangled when increasing this, however they also get wider.
     */
    var upDownPreference = 3.0

    var seed: Long = 0
}