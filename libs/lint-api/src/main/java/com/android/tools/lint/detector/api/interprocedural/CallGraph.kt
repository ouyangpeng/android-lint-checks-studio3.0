/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.lint.detector.api.interprocedural

import com.android.tools.lint.detector.api.interprocedural.CallGraph.Edge
import com.android.tools.lint.detector.api.interprocedural.CallGraph.Node
import com.android.tools.lint.detector.api.interprocedural.CallTarget.DefaultCtor
import com.android.tools.lint.detector.api.interprocedural.CallTarget.Lambda
import com.android.tools.lint.detector.api.interprocedural.CallTarget.Method
import com.google.common.collect.HashMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Multimap
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.java.stubs.impl.PsiParameterStubImpl
import com.intellij.psi.impl.source.PsiParameterImpl
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.java.JavaUParameter
import org.jetbrains.uast.toUElement
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import kotlin.collections.set

sealed class CallTarget(open val element: UElement) {
    data class Method(override val element: UMethod) : CallTarget(element)
    data class Lambda(override val element: ULambdaExpression) : CallTarget(element)
    data class DefaultCtor(override val element: UClass) : CallTarget(element)
}

/**
 * A graph in which nodes represent methods (including lambdas)
 * and edges indicate that one method calls another.
 */
interface CallGraph {
    val nodes: Collection<Node>

    interface Node {
        val target: CallTarget
        val edges: Collection<Edge>
        val likelyEdges: Collection<Edge> get() = edges.filter { it.isLikely }
    }

    /**
     * An edge to [node] of type [kind] due to [call].
     * [call] can be null for, e.g., implicit calls to super constructors or method references.
     * [node] can be null for variable function invocations in Kotlin.
     */
    data class Edge(val node: Node?, val call: UCallExpression?, val kind: Kind) {
        val isLikely: Boolean get() = kind.isLikely

        enum class Kind {
            DIRECT, // A statically dispatched call.
            UNIQUE, // A call that appears to have a single implementation.
            TYPE_EVIDENCED, // A call evidenced by runtime type estimates.
            BASE, // The base method of a call (if not already direct/unique/evidenced).
            NON_UNIQUE_OVERRIDE, // A call that is one of several overriding implementations.
            INVOKE; // A function expression invocation in Kotlin.

            val isLikely: Boolean
                get() = when (this) {
                    DIRECT, UNIQUE, TYPE_EVIDENCED -> true
                    BASE, NON_UNIQUE_OVERRIDE, INVOKE -> false
                }
        }
    }

    fun getNode(element: UElement): Node

    /** Describes all contents of the call graph (for debugging). */
    @Suppress("UNUSED")
    fun dump(filter: (Edge) -> Boolean = { true }) = buildString {
        for (node in nodes.sortedBy { it.shortName }) {
            val callees = node.edges.filter(filter)
            appendln(node.shortName)
            callees.forEach { appendln("    ${it.node.shortName} [${it.kind}]") }
        }
    }

    @Suppress("UNUSED")
    fun outputToDotFile(file: String, filter: (Edge) -> Boolean = { true }) {
        PrintWriter(BufferedWriter(FileWriter(file))).use { writer ->
            writer.println("digraph {")
            for (node in nodes) {
                for ((callee) in node.likelyEdges) {
                    writer.print("  \"${node.shortName}\" -> \"${callee.shortName}\"")
                }
            }
            writer.println("}")
        }
    }
}

/** Describes this node in a form like class#method, using "anon" for anonymous classes/lambdas. */
val Node?.shortName: String
    get() {
        if (this == null)
            return "[unresolved invoke]"
        val containingClass = target.element.getContainingUClass()
        val containingClassStr =
                if (containingClass?.name == "Companion")
                    containingClass.containingClass?.name ?: "anon"
                else containingClass?.name ?: "anon"
        val containingMethod = target.element.getContainingUMethod()?.name ?: "anon"
        val target = target // Enables smart casts.
        return when (target) {
            is Method -> "$containingClassStr#${target.element.name}"
            is Lambda -> "$containingClassStr#$containingMethod#lambda"
            is DefaultCtor -> "${target.element.name}#${target.element.name}"
        }
    }

class MutableCallGraph : CallGraph {
    private val nodeMap = LinkedHashMap<PsiElement, MutableNode>()
    override val nodes get() = nodeMap.values

    class MutableNode(
            override val target: CallTarget,
            override val edges: MutableCollection<Edge> = ArrayList()) : Node {
        override fun toString() = shortName
    }

    override fun getNode(element: UElement): MutableNode {
        // TODO(kotlin-uast-cleanup)
        // We hash Psi rather than Uast because not all Uast nodes implement hashCode/equals
        // correctly. For example, KotlinULambdaExpression uses pointer equality rather than
        // comparing the underlying psi. Pointer equality is insufficient because Uast nodes
        // can be re-parsed. This should be fixed once we have access to Kotlin plugin source code.
        val psi = element.psi
        psi ?: throw Error("Expected psi for element $element")
        return nodeMap.getOrPut(psi) {
            val caller = when (element) {
                is UMethod -> Method(element)
                is ULambdaExpression -> Lambda(element)
                is UClass -> DefaultCtor(element)
                else -> throw Error("Unexpected UElement type ${element.javaClass}")
            }
            MutableNode(caller)
        }
    }

    override fun toString(): String {
        val numEdges = nodes.asSequence().map { it.edges.size }.sum()
        return "Call graph: ${nodeMap.size} nodes, $numEdges edges"
    }
}

/**
 * Returns non-intersecting paths from nodes in [sources]
 * to nodes for which [isSink] returns true.
 */
fun <T : Any> searchForPaths(
        sources: Collection<T>,
        isSink: (T) -> Boolean,
        getNeighbors: (T) -> Collection<T>): Collection<List<T>> {
    val res = ArrayList<List<T>>()
    val prev = HashMap<T, T?>(sources.associate { Pair(it, null) })
    fun T.seen() = this in prev
    val used = HashSet<T>() // Nodes already part of a result path.
    val q = ArrayDeque<T>(sources.toSet())
    while (!q.isEmpty()) {
        val n = q.removeFirst()
        if (isSink(n)) {
            // Keep running time linear by preempting path construction
            // if it intersects with one already seen.
            val path = generateSequence(n) { if (it in used) null else prev[it] }
                    .toList()
                    .reversed()
            if (path.none { it in used })
                res.add(path)
            used.addAll(path)
        } else {
            getNeighbors(n).filter { !it.seen() }.forEach {
                q.addLast(it)
                prev[it] = n
            }
        }
    }
    return res
}

/** Describes a parameter specialization tuple, mapping each parameter to one concrete receiver. */
data class ParamContext(
        val params: List<Pair<UParameter, DispatchReceiver>>,
        val implicitThis: DispatchReceiver?
) {
    operator fun get(param: UParameter) = params.firstOrNull { it.first == param }?.second

    companion object {
        val EMPTY = ParamContext(emptyList(), /*implicitReceiver*/ null)
    }

    // TODO(kotlin-uast-cleanup)
    // We hash Psi below rather than Uast because some Kotlin Uast nodes do not implement
    // equals/hashCode correctly. We should be able to remove the methods below once we can fix
    // this issue in the Kotlin source code.

    private fun List<Pair<UParameter, DispatchReceiver>>.mapToPsi() = map { (param, receiver) ->
        Pair(param.psi.navigationElement, receiver.element.psi?.navigationElement)
    }

    private fun DispatchReceiver?.mapToPsi() = this?.element?.psi?.navigationElement

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val o = other as? ParamContext ?: return false
        return implicitThis.mapToPsi() == o.implicitThis.mapToPsi() &&
                params.mapToPsi() == o.params.mapToPsi()
    }

    override fun hashCode() =
            31 * (implicitThis.mapToPsi()?.hashCode() ?: 0) + params.mapToPsi().hashCode()

}

/** A specialization of a call graph node on a parameter context. */
data class ContextualNode(val node: Node, val paramContext: ParamContext)

/**
 * An edge to [contextualNode] due to [cause] (usually a call expression).
 * By convention, an edge without cause (e.g., the beginning endpoint of a call path) will
 * have its [cause] field set to the UElement of its own node.
 */
data class ContextualEdge(
        val contextualNode: ContextualNode,
        val cause: UElement)

/** Augments the non-contextual receiver evaluator with a parameter context. */
class ContextualDispatchReceiverEvaluator(
        val paramContext: ParamContext,
        nonContextualEval: IntraproceduralDispatchReceiverEvaluator
) : DispatchReceiverEvaluator(nonContextualEval) {

    override fun getOwn(
            element: UElement,
            root: DispatchReceiverEvaluator): Collection<DispatchReceiver> = when (element) {
        is UThisExpression -> getForImplicitThis() // TODO: Qualified `this` not yet in UAST.
        is UParameter -> listOfNotNull(paramContext[element])
        is USimpleNameReferenceExpression -> {
            element.resolve()?.navigationElement?.toUElement()?.let { root[it] } ?: emptyList()
        }
        else -> {
            // TODO(kotlin-uast-cleanup)
            // The Kotlin plugin appears to use two completely separate sets of Psi elements: one
            // for the compiler frontend, and another for integrating with Lint and other code
            // that expects Java-like Psi behavior. The parameter context uses the latter, but
            // sometimes we have to use the former to help with name resolution. Thus here we use
            // the [navigationElement] to get the original Kotlin Psi while doing equality checks,
            // in case [element] is a parameter in disguise.
            paramContext.params
                    .find { (param, _) -> param.psi.navigationElement.toUElement() == element }
                    ?.second
                    ?.let { listOf(it) } ?: emptyList()
        }
    }

    override fun getOwnForImplicitThis(): Collection<DispatchReceiver> =
            listOfNotNull(paramContext.implicitThis)
}

/**
 * Builds parameter contexts for the target of [call] by taking the Cartesian product of the call
 * argument receivers. Returns an empty parameter context if there are no call argument receivers.
 * See "The Cartesian Product Algorithm" by Ole Agesen.
 */
fun buildParamContextsFromCall(
        callee: CallTarget,
        call: UCallExpression,
        implicitThisDispatchReceivers: List<DispatchReceiver>,
        receiverEval: ContextualDispatchReceiverEvaluator): Collection<ParamContext> {

    // The potential for an implicit receiver argument to the callee complicates the logic here.
    // When creating the Cartesian product we include the implicit receiver argument as a "normal"
    // call argument, then pull it out again when creating parameter contexts.

    val implicitThisParamPsiStub = PsiParameterStubImpl(
            null, "phony", TypeInfo.createConstructorType(), false, false)
    val implicitThisParamPsi = PsiParameterImpl(implicitThisParamPsiStub)
    val implicitThisParam = JavaUParameter(implicitThisParamPsi, null)
    val explicitParams = when (callee) {
        is Method -> callee.element.uastParameters
        is Lambda -> callee.element.valueParameters
        is DefaultCtor -> emptyList()
    }
    val params = listOf(implicitThisParam) + explicitParams

    val explicitArgReceivers = call.valueArguments.map { receiverEval[it].toList() }
    val argReceivers = listOf(implicitThisDispatchReceivers) + explicitArgReceivers

    // Note that the size of params may not match the size of args when there is
    // a variadic parameter; in that case the variadic parameter is ignored.

    // We will take a Cartesian product, so filter out empty receiver sets.
    val (paramsWithReceivers, nonEmptyArgReceivers) = params.zip(argReceivers)
            .filter { it.second.isNotEmpty() }
            .unzip()

    if (nonEmptyArgReceivers.isEmpty())
        return listOf(ParamContext.EMPTY) // Optimization.

    // Zip formal parameters with all possible argument receiver combinations.
    val cartesianProd = Lists.cartesianProduct(nonEmptyArgReceivers)
    val numImplicitArgs = if (implicitThisDispatchReceivers.isNotEmpty()) 1 else 0
    val maxNumParamContexts = 1000
    val paramContexts = cartesianProd
            .take(maxNumParamContexts) // Cap combinatorial explosions.
            .map { receiverTuple ->
                val zippedArgs = paramsWithReceivers.zip(receiverTuple)
                val implicitThis = receiverTuple.take(numImplicitArgs).firstOrNull()
                ParamContext(zippedArgs.drop(numImplicitArgs), implicitThis)
            }
    assert(paramContexts.isNotEmpty())
    return paramContexts
}

/** Examines call sites to find contextualized neighbors of a search node. */
fun ContextualNode.computeEdges(
        callGraph: CallGraph,
        nonContextualReceiverEval: IntraproceduralDispatchReceiverEvaluator
): Collection<ContextualEdge> {

    val contextualReceiverEval =
            ContextualDispatchReceiverEvaluator(paramContext, nonContextualReceiverEval)

    // TODO: Kotlin lambda receivers not yet reflected in UAST.
    fun DispatchReceiver.deriveImplicitThisDispatchReceiver() = when (this) {
        is DispatchReceiver.Class -> this
        is DispatchReceiver.Functional.Reference -> this.receiver
        is DispatchReceiver.Functional.Lambda -> null
    }

    return node.edges.flatMap { edge ->

        val cause = edge.call ?: node.target.element
        when {
            edge.isLikely && edge.node != null -> {
                // Resolved edges are created directly.
                val implicitReceiverDispatchReceivers = edge.call
                        ?.getDispatchReceivers(contextualReceiverEval)
                        ?.mapNotNull { it.deriveImplicitThisDispatchReceiver() }
                        ?: emptyList()
                val paramContexts =
                        if (edge.call == null) listOf(ParamContext.EMPTY)
                        else buildParamContextsFromCall(
                                edge.node.target, edge.call,
                                implicitReceiverDispatchReceivers,
                                contextualReceiverEval)
                paramContexts.map { calleeContext ->
                    val node = ContextualNode(edge.node, calleeContext)
                    ContextualEdge(node, cause)
                }
            }
            (edge.kind == Edge.Kind.BASE || edge.kind == Edge.Kind.INVOKE) && edge.call != null -> {
                // Try to refine the base method to concrete targets.
                edge.call.getDispatchReceivers(contextualReceiverEval).flatMap { dispatchReceiver ->
                    val target = edge.call.getTarget(dispatchReceiver)
                    if (target == null)
                        emptyList()
                    else {
                        val implicitThisDispatchReceiver =
                                dispatchReceiver.deriveImplicitThisDispatchReceiver()
                        val paramContexts = buildParamContextsFromCall(
                                target, edge.call,
                                listOfNotNull(implicitThisDispatchReceiver),
                                contextualReceiverEval)
                        paramContexts.map { calleeContext ->
                            val node = ContextualNode(
                                    callGraph.getNode(target.element),
                                    calleeContext)
                            ContextualEdge(node, cause)
                        }
                    }
                }
            }
            else -> emptyList()
        }
    }
}

interface ContextualCallGraph {
    val contextualNodes: Collection<ContextualNode>

    fun outEdges(n: ContextualNode): Collection<ContextualEdge>

    fun inEdges(n: ContextualNode): Collection<ContextualEdge>
}

class MutableContextualCallGraph : ContextualCallGraph {
    override val contextualNodes = ArrayList<ContextualNode>()
    val outEdgeMap: Multimap<ContextualNode, ContextualEdge> = HashMultimap.create()
    val inEdgeMap: Multimap<ContextualNode, ContextualEdge> = HashMultimap.create()

    override fun outEdges(n: ContextualNode): Collection<ContextualEdge> = outEdgeMap[n]

    override fun inEdges(n: ContextualNode): Collection<ContextualEdge> = inEdgeMap[n]
}

/**
 * To find initial parameter contexts, we employ a nice trick: we do a BFS from *all* nodes in the
 * call graph (specialized on empty parameter contexts) and take note of all parameter contexts
 * discovered for each node. These parameter contexts are used to form search nodes that can be used
 * to better initialize a subsequent path search.
 *
 * This is useful, e.g., for thread annotation checking. When a method is annotated with @UiThread
 * and takes a lambda as an argument, we want to make sure that we take note of all the lambdas
 * passed to this method, as one of the lambdas may lead to a @WorkerThread method. If we instead
 * initialized the path search with empty parameter contexts, then we would never see evidenced
 * thread violations through the lambda parameter.
 */
fun CallGraph.buildContextualCallGraph(
        nonContextualReceiverEval: IntraproceduralDispatchReceiverEvaluator): ContextualCallGraph {
    val contextualGraph = MutableContextualCallGraph()
    val allSources = nodes.map { ContextualNode(it, ParamContext.EMPTY) }
    searchForPaths(
            sources = allSources,
            isSink = { contextualGraph.contextualNodes.add(it); false },
            getNeighbors = { n ->
                val edges = n.computeEdges(this, nonContextualReceiverEval)
                contextualGraph.outEdgeMap.putAll(n, edges)
                edges.forEach { (nbr, cause) ->
                    contextualGraph.inEdgeMap.put(nbr, ContextualEdge(n, cause))
                }
                edges.map { it.contextualNode }
            })
    return contextualGraph
}

/**
 * A context-sensitive search for paths from
 * contextualized [contextualSources] to [contextualSinks].
 */
fun ContextualCallGraph.searchForContextualPaths(
        contextualSources: Collection<ContextualNode>,
        contextualSinks: Collection<ContextualNode>
): Collection<List<ContextualEdge>> {

    val searchSources = contextualSources.map { ContextualEdge(it, it.node.target.element) }
    val sinkSet = contextualSinks.toSet()
    return searchForPaths(
            sources = searchSources,
            isSink = { it.contextualNode in sinkSet },
            getNeighbors = { outEdges(it.contextualNode) })
}

/** A context-sensitive search for paths from [sources] to [sinks]. */
fun CallGraph.searchForPaths(
        sources: Collection<Node>,
        sinks: Collection<Node>,
        nonContextualReceiverEval: IntraproceduralDispatchReceiverEvaluator
): Collection<List<ContextualEdge>> {

    val contextualGraph = buildContextualCallGraph(nonContextualReceiverEval)
    val sourceSet = sources.toSet()
    val sinkSet = sinks.toSet()
    val searchSources = contextualGraph.contextualNodes.filter { it.node in sourceSet }
    val searchSinks = contextualGraph.contextualNodes.filter { it.node in sinkSet }
    return contextualGraph.searchForContextualPaths(
            searchSources,
            searchSinks)
}