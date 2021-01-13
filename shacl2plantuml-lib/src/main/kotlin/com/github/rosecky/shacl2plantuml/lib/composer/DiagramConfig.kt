package com.github.rosecky.shacl2plantuml.lib.composer

import org.springframework.stereotype.Component

/**
 * PlantUML configuration properties
 */
@Component
class DiagramConfig(
        val style: DiagramStyleConfig,
        val layout: LayoutConfig
)