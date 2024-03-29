package com.ickphum.armature.objects

import com.ickphum.armature.Renderer
import com.ickphum.armature.enum.Axis
import com.ickphum.armature.programs.CylinderShaderProgram
import com.ickphum.armature.util.Geometry

private const val TAG = "Node"

class Node (private var center: Geometry.Point, private val radius: Float, id1 : Int, id2 : Int ) {
    companion object {
        private var classIndex = 0
        fun nextIndex() = classIndex++
        fun resetIndex(index : Int ) = run { classIndex = index }
    }

    enum class NodeElement {
        BODY {
            override fun axis() = null
        },
        X {
            override fun axis() = Axis.X
        },
        Y {
            override fun axis() = Axis.Y
        },
        Z {
            override fun axis() = Axis.Z
        };

        abstract fun axis(): Axis?
    }

    data class NodeTouch(
        val node: Node,
        val point: Geometry.Point,
        val element: NodeElement
    )

    var id = nextIndex()

    val cylinderIds = mutableSetOf( id1, id2 )

    var selected = false
    var highlighted = false

    private var icosahedron = Icosahedron (center.asVector(), radius )
    private var handle = Handle( center, radius * 1.5f )

    private val singleColor = floatArrayOf(0.8f, 0.5f, 0f, 1f)
    private val groupColor = floatArrayOf(0.97f, 0.53f, 0.08f, 1f)
    private val normalColor = floatArrayOf(0f, 0.5f, 0.8f, 1f)
    private val highlightColor = floatArrayOf(0f, 1f, 0f, 1f)

    fun draw(cylinderProgram: CylinderShaderProgram, state : Renderer.State, preDragState: Renderer.State) {

        if ( selected )
        {
            val color = if ( highlighted ) highlightColor else normalColor
            handle.bindData()
            handle.draw( cylinderProgram, color )
        }
        else
        {
            val color = if ( state == Renderer.State.GROUP || ( state == Renderer.State.PANNING && preDragState == Renderer.State.GROUP ))
                groupColor
            else
                singleColor
            icosahedron.bindData()
            icosahedron.draw( cylinderProgram, color )
        }
    }

    fun addCylinder( id: Int ) = cylinderIds.add( id )

    fun findIntersectionPoint( ray: Geometry.Ray, modelViewMatrix: FloatArray): NodeTouch? {
        if ( selected )
        {
            // the handle is displayed so pass off detection to that
            val handleHit = handle.findIntersectionPoint( ray, modelViewMatrix )
            if ( handleHit != null )
            {
                val nodeElement =
                    when (handleHit.element) {
                        Handle.HandleElement.X -> NodeElement.X
                        Handle.HandleElement.Y -> NodeElement.Y
                        else -> NodeElement.Z
                    }
                return NodeTouch( this, handleHit.point, nodeElement )
            }
        }
        else
            if ( Geometry.distanceBetween( center, ray) < radius )
            {
                // we return the point on the ico that's closest to the viewer, ie the ico center
                // translated back along the ray by the ico radius.
    //            Log.d( TAG, "Touched node with center $center")
                val rayVector = ray.vector.normalize().scale( radius )
    //            Log.d( TAG, "ray ${ray.vector}, ray vector $rayVector")
    //            val p1 = center.add( rayVector )
                val nearPoint = center.subtract( rayVector )
    //            Log.d( TAG, "p1 $p1, p2 $p2")
                return NodeTouch( this, nearPoint, NodeElement.BODY )
            }
        return null
    }

    fun changePosition(delta: Geometry.Vector) {
        if ( delta.length() > 0f ) {
            center = center.add(delta)
            handle.changePosition( delta )
            icosahedron.changePosition( delta )
        }
    }

}