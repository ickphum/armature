package com.ickphum.armature.objects

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER
import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_UNSIGNED_SHORT
import android.opengl.GLES20.glBindBuffer
import android.opengl.GLES20.glDrawElements
import com.ickphum.armature.Constants.BYTES_PER_FLOAT
import com.ickphum.armature.data.IndexBuffer
import com.ickphum.armature.data.VertexBuffer
import com.ickphum.armature.programs.HeightmapShaderProgram
import com.ickphum.armature.util.Geometry
import com.ickphum.armature.util.Geometry.Helper.intClamp


class Heightmap( bitmap: Bitmap ) {
    private val POSITION_COMPONENT_COUNT = 3
    private val NORMAL_COMPONENT_COUNT = 3
    private val TOTAL_COMPONENT_COUNT = POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT
    private val STRIDE: Int = (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT) * BYTES_PER_FLOAT

    private val width = bitmap.width
    private val height = bitmap.height
    private val numElements = calculateNumElements()
    private lateinit var vertexBuffer: VertexBuffer
    private lateinit var indexBuffer: IndexBuffer

    init {
        if ( width * height > 65536 )
            throw RuntimeException( "Heightmap is too large for the index buffer." )
        vertexBuffer = VertexBuffer(loadBitmapData(bitmap));
        indexBuffer = IndexBuffer(createIndexData());
    }

    private fun loadBitmapData(bitmap: Bitmap): FloatArray {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        val heightmapVertices = FloatArray(width * height * TOTAL_COMPONENT_COUNT)
        var offset = 0
        for (row in 0 until height) {
            for (col in 0 until width) {

                val point = getPoint(pixels, row, col);
                heightmapVertices[offset++] = point.x
                heightmapVertices[offset++] = point.y
                heightmapVertices[offset++] = point.z

                val top = getPoint(pixels, row - 1, col);
                val left = getPoint(pixels, row, col - 1);
                val right = getPoint(pixels, row, col + 1);
                val bottom = getPoint(pixels, row + 1, col);

                val rightToLeft = Geometry.vectorBetween(right, left);
                val topToBottom = Geometry.vectorBetween(top, bottom);
                val normal = rightToLeft.crossProduct(topToBottom).normalize();

                heightmapVertices[offset++] = normal.x;
                heightmapVertices[offset++] = normal.y;
                heightmapVertices[offset++] = normal.z;
            }
        }
        return heightmapVertices
    }

    private fun calculateNumElements(): Int {
        return (width - 1) * (height - 1) * 2 * 3
    }

    private fun createIndexData(): ShortArray {
        val indexData = ShortArray(numElements)
        var offset = 0
        for (row in 0 until height - 1) {
            for (col in 0 until width - 1) {
                val topLeftIndexNum = (row * width + col).toShort()
                val topRightIndexNum = (row * width + col + 1).toShort()
                val bottomLeftIndexNum = ((row + 1) * width + col).toShort()
                val bottomRightIndexNum = ((row + 1) * width + col + 1).toShort()
                // Write out two triangles.
                indexData[offset++] = topLeftIndexNum
                indexData[offset++] = bottomLeftIndexNum
                indexData[offset++] = topRightIndexNum
                indexData[offset++] = topRightIndexNum
                indexData[offset++] = bottomLeftIndexNum
                indexData[offset++] = bottomRightIndexNum
            }
        }
        return indexData
    }

    fun bindData(heightmapProgram: HeightmapShaderProgram) {
        vertexBuffer.setVertexAttribPointer(
            0,
            heightmapProgram.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT, STRIDE
        )

        vertexBuffer.setVertexAttribPointer(
            POSITION_COMPONENT_COUNT * BYTES_PER_FLOAT,
            heightmapProgram.getNormalAttributeLocation(),
            NORMAL_COMPONENT_COUNT, STRIDE);
    }

    private fun getPoint(pixels: IntArray, row: Int, col: Int): Geometry.Point {
        var row = row
        var col = col
        val x = col.toFloat() / (width - 1).toFloat() - 0.5f
        val z = row.toFloat() / (height - 1).toFloat() - 0.5f
        row = intClamp(row, 0, width - 1)
        col = intClamp(col, 0, height - 1)
        val y = Color.red(pixels[row * height + col]).toFloat() / 255f
        return Geometry.Point(x, y, z)
    }

    fun draw() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getBufferId())
        glDrawElements(GL_TRIANGLES, numElements, GL_UNSIGNED_SHORT, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }
}