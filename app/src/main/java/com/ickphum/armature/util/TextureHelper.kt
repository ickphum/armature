package com.ickphum.armature.util

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils.texImage2D
import android.util.Log

private const val TAG = "TextureHelper"

class TextureHelper {

    companion object Helper {
        fun loadTexture( context: Context, resourceId: Int ) : Int {
            val textureObjectIds = IntArray(1)
            glGenTextures(1, textureObjectIds, 0);
            if (textureObjectIds[0] == 0) {
                Log.w(TAG, "Could not generate a new OpenGL texture object.");
                return 0;
            }

            val options = BitmapFactory.Options()
            options.inScaled = false

            val bitmap = BitmapFactory.decodeResource( context.resources, resourceId, options )
            if ( bitmap == null ) {
                Log.w(TAG, "Resource ID %d could not be decoded".format( resourceId ))
                glDeleteTextures(1, textureObjectIds, 0);
                return 0
            }

            glBindTexture(GL_TEXTURE_2D, textureObjectIds[0]);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

            bitmap.recycle();

            glGenerateMipmap(GL_TEXTURE_2D);

            glBindTexture(GL_TEXTURE_2D, 0);

            return textureObjectIds[0];
        }
    }
}