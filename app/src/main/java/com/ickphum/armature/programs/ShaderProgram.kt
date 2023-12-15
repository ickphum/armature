package com.ickphum.armature.programs

import android.content.Context
import android.opengl.GLES20.glUseProgram
import com.ickphum.armature.util.ShaderHelper

open class ShaderProgram( context: Context, vertexShaderResourceId: Int, fragmentShaderResourceId: Int) {

    // Uniform constants
    protected val U_MATRIX = "u_Matrix"
    protected val U_COLOR = "u_Color"
    protected val U_TEXTURE_UNIT = "u_TextureUnit"
    protected val U_TEXTURE_UNIT_1 = "u_TextureUnit1"
    protected val U_TEXTURE_UNIT_2 = "u_TextureUnit2"
    protected val U_TIME = "u_Time"
    protected val U_VECTOR_TO_LIGHT = "u_VectorToLight"
    protected val U_MV_MATRIX = "u_MVMatrix"
    protected val U_IT_MV_MATRIX = "u_IT_MVMatrix"
    protected val U_MVP_MATRIX = "u_MVPMatrix"
    protected val U_POINT_LIGHT_POSITIONS = "u_PointLightPositions"
    protected val U_POINT_LIGHT_COLORS = "u_PointLightColors"

    // Attribute constants
    protected val A_POSITION: String? = "a_Position"
    protected val A_COLOR = "a_Color"
    protected val A_NORMAL = "a_Normal"
    protected val A_TEXTURE_COORDINATES = "a_TextureCoordinates"
    protected val A_DIRECTION_VECTOR = "a_DirectionVector"
    protected val A_PARTICLE_START_TIME = "a_ParticleStartTime"

    protected var program =
        ShaderHelper.buildProgram(context, vertexShaderResourceId, fragmentShaderResourceId)

    open fun useProgram() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(program)
    }
}
