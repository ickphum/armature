package com.ickphum.armature.objects

import com.ickphum.armature.Constants
import com.ickphum.armature.data.VertexArray
import com.ickphum.armature.programs.ColorShaderProgram
import com.ickphum.armature.util.Geometry


private const val POSITION_COMPONENT_COUNT = 3

class Mallet(private val radius: Float, public val height: Float, numPointsAroundMallet: Int ) {

    private val generatedData = ObjectBuilder.createMallet(
        Geometry.Point(
            0f,
            0f, 0f
        ), radius, height, numPointsAroundMallet
    )

    private val vertexArray: VertexArray = VertexArray(generatedData.vertexData)
    private val drawList: List<ObjectBuilder.DrawCommand> = generatedData.drawList

    fun bindData(colorProgram: ColorShaderProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            colorProgram.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT, 0
        )
    }

    fun draw() {
        for (drawCommand in drawList) {
            drawCommand.draw()
        }
    }


}