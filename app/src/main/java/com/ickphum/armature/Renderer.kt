package com.ickphum.armature

/*
Plans

Easily snap cylinder(s) back to a plane? Maybe fixed by snapping anyway?

When adjusting an item, show guide planes when a moving end shares a coord with another item,
and a guide line when 2 coords are shared

Long press on a handle to change the build plane

 */

import android.content.Context
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_TEST
import android.opengl.GLES20.GL_LEQUAL
import android.opengl.GLES20.GL_LESS
import android.opengl.GLES20.GL_ONE
import android.opengl.GLES20.GL_VERSION
import android.opengl.GLES20.glBlendFunc
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glDepthFunc
import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glGetString
import android.opengl.GLES20.glLineWidth
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import android.opengl.Matrix.invertM
import android.opengl.Matrix.multiplyMM
import android.opengl.Matrix.rotateM
import android.opengl.Matrix.setIdentityM
import android.opengl.Matrix.translateM
import android.opengl.Matrix.transposeM
import android.os.SystemClock
import android.util.Log
import com.ickphum.armature.enum.Axis
import com.ickphum.armature.objects.Cylinder
import com.ickphum.armature.objects.Handle
import com.ickphum.armature.objects.Icosahedron
import com.ickphum.armature.objects.Item
import com.ickphum.armature.objects.Line
import com.ickphum.armature.objects.Mesh
import com.ickphum.armature.objects.Model
import com.ickphum.armature.objects.Node
import com.ickphum.armature.objects.Skybox
import com.ickphum.armature.programs.CylinderShaderProgram
import com.ickphum.armature.programs.LineShaderProgram
import com.ickphum.armature.programs.MeshShaderProgram
import com.ickphum.armature.programs.SkyboxShaderProgram
import com.ickphum.armature.util.Geometry
import com.ickphum.armature.util.TextureHelper
import glm_.glm
import glm_.vec3.Vec3
import kotlinx.serialization.json.Json
import javax.microedition.khronos.opengles.GL10
import kotlin.math.floor
import kotlin.math.sin

private const val TAG = "Renderer"
private const val DEFAULT_ITEM_RADIUS = 0.3f
private const val BASE_SIZE = 5f
private const val NODE_RADIUS = 0.625f

class Renderer(private val context: Context) : GLSurfaceView.Renderer {

    enum class State {
        SELECT, SINGLE, GROUP, PANNING, MOVE
    }

    enum class TouchableObjectType {
        CYLINDER, NODE, BASE
    }

    enum class LightRotation {
        X, Y, Z, None
    }

    enum class Congruency {
        EXISTING_NODE {
            override fun meshRequired() = false
            override fun lineRequired() = false
            override fun axis() = Axis.NONE
            override fun score() = 4
        },
        SAME_POINT {
            override fun meshRequired() = false
            override fun lineRequired() = false
            override fun axis() = Axis.NONE
            override fun score() = 3
        },
        SAME_XY {
            override fun meshRequired() = false
            override fun lineRequired() = true
            override fun axis() = Axis.Z
            override fun score() = 2
        },
        SAME_XZ {
            override fun meshRequired() = false
            override fun lineRequired() = true
            override fun axis() = Axis.Y
            override fun score() = 2
        },
        SAME_YZ {
            override fun meshRequired() = false
            override fun lineRequired() = true
            override fun axis() = Axis.X
            override fun score() = 2
        },
        SAME_X {
            override fun meshRequired() = true
            override fun lineRequired() = false
            override fun axis() = Axis.X
            override fun score() = 1
        },
        SAME_Y {
            override fun meshRequired() = true
            override fun lineRequired() = false
            override fun axis() = Axis.Y
            override fun score() = 1
        },
        SAME_Z {
            override fun meshRequired() = true
            override fun lineRequired() = false
            override fun axis() = Axis.Z
            override fun score() = 1
        },
        NONE {
            override fun meshRequired() = false
            override fun lineRequired() = false
            override fun axis() = Axis.NONE
            override fun score() = 0
        };

        abstract fun meshRequired() : Boolean
        abstract fun lineRequired() : Boolean
        abstract fun axis() : Axis
        abstract fun score() : Int
    }

    data class TouchedObject(val type: TouchableObjectType, val itemTouch: Item.ItemTouch?, val basePoint: Geometry.Point? ) {
        constructor(type: Renderer.TouchableObjectType, item: Item.ItemTouch) : this( type, item, null )
        constructor(type: Renderer.TouchableObjectType, basePoint: Geometry.Point) : this( type, null, basePoint )
    }

    private var touchedObject: TouchedObject? = null

    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val viewMatrixForSkybox = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val invertedViewProjectionMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val itModelViewMatrix = FloatArray(16)

    private var globalStartTime: Long = 0

    private lateinit var skyboxProgram: SkyboxShaderProgram
    private lateinit var skybox: Skybox
    private var skyboxTexture = 0

    private lateinit var meshProgram: MeshShaderProgram
    private lateinit var base: Mesh
    private var mesh: Mesh? = null
    private var previousMeshPoint: Geometry.Point? = null
    private var snapHandle: Geometry.Point? = null
    private var prevHandleOffset = Geometry.Vector ( 0f, 0f, 0f )

    // this will hold the axis for the dragging plane for the next item edit;
    // it will always be reset by the creation of the first item so there's no point
    // setting it now.
    // It's also used to draw the item handles.
    private var nextPlane: Axis? = null

    enum class PreviousTouch {
        NOTHING, BASE, ITEM
    }
    private var previousTouch = PreviousTouch.NOTHING

    private lateinit var cylinderProgram: CylinderShaderProgram

    private lateinit var lineProgram: LineShaderProgram

    private var xRotation = 0f
    private var yRotation = 25f

    private var vectorToLight = Geometry.Vector(3f, 5f, 10f).normalize()
    private val rotation: LightRotation = LightRotation.None

    private var state = State.SELECT
    private var preDragState = State.SELECT

    data class CongruentPoint(val point: Geometry.Point, val congruency : Congruency, val from: Int, val fromTop: Boolean, val to: Int, val toTop: Boolean ) {
        var mesh : Mesh? = null
        var line : Line? = null
        var icosahedron: Icosahedron? = null
        init {
            if ( congruency.meshRequired() )
                mesh = Mesh( BASE_SIZE / 2f, congruency.axis(), point.asArray()[ congruency.axis().axis() ] )
            if ( congruency.lineRequired() )
                line = Line(BASE_SIZE / 2f, congruency.axis(), point)
            if ( congruency === Congruency.SAME_POINT )
                icosahedron = Icosahedron( point.asVector(), NODE_RADIUS )
        }
    }
    private val congruencies = mutableListOf<CongruentPoint>()

    private val model = Model()

    private var testHandle : Handle? = null

    override fun onSurfaceCreated(glUnused: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {

        val version = glGetString(GL_VERSION)

        glClearColor(0.2f, 0.0f, 0.0f, 0.0f)
        glEnable(GL_DEPTH_TEST)
//        glEnable(GL_CULL_FACE)

        Log.d( TAG, "************** version $version ******************")

        globalStartTime = System.nanoTime();

        skyboxProgram = SkyboxShaderProgram( context )
        skybox = Skybox()
        skyboxTexture = TextureHelper.loadCubeMap( context,
            intArrayOf(R.drawable.left, R.drawable.right,
                R.drawable.bottom, R.drawable.top,
                R.drawable.front, R.drawable.back))

        meshProgram = MeshShaderProgram( context )
        base = Mesh( BASE_SIZE / 2f, Axis.Y, 0f )

        cylinderProgram = CylinderShaderProgram( context )

        lineProgram = LineShaderProgram( context )

        Log.d( TAG, "Handle created $testHandle")
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        MatrixHelper.perspectiveM(
            projectionMatrix, 45f,
            width.toFloat() / height.toFloat(), 1f, 100f
        )
        updateViewMatrices()
    }

    override fun onDrawFrame(glUnused: GL10?) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT )

        if ( rotation === LightRotation.Y ) {
            val time = SystemClock.uptimeMillis() % 6280
            val angle = 0.002f * time.toInt()

            val rotationAxis = Geometry.Vector(0f, 1f, 0f)
            val rotationQuat = glm.angleAxis(angle, rotationAxis.x, rotationAxis.y, rotationAxis.z)
            val positionR = rotationQuat.times(Vec3(2f, 0f, 0f))
            vectorToLight.fromVec3(positionR)
        }

        drawSkybox()
        drawCylinders()
        drawLines()
        drawBase()
    }

    private fun updateViewMatrices() {
        setIdentityM(viewMatrix, 0)
        rotateM(viewMatrix, 0, yRotation, 1f, 0f, 0f)
        rotateM(viewMatrix, 0, xRotation, 0f, 1f, 0f)
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.size )

        setIdentityM(viewMatrix, 0)
        translateM(viewMatrix, 0, 0f, -4f, -15f)
        rotateM(viewMatrix, 0, xRotation, 0f, 1f, 0f)

        // xFactor formula
        // =(MAX(MOD(B2,360), 360-MOD(B2,360)) - 270)/90
        val xRotMod360 = xRotation - (floor( xRotation / 360f ) * 360f);
        val zRotMod360 = (xRotation - 90f) - (floor( (xRotation - 90f) / 360f ) * 360f);

        val xFactor = ( xRotMod360.coerceAtLeast(360f - xRotMod360) - 270f ) / 90f
        val zFactor = ( zRotMod360.coerceAtLeast(360f - zRotMod360) - 270f ) / 90f;

        rotateM(viewMatrix, 0, yRotation, xFactor, 0f, zFactor )

    }

    private fun updateMvpMatrix() {
        multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        invertM(tempMatrix, 0, modelViewMatrix, 0)
        transposeM(itModelViewMatrix, 0, tempMatrix, 0)
        multiplyMM(
            modelViewProjectionMatrix, 0,
            projectionMatrix, 0,
            modelViewMatrix, 0
        )
        invertM(invertedViewProjectionMatrix, 0, modelViewProjectionMatrix, 0);
    }

    private fun updateMvpMatrixForSkybox() {
        multiplyMM(tempMatrix, 0, viewMatrixForSkybox, 0, modelMatrix, 0)
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
    }

    private fun drawSkybox() {
        setIdentityM(modelMatrix, 0);
        updateMvpMatrixForSkybox();

        skyboxProgram.useProgram()
        skyboxProgram.setUniforms(modelViewProjectionMatrix, skyboxTexture)
        skybox.bindData()
        glDepthFunc(GL_LEQUAL);
        skybox.draw()
        glDepthFunc(GL_LESS);
    }

    private fun drawBase() {

        val currentTime = (System.nanoTime() - globalStartTime) / 1000000000f

        // pct is in range 0-1
        val pct = if (state == State.SELECT) ((sin(currentTime * 3f) + 1f) / 2f)  else 0.5f

        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE)
//        glEnable(GL_POINT_SMOOTH)

        meshProgram.useProgram()

        meshProgram.setUniforms( modelViewProjectionMatrix, currentTime, 0f, 0.2f + 0.2f * pct, 0.1f )
        base.bindData()
        base.draw()

        if ( mesh != null ) {

            // while a mesh is displayed, we can't be in SELECT so pct is not changing
            meshProgram.setUniforms( modelViewProjectionMatrix, currentTime, 0.3f, 0.2f, 0.1f )
            mesh!!.bindData()
            mesh!!.draw()
        }

        meshProgram.setUniforms( modelViewProjectionMatrix, currentTime, 0.1f, 0.3f, 0.2f )
        for (congruency in congruencies.filter { c -> c.mesh != null } ) {
            congruency.mesh!!.bindData()
            congruency.mesh!!.draw()
        }

        glDisable(GL_BLEND)
    }

    private fun drawLines() {

        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        glLineWidth( 10f )

        lineProgram.useProgram()

        lineProgram.setUniforms( modelViewProjectionMatrix, 0f, 0.8f, 0.1f )

        for (congruency in congruencies.filter { c -> c.line != null } ) {
            congruency.line!!.bindData()
            congruency.line!!.draw()
        }
    }

    private fun drawCylinders() {

        if (model.items().isEmpty()) return

        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        cylinderProgram.useProgram()
        cylinderProgram.setUniforms( modelViewProjectionMatrix, vectorToLight )

        for ( cyl in model.cylinders() ) {
            cyl.bindData()
            // @todo pass in nextPlane so we can indicate the body move plane via the handle color
            cyl.draw(cylinderProgram, state, preDragState)
        }

        for ( node in model.nodes() ) {
            node.draw( cylinderProgram, state, preDragState )
        }

        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE)
        for (congruency in congruencies.filter { c -> c.icosahedron != null } ) {
            congruency.icosahedron!!.bindData()
            congruency.icosahedron!!.draw( cylinderProgram, floatArrayOf(0.5f, 0.1f, 0.5f, 1f) )
        }
        glDisable(GL_BLEND)
    }

    fun handleTouchDown(normalizedX: Float, normalizedY: Float) : Int {
        val ray = Geometry.convertNormalized2DPointToRay(normalizedX, normalizedY, invertedViewProjectionMatrix )

        touchedObject = null

        var maxZ : Float? = null

        for (item in model.items() ) {
            val hit = item.findIntersectionPoint( ray, modelViewMatrix )
            if ( hit != null ) {
                val newMax = Geometry.compareTouchedPoint( hit.point, maxZ, modelViewMatrix )
                if ( newMax !== null ) {
                    maxZ = newMax
                    touchedObject = TouchedObject( item.touchableType, hit )
                }
            }
        }

        if ( touchedObject != null ) {
            previousTouch = PreviousTouch.ITEM
            var axis: Axis? = null

            if (touchedObject!!.type == TouchableObjectType.CYLINDER || touchedObject!!.type == TouchableObjectType.NODE) {
                axis = touchedObject!!.itemTouch!!.element.axis()
                previousMeshPoint = touchedObject!!.itemTouch!!.point
            }

            // if we touched a handle, switch to the correct plane
            if (axis != null)
                nextPlane = axis

            mesh = Mesh(
                BASE_SIZE / 2f,
                nextPlane!!,
                previousMeshPoint!!.asArray()[nextPlane!!.axis()]
            )

            // recalculate the snap points around the touched point
            mesh!!.findIntersectionPoint(ray)
        }
        else {

            // basePoint is only set if the touched point was within the base boundary
            val basePoint = base.findIntersectionPoint(ray)
            if ( basePoint != null ) {
                touchedObject = TouchedObject( TouchableObjectType.BASE, basePoint )
                previousTouch = PreviousTouch.BASE
            }
            else
                previousTouch = PreviousTouch.NOTHING

        }
        return previousTouch.ordinal
    }

    fun handleShortPress( normalizedX: Float, normalizedY: Float) {

        if ( previousTouch == PreviousTouch.ITEM ) {
            if ( state == State.SINGLE ) {
                val selectedItem = model.findSelected()
                if ( selectedItem == touchedObject!!.itemTouch!!.item ) {

                    // note that touching a handle will immediately reset nextPlane as required
                    nextPlane = nextPlane!!.nextAxis()

                }
                else {
                    model.clearSelections()
                    touchedObject!!.itemTouch!!.item.selected = true
                }
            }
            else if ( state == State.GROUP ) {
                if ( touchedObject!!.type == TouchableObjectType.CYLINDER || touchedObject!!.type == TouchableObjectType.NODE ) {
                    if (touchedObject!!.itemTouch!!.item.selected)
                        nextPlane = nextPlane!!.nextAxis()
                    else
                        touchedObject!!.itemTouch!!.item.selected = true
                }
            }
            else if ( state == State.SELECT ) {
                if ( touchedObject!!.type == TouchableObjectType.CYLINDER || touchedObject!!.type == TouchableObjectType.NODE ) {
                    touchedObject!!.itemTouch!!.item.selected = true
                    state = State.SINGLE

                    // this is how to serialize an object
                    if ( touchedObject!!.type == TouchableObjectType.CYLINDER ) {
                        val json = touchedObject!!.itemTouch!!.item.toJson()
                        Log.d(TAG, "New cylinder $json")
                        val cj = Json.decodeFromString<Cylinder.CylinderJSON>( json )
                        Log.d( TAG, "cj $cj")
                    }
                }
            }
            mesh = null
        }
        else if ( previousTouch == PreviousTouch.NOTHING ) {
            model.clearSelections()
            state = State.SELECT
        }
    }

    fun handleDragStart() {

        preDragState = state

        if ( touchedObject?.type == TouchableObjectType.CYLINDER && touchedObject!!.itemTouch!!.item.selected
            && ( state == State.SINGLE || state == State.GROUP ) )
        {
            val element = touchedObject!!.itemTouch!!.element

            // before we start a move, we have to check the selected cylinder(s) for nodes;
            // any cylinder joined to a node prevents that end of the cylinder being moved.
            if ( model.items().none { i -> i.thisMoveBlocked( element )})
            {
                state = State.MOVE
                snapHandle = mesh!!.nearestSnapPoint( previousMeshPoint!! )
                prevHandleOffset = Geometry.Vector(0f, 0f ,0f)
            }
            else
                Log.d( TAG, "No move, locked cylinder(s) selected")

        }
        else if ( touchedObject?.type == TouchableObjectType.NODE && touchedObject!!.itemTouch!!.item.selected
            && ( state == State.SINGLE || state == State.GROUP ) )
        {
            state = State.MOVE
            snapHandle = mesh!!.nearestSnapPoint( previousMeshPoint!! )
            prevHandleOffset = Geometry.Vector(0f, 0f ,0f)
        }
        else if ( touchedObject?.type == TouchableObjectType.BASE && state == State.SELECT ) {

            snapHandle = base.nearestSnapPoint( touchedObject!!.basePoint!! )
            prevHandleOffset = Geometry.Vector(0f, 0f ,0f)

            // add a new item
            val cylinder = Cylinder(snapHandle!!, snapHandle!!.translateY( 0.01f ), DEFAULT_ITEM_RADIUS )
            model.addCylinder( cylinder )

            preDragState = State.SINGLE
            state = State.MOVE
            mesh = Mesh( BASE_SIZE / 2f, Axis.Z, snapHandle!!.z)

            nextPlane = Axis.Z

            // change touchedObject to emulate a click on the top Z handle of the new cylinder
            previousMeshPoint = snapHandle!!.translateY( 0.01f )
            touchedObject = TouchedObject( TouchableObjectType.CYLINDER,
                Item.ItemTouch( cylinder, previousMeshPoint!!, Item.ItemElement.TOP_Z ),
                )
        }
        else {
            state = State.PANNING
        }

    }

    private fun checkForCongruency(element: Item.ItemElement, cylinder: Cylinder, other: Cylinder) {

        // we're moving one end of the cylinder or the whole thing ie both ends. We want to know
        // when one of the moving ends come into congruence with an existing cylinder, by which we
        // mean either the top or bottom end of the existing cylinder.
        // We check the moving end(s) against both ends of unselected cylinders; for selected cylinders,
        // we only check the end that isn't moving, since the moving end is, uh, moving, so the relationship
        // between the moving end(s) of the selected cylinders won't change.

        // We're finding these congruencies for two reasons:
        //  1) We want to display meshes and lines to help the user line up items
        //  2) We want to know what cylinder ends may be joined to create a new node
        // The upshot is that we only want to see one congruency of each type; for display purposes,
        // one is enough, and for creating a node, we want each cylinder that matches the node location.
        // What we'll do is find all the congruencies for this pair, and then check them against the
        // existing congruencies. If we find matches by Congruency.type, we either skip them, or
        // in the case of Congruency.SAME_POINT, we add the other cylinder to the existing congruency
        // so if the node is created, we have a complete list of cylinders linked to the node.

        // @todo
        // We could also check for congruencies at regular subdivisions of the each cylinder's
        // length, eg halfway, etc. For now, just create multiple cylinders.
        // Also, we show when joined cylinders were aligned at given angles eg 0 (ie collinear),
        // 45, 90, etc.

        // create a temp class for this as creating instances of CongruentPoint invokes the
        // 3D object init for the type of congruency
        data class TempCongruentPoint (val point : Geometry.Point, var congruency: Congruency, val fromTop: Boolean, val toTop: Boolean )
        val newCongruencies = mutableListOf<TempCongruentPoint>()

        // check cylinder.top vs other.bottom for any other cylinder,
        // and cylinder.top vs other.top for an unselected other cylinder.
        // If we are doing both checks (ie other is unselected), the checks don't
        // short-circuit; it's possible for one end to match both ends of the other cylinder
        // in different planes.
        if (element.top())
        {

            // Since we're going to check the list of new congruencies for duplicates against
            // existing congruencies anyway, there's no point in checking they're not NONE
            // at this point
            newCongruencies.add( TempCongruentPoint( cylinder.topCenter, cylinder.topCenter.findCongruency(other.bottomCenter),
                fromTop = true,
                toTop = false
            ) )
            if (!other.selected)
                newCongruencies.add( TempCongruentPoint( cylinder.topCenter, cylinder.topCenter.findCongruency(other.topCenter),
                    fromTop = true,
                    toTop = true
                ) )
        }

        // ditto for bottom
        if ( element.bottom() )
        {
            newCongruencies.add( TempCongruentPoint( cylinder.bottomCenter, cylinder.bottomCenter.findCongruency(other.topCenter),
                fromTop = false,
                toTop = true
            ) )
            if (!other.selected)
                newCongruencies.add( TempCongruentPoint( cylinder.bottomCenter, cylinder.bottomCenter.findCongruency(other.bottomCenter),
                    fromTop = false,
                    toTop = false
                ) )
        }

        // we now have either 1, 2 or 4 potential new congruencies; filter out NONEs
        // and check the others against the existing list
        for ( newC in newCongruencies.filter { nc -> nc.congruency != Congruency.NONE } )
        {
            // @todo
            //  doesn't work for group moves with full group checking; have to check the relevant parts of the point
            // as well; two separate cylinders can both have XY matches, for example.
            // However, for now we're only checking the actual touched cylinder for congruencies,
            // so this doesn't arise. One reason we're doing this is that it could get very crowded
            // visually if we're showing all congruencies for all group cylinders + all existing cylinders.
            // This way, the key cylinder can be selected for the move and only that one is displayed.
            //            val sameCongruencies = congruencies.filter { cp -> cp.congruency == newC.congruency }
            //            if (sameCongruencies.isNotEmpty())
            //            {
            //                // we need to check each existing congruency and see if the new one is really new.
            //                // For a plane congruency, say SAME_X, we'd check the X coords of the 2 congruency's points.
            //                // For a line congruent, say SAME_XY, we'd check both the X and Y coords.
            //                // For a full or point congruency, we'd check all 3, and it is valid to have 2
            //                // different such congruencies; imagine setting up 2 vertical cylinders, then 2
            //                // angled cylinders with the same spacing, and then moving both angled cylinders onto
            //                // the verticals.
            //            }

//            if ( newC.congruency === Congruency.SAME_POINT )
//            {
//                // check for existing nodes at the same point
//
//                // if we assume that all nodes begin with 2 points, since you can't move two
//                // ends onto the same point, there's a 50:50 chance that this congruency has
//                // already been added, by the other point. Check for that congruency.
//                // (Note that it's quite possible to have several nodes added in one move,
//                // if you move a group.)
//                if ( congruencies.firstOrNull { cp -> cp.congruency == Congruency.SAME_POINT && newC.from == cp.to } == null )
//                {
//
//                }
//            }
//            else if ( congruencies.firstOrNull { cp -> cp.congruency == newC.congruency } == null )

            // if we've moved onto a point, check if that point is part of a node
            var otherId = other.id
            if ( newC.congruency === Congruency.SAME_POINT && ( ( newC.toTop && other.topNode != null ) || ( !newC.toTop && other.bottomNode != null )) )
            {
                // change the congruency type and assign the node id as the target
                newC.congruency = Congruency.EXISTING_NODE
                otherId = if ( newC.toTop ) other.topNode!! else other.bottomNode!!
                model.getNodeById( otherId ).highlighted = true
            }

            congruencies.add( CongruentPoint( newC.point, newC.congruency, cylinder.id, newC.fromTop, otherId, newC.toTop )  )
        }

    }

    fun handleDragMove(deltaX: Float, deltaY: Float, normalizedX: Float, normalizedY: Float) {

        if ( state == State.PANNING ) {
            xRotation += deltaX / 16f;
            yRotation += deltaY / 16f;
            if (yRotation < -90)
                yRotation = -90f;
            else if (yRotation > 90)
                yRotation = 90f;
            updateViewMatrices()
        }
        else if ( state == State.MOVE )
        {

            // find intersection of current position with current mesh
            val ray = Geometry.convertNormalized2DPointToRay(normalizedX, normalizedY, invertedViewProjectionMatrix)
            val meshPoint = mesh!!.findIntersectionPoint(ray)
            if (meshPoint != null) {

                // if we're snapping points, we have to find out how the handle moves,
                // and then apply the offset to the other cylinders in the group.
                val newHandle = mesh!!.nearestSnapPoint( meshPoint )
                val delta = newHandle!!.subtract( snapHandle!! ).asVector()

                if ( delta.length() > 0f )
                {
                    if ( touchedObject!!.type == TouchableObjectType.CYLINDER )
                    {
                        val element = touchedObject!!.itemTouch!!.element

                        // apply the offsets to all other selected objects.
                        for (cylinder in model.selectedCylinders() ) {
                            cylinder.changePosition( if (element.top()) delta else null, if (element.bottom()) delta else null )
                        }

                        // check for congruencies, ie shared points/lines/planes
                        congruencies.clear()
                        for (node in model.nodes())
                            node.highlighted = false
                        for (cylinder in model.selectedCylinders() ) {

                            // check against all other cylinders
                            for (other in model.cylinders().filter { o -> o !== cylinder } ) {

                                // only check the moving end; both ends move when selection == BODY
                                checkForCongruency( element, cylinder, other )

                            }
                        }
                    }
                    else if ( touchedObject!!.type == TouchableObjectType.NODE )
                    {
                        // apply the offsets to all other selected objects.
                        for (node in model.selectedNodes() ) {
                            node.changePosition( delta )
                            for ( id in node.cylinderIds )
                                model.getCylinderById( id ).changePositionByNode( delta, node.id )
                        }
                    }
                }
                snapHandle = newHandle
            }

        }
    }

    fun handleDragEnd(normalizedX: Float, normalizedY: Float) {

        mesh = null
        state = preDragState
        for ( point in congruencies.filter { it.congruency == Congruency.EXISTING_NODE } ) {
            val node = model.getNodeById( point.to )
            node.addCylinder( point.from )
            model.getCylinderById( point.from ).setNode( node, point.fromTop)
            Log.d( TAG, "Join to node $node from ${point.from}/${point.fromTop} to Node ${point.to} ")
        }
        for ( point in congruencies.filter { it.congruency == Congruency.SAME_POINT } ) {
            val node = Node( point.point, NODE_RADIUS, point.from, point.to )
            model.addNode( node )
            model.getCylinderById( point.from ).setNode( node, point.fromTop)
            model.getCylinderById( point.to ).setNode( node, point.toTop)
            Log.d( TAG, "Create node $node from ${point.from}/${point.fromTop} to ${point.to}/${point.toTop} ")
        }
        congruencies.clear()
        for (node in model.nodes())
            node.highlighted = false

    }

    fun handleLongPress() {
        if ( previousTouch == PreviousTouch.ITEM ) {
            if ( state == State.GROUP && touchedObject!!.itemTouch!!.item.selected ) {
                touchedObject!!.itemTouch!!.item.selected = false
                val count = model.selectedItems().size
                if ( count == 0 )
                    state = State.SELECT
            }
            else if ( state == State.SINGLE || state == State.SELECT || state == State.GROUP ) {
                touchedObject!!.itemTouch!!.item.selected = true
                state = State.GROUP
            }
            mesh = null
        }
        else if ( previousTouch == PreviousTouch.NOTHING )
        {
            Log.d( TAG, "Nothing long")
            model.clear()
        }
    }

}

/*
TODO

+ UI changes;
on screen buttons for undo, settings, save/load etc
need to be able to undo & redo, leave nodes, delete nodes & cylinders
save, load, clear models
automatic joining of group selections
    For groups > 3, find closest neighbour
    cylinders - top/top, bottom/bottom, alternating.

 */