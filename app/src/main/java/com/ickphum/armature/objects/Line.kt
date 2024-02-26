package com.ickphum.armature.objects

import android.opengl.GLES20
import com.ickphum.armature.Constants.POSITION_COMPONENT_COUNT
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.enum.Axis
import com.ickphum.armature.programs.LineShaderProgram
import com.ickphum.armature.util.Geometry

class Line (size: Float, private val axis: Axis, private var point: Geometry.Point ){
    companion object {
        private const val TAG = "Line"
    }

    private val vertexData = FloatArray( POSITION_COMPONENT_COUNT * 2 )
    private lateinit var vertexArray: VertexArray

    init {
        val otherAxes = axis.otherAxes()

        vertexData[ axis.axis() ] = -size
        vertexData[ POSITION_COMPONENT_COUNT + axis.axis() ] = size

        // other axes match the input point
        vertexData[ otherAxes[0] ] = point[ otherAxes[ 0 ] ]
        vertexData[ otherAxes[1] ] = point[ otherAxes[ 1 ] ]
        vertexData[ POSITION_COMPONENT_COUNT + otherAxes[0] ] = point[ otherAxes[ 0 ] ]
        vertexData[ POSITION_COMPONENT_COUNT + otherAxes[1] ] = point[ otherAxes[ 1 ] ]

        vertexArray = VertexArray( vertexData )
        vertexData
    }

    fun bindData(program: LineShaderProgram) {
        vertexArray.setVertexAttribPointer(0,0, POSITION_COMPONENT_COUNT, 0 )
    }

    fun draw() {
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2 )
    }

}