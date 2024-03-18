package com.ickphum.armature.util

import android.content.Context
import android.opengl.GLES20.GL_COMPILE_STATUS
import android.opengl.GLES20.GL_FRAGMENT_SHADER
import android.opengl.GLES20.GL_LINK_STATUS
import android.opengl.GLES20.GL_VALIDATE_STATUS
import android.opengl.GLES20.GL_VERTEX_SHADER
import android.opengl.GLES20.glAttachShader
import android.opengl.GLES20.glCompileShader
import android.opengl.GLES20.glCreateProgram
import android.opengl.GLES20.glCreateShader
import android.opengl.GLES20.glDeleteProgram
import android.opengl.GLES20.glDeleteShader
import android.opengl.GLES20.glDetachShader
import android.opengl.GLES20.glGetProgramInfoLog
import android.opengl.GLES20.glGetProgramiv
import android.opengl.GLES20.glGetShaderiv
import android.opengl.GLES20.glLinkProgram
import android.opengl.GLES20.glShaderSource
import android.opengl.GLES20.glValidateProgram
import android.util.Log
import com.ickphum.armature.util.Logging.Helper.checkError


private const val TAG = "ShaderHelper"

class ShaderHelper {

    companion object Helper {
        private fun compileShader(context: Context, type: Int, sourceResourceId: Int): Int {
            val shaderObjectId = glCreateShader(type);
            if (shaderObjectId == 0) {
                Log.w(TAG, "Could not create new shader.");
                return 0;
            }

            val rawText = context.resources.openRawResource( sourceResourceId )
//            val rawText = context.assets.open(sourceFileName)
            val shaderString = rawText.bufferedReader().use { it.readText() }
            Log.w( TAG, shaderString )

            glShaderSource(shaderObjectId, shaderString)
            glCompileShader(shaderObjectId)

            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0)
//            Log.v(TAG, "Results of compiling source:" + "\n" + sourceResourceId + "\n:" + glGetShaderInfoLog(shaderObjectId));
            if (compileStatus[0] == 0) {
                // If it failed, delete the shader object.
                glDeleteShader(shaderObjectId);
                Log.w(TAG, "Compilation of shader failed.");
                return 0;
            }
            return shaderObjectId;
        }
        private fun compileVertexShader(context: Context, sourceResourceId: Int): Int {
            return compileShader(context, GL_VERTEX_SHADER, sourceResourceId)
        }

        private fun compileFragmentShader(context: Context, sourceResourceId: Int): Int {
            return compileShader(context, GL_FRAGMENT_SHADER, sourceResourceId)
        }

        private fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
            val programObjectId = glCreateProgram();
            if (programObjectId == 0) {
                Log.w(TAG, "Could not create new program");
                return 0;
            }
            glAttachShader(programObjectId, vertexShaderId);
            glAttachShader(programObjectId, fragmentShaderId);

            glLinkProgram(programObjectId);

            val linkStatus = IntArray(1)
            glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                // If it failed, delete the program object.
                glDeleteProgram(programObjectId);
                Log.w(TAG, "Linking of program failed.");
                return 0;
            }
            return programObjectId;
        }

        private fun validateProgram(programObjectId: Int): Boolean {
            glValidateProgram(programObjectId)
            val validateStatus = IntArray(1)
            glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0)
            Log.v(
                TAG, """
     Results of validating program: ${validateStatus[0]}
     Log:${glGetProgramInfoLog(programObjectId)}
     """.trimIndent()
            )
            return validateStatus[0] != 0
        }

        fun buildProgram(
            context: Context,
            vertexShaderResourceId: Int,
            fragmentShaderResourceId: Int
        ): Int {
            val program: Int
            // Compile the shaders.
            val vertexShader = compileVertexShader(context, vertexShaderResourceId)
            val fragmentShader = compileFragmentShader(context, fragmentShaderResourceId)
            // Link them into a shader program.
            program = linkProgram(vertexShader, fragmentShader)
            validateProgram(program)
            val dbgDomain = "buildProgram"

            //After we have linked the program, it's a good idea to detach the shaders from it:
            glDetachShader(program, vertexShader)
            checkError(dbgDomain, "Failed to detach vertex shader")

            glDetachShader(program, fragmentShader)
            checkError(dbgDomain, "Failed to detach fragment shader")

            //We don't need the shaders anymore, so we can delete them right here:
            glDeleteShader(vertexShader)
            checkError(dbgDomain, "Failed to delete vertex shader")

            glDeleteShader(fragmentShader)
            checkError(dbgDomain, "Failed to delete fragment shader")

            return program
        }
    }
}
