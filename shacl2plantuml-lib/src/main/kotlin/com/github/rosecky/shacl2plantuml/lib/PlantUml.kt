package com.github.rosecky.shacl2plantuml.lib

import com.github.rosecky.shacl2plantuml.lib.renderer.DiagramRenderer
import java.io.File
import java.io.OutputStream

class PlantUml(
    private val renderer: DiagramRenderer,

    /**
     * PlantUML string
     */
    val plantUmlString: String
) {
    /**
     * Dumps PlantUML to the given (.wsd) file location
     */
    fun dump(outFile: String) {
        File(outFile)
            .also {it.parentFile?.mkdirs()}
            .writeText(plantUmlString)
    }

    /**
     * Generates the diagram svg and dumps it to the given file location
     */
    fun generateSvg(outFile: String) = generate("svg", outFile)
    fun generateSvg(out: OutputStream) = generate("svg", out)

    fun generate(format: String, outFile: String) {
        renderer.toFile(format,this, outFile)
    }

    fun generate(format: String, out: OutputStream) {
        renderer.toStream(format,this, out)
    }
}
