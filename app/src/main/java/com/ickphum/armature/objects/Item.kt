package com.ickphum.armature.objects

import com.ickphum.armature.Renderer
import com.ickphum.armature.enum.Axis
import com.ickphum.armature.programs.CylinderShaderProgram
import com.ickphum.armature.util.Geometry

abstract class Item ( var selected: Boolean = true ){

    enum class ItemElement {
        BODY {
            override fun axis() = null
            override fun top() = true
            override fun bottom() = true
        },
        BOTTOM_X {
            override fun axis() = Axis.X
            override fun top() = false
            override fun bottom() = true
        },
        BOTTOM_Y {
            override fun axis() = Axis.Y
            override fun top() = false
            override fun bottom() = true
        },
        BOTTOM_Z {
            override fun axis() = Axis.Z
            override fun top() = false
            override fun bottom() = true
        },
        TOP_X {
            override fun axis() = Axis.X
            override fun top() = true
            override fun bottom() = false
        },
        TOP_Y {
            override fun axis() = Axis.Y
            override fun top() = true
            override fun bottom() = false
        },
        TOP_Z {
            override fun axis() = Axis.Z
            override fun top() = true
            override fun bottom() = false
        },
        X {
            override fun axis() = Axis.X
            override fun top() = false
            override fun bottom() = false
        },
        Y {
            override fun axis() = Axis.Y
            override fun top() = false
            override fun bottom() = false
        },
        Z {
            override fun axis() = Axis.Z
            override fun top() = false
            override fun bottom() = false
        };

        abstract fun axis(): Axis?
        abstract fun top(): Boolean
        abstract fun bottom(): Boolean
    }

    data class ItemTouch(
        val item: Item,
        val point: Geometry.Point,
        val element: ItemElement
    )

    companion object {
        private var classIndex = 0
        fun nextIndex() = classIndex++
        fun resetIndex(index : Int ) = run { classIndex = index }
    }

    var id = nextIndex()

    abstract val touchableType : Renderer.TouchableObjectType

    abstract fun thisMoveBlocked(element: ItemElement ) : Boolean

    abstract fun draw(cylinderProgram: CylinderShaderProgram, state : Renderer.State, preDragState: Renderer.State)

    abstract fun findIntersectionPoint(ray: Geometry.Ray, modelViewMatrix: FloatArray): ItemTouch?

}