package com.github.rosecky.shacl2plantuml.lib.renderer

import com.github.rosecky.shacl2plantuml.lib.PlantUml
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import net.sourceforge.plantuml.cucadiagram.dot.GraphvizUtils
import org.springframework.stereotype.Component
import java.io.File
import java.io.OutputStream
import java.net.URL
import javax.annotation.PostConstruct

@Component
class DiagramRenderer(
    private val config: RenderConfig
) {
    val supportedFormats: List<String>
        get() = FileFormat.values().map {it.toString().toLowerCase()}

    fun toStream(format: String, plantUml: com.github.rosecky.shacl2plantuml.lib.PlantUml, out: OutputStream) {
        if (!canRender) {
            throw Exception("Problems with GraphViz Dot engine: \n$dotError")
        }
        if (format !in supportedFormats) {
            throw Exception("Format $format not supported. \nSupported formats: $supportedFormats.")
        }
        val reader = SourceStringReader(plantUml.plantUmlString)

        if (config.server != null) {
            val encodedSUrl: String = reader.blocks[0].encodedUrl
            val url = "${config.server}/$format/$encodedSUrl"
            URL(url).openStream().use { svg ->
                svg.copyTo(out)
            }
        } else {
            val fileFormat = FileFormat.valueOf(format.toUpperCase())
            reader.outputImage(out, FileFormatOption(fileFormat))
        }
    }

    fun toFile(format: String, plantUml: com.github.rosecky.shacl2plantuml.lib.PlantUml, fileName: String) {
        val file = File(fileName).apply {
            parentFile?.mkdirs()
        }
        file.outputStream().use { out ->
            toStream(format, plantUml, out)
        }
    }

    private var canRender: Boolean = true
    private var dotError: String? = null

    @PostConstruct
    fun init() {
        testDot()
    }

    private fun testDot() {
        if (config.server == null) {
            val report = mutableListOf<String>()
            val errorCode = GraphvizUtils.addDotStatus(report,false)
            if (errorCode != 0) {
                canRender = false
                dotError = report.joinToString("\n" )
            }
        }
    }
}