package com.ickphum.armature.util

import android.content.Context
import android.graphics.Bitmap
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

        fun loadCubeMap(context: Context, cubeResources: IntArray): Int {
            val textureObjectIds = IntArray(1)
            glGenTextures(1, textureObjectIds, 0)
            if (textureObjectIds[0] == 0) {
                Log.w(TAG, "Could not generate a new OpenGL texture object.")
                return 0
            }
            val options = BitmapFactory.Options()
            options.inScaled = false
            val cubeBitmaps = arrayOfNulls<Bitmap>(6)
            for (i in 0..5) {
                cubeBitmaps[i] = BitmapFactory.decodeResource(
                    context.resources,
                    cubeResources[i], options
                )
                if (cubeBitmaps[i] == null) {
                    Log.w(
                        TAG, "Resource ID " + cubeResources[i] + " could not be decoded."
                    )
                    glDeleteTextures(1, textureObjectIds, 0)
                    return 0
                }
            }
            glBindTexture(GL_TEXTURE_CUBE_MAP, textureObjectIds[0])

            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

            texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, cubeBitmaps[0], 0)
            texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, cubeBitmaps[1], 0)
            texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, cubeBitmaps[2], 0)
            texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, cubeBitmaps[3], 0)
            texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, cubeBitmaps[4], 0)
            texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, cubeBitmaps[5], 0)

            glBindTexture(GL_TEXTURE_2D, 0)

            for (bitmap in cubeBitmaps) {
                bitmap!!.recycle()
            }

            return textureObjectIds[0]
        }
    }
}
