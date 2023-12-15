package com.ickphum.armature.data

import android.opengl.GLES20.GL_ARRAY_BUFFER
import android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER
import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_STATIC_DRAW
import android.opengl.GLES20.glBindBuffer
import android.opengl.GLES20.glBufferData
import android.opengl.GLES20.glEnableVertexAttribArray
import android.opengl.GLES20.glGenBuffers
import android.opengl.GLES20.glVertexAttribPointer
import com.ickphum.armature.Constants.BYTES_PER_SHORT
import java.nio.ByteBuffer
import java.nio.ByteOrder


class IndexBuffer(indexData: ShortArray ) {
    private var bufferId = 0

    init {
        val buffers = IntArray(1)
        glGenBuffers(buffers.size, buffers, 0)
        if (buffers[0] == 0) {
            throw RuntimeException("Could not create a new index buffer object.");
        }
        bufferId = buffers[0];

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[0]);

        // Transfer data to native memory.
        val indexArray = ByteBuffer
            .allocateDirect(indexData.size * BYTES_PER_SHORT)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indexData)
        indexArray.position(0)

        // Transfer data from native memory to the GPU buffer.
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexArray.capacity() * BYTES_PER_SHORT,
            indexArray, GL_STATIC_DRAW);

        // IMPORTANT: Unbind from the buffer when we're done with it.
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    fun getBufferId(): Int {
        return bufferId
    }
}