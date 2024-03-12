package com.ickphum.armature.objects

import com.ickphum.armature.programs.CylinderShaderProgram
import com.ickphum.armature.util.Geometry

private const val TAG = "Node"

class Node ( val center: Geometry.Point, val radius: Float, id1 : Int, id2 : Int ) {
    companion object {
        private var classIndex = 0
        fun nextIndex() = classIndex++
        fun resetIndex(index : Int ) = run { classIndex = index }
    }

    var id = Node.nextIndex()

    private val cylinderIds = mutableSetOf( id1, id2 )

    var selected = false
    var highlighted = false

    private var icosahedron = Icosahedron (center.asVector(), radius )

    private val singleColor = floatArrayOf(0.8f, 0.5f, 0f, 1f)
    private val groupColor = floatArrayOf(0.97f, 0.53f, 0.08f, 1f)
    private val normalColor = floatArrayOf(0f, 0.5f, 0.8f, 1f)
    private val highlightColor = floatArrayOf(0f, 1f, 0f, 1f)
    private val handleColor = floatArrayOf(0.5f, 0f, 0.8f, 1f)

    fun draw( cylinderProgram: CylinderShaderProgram ) {
        icosahedron.bindData()
        icosahedron.draw( cylinderProgram, if ( highlighted ) highlightColor else normalColor )
    }

    fun addCylinder( id: Int ) = cylinderIds.add( id )

}