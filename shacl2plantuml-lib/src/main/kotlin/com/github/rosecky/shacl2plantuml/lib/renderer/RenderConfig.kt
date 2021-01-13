package com.github.rosecky.shacl2plantuml.lib.renderer

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("shacl2plantuml.plantuml")
class RenderConfig {
    var server: String? = null // eg. "http://www.plantuml.com/plantuml"
}