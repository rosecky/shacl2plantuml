package com.github.rosecky.shacl2plantuml.lib.composer

import com.github.rosecky.shacl2plantuml.lib.model.Diagram
import org.jgrapht.alg.connectivity.ConnectivityInspector
import org.jgrapht.alg.drawing.FRLayoutAlgorithm2D
import org.jgrapht.alg.drawing.model.Box2D
import org.jgrapht.alg.drawing.model.MapLayoutModel2D
import org.jgrapht.alg.drawing.model.Point2D
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultUndirectedGraph
import kotlin.math.abs
import java.util.Random
import kotlin.math.sqrt

/**
 * This algorithm performs a force-directed layout and then computes an approximate direction of every link:
 * left, right, down or up. Note that if "left to right direction" is applied in DiagramStyle.global,
 * "up" actually means "left" etc.
 * The algorithm only treats classes as points, not boxes.
 *
 * Usage:
 *
 * val layout = GraphLayout(model, config)
 * layout.computeLayout()
 * val link = "$fromUri -${layout.getDirection(fromUri, toUri)}-> toUri"
 */
class GraphLayout(
    private val model: Diagram,
    private val config: LayoutConfig
) {
    private val connectedComponentLayouts: Iterable<ConnectedComponentLayout>

    init {
        if (config.applyLayout) {
            val connectedComponents = findNonTrivialConnectedComponents()
            connectedComponentLayouts = connectedComponents.map {
                ConnectedComponentLayout(
                    it,
                    model,
                    config
                )
            }
        } else {
            connectedComponentLayouts = listOf()
        }
    }

    private fun findNonTrivialConnectedComponents(): Collection<Set<String>> {
        val graph = DefaultUndirectedGraph<String, StringEdge>(
            StringEdge::class.java)
        model.classes.forEach {
            graph.addVertex(it.getUri())
        }
        model.classes.forEach { c ->
            c.propertyLinks.forEach { p -> p.getObjects().forEach { o -> graph.addEdge(c.getUri(), o) } }
            c.superClassLinks.forEach { sc -> graph.addEdge(c.getUri(), sc.uri) }
        }
        return ConnectivityInspector(graph).connectedSets().filter { it.size > 1 }
    }

    fun computeLayout() {
        connectedComponentLayouts.forEach { it.computeLayout() }
    }

    fun getDirection(fromUri: String, toUri: String): String {
        return connectedComponentLayouts.firstOrNull { fromUri in it.classes }?.getDirection(fromUri, toUri) ?: ""
    }
}

private class ConnectedComponentLayout(
    val classes: Iterable<String>,
    private val model: Diagram,
    private val config: LayoutConfig
) {

    private val graph = DefaultUndirectedGraph<String, StringEdge>(
        StringEdge::class.java)
    private val layout = MapLayoutModel2D<String>(Box2D(config.boxWidth, config.boxHeight))

    init {
        classes.forEach {
            graph.addVertex(it)
        }
        classes.forEach {
            val c = model.getClass(it)
            c.propertyLinks.forEach { p -> p.getObjects().forEach { o -> addEdge(c.getUri(), o) } }
            c.superClassLinks.forEach { sc -> addEdge(c.getUri(), sc.uri) }
        }
    }

    private fun addEdge(fromUri: String, toUri: String) {
        if (fromUri != toUri)
            graph.addEdge(fromUri, toUri)
    }

    fun computeLayout() {
        FRLayoutAlgorithm2D<String, StringEdge>(config.iterations, FRLayoutAlgorithm2D.DEFAULT_NORMALIZATION_FACTOR, Random(config.seed)).layout(graph, layout)
    }

    fun getDirection(fromUri: String, toUri: String): String {
        val from = layout.get(fromUri)
        val to = layout.get(toUri)

        if (from == null || to == null) {
            return ""
        } else {
            val directionVector = from - to
            return if (abs(directionVector.x) > abs(directionVector.y)*config.upDownPreference) {
                if (directionVector.x > 0)
                    "l"
                else
                    "r"
            } else {
                if (directionVector.y > 0)
                    "u"
                else
                    "d"
            }
        }
    }
}

private fun <V> MapLayoutModel2D<V>.getLength(edge: Pair<V, V>): Double {
    val vector = get(edge.second) - get(edge.first)
    return vector.getLength()
}

private fun Point2D.getLength(): Double {
    return sqrt(x*x + y*y)
}

private operator fun Point2D.minus(other: Point2D): Point2D {
    return Point2D(this.x-other.x, this.y-other.y)
}

private class StringEdge: DefaultEdge() {
    override fun hashCode(): Int {
        return source.hashCode() * 17 + target.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return hashCode() == other.hashCode()
    }
}
