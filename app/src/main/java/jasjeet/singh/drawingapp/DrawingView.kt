package jasjeet.singh.drawingapp

import android.content.Context
import android.graphics.*
import android.os.Parcelable
// This means that when we use more than 2-3 classes of the library,
// it imports the whole library and is displayed but "android.library.*"
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


/**Reference:

[Paint Class](https://developer.android.com/reference/kotlin/android/graphics/Paint)

[Custom Drawing](https://developer.android.com/training/custom-views/custom-drawing)*/
class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mDrawPath: CustomPath? = null

    /**
     *  Bitmap, method by which a display space (such as a graphics image file) is defined, including
     *  the colour of each of its pixels (or bits). In effect, a bitmap is an array of binary data
     *  representing the values of pixels in an image or display.
     */
    private var mCanvasBitmap: Bitmap? = null

    /**
    * The Paint class holds the style and color information about how to draw geometries, text and
    * bitmaps.
    */
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null

    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK

    /**
     * The Canvas class holds the "draw" calls. To draw something, you need 4 basic components:
     * A Bitmap to hold the pixels, a Canvas to host the draw calls (writing into the bitmap), a
     * drawing primitive (e.g. Rect, Path, text, Bitmap), and a paint (to describe the colors and
     * styles for the drawing).
     */
    private var canvas: Canvas? = null
    private var mPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }
    
    /*override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        if (mPaths.isNotEmpty()) {
            draw(this.canvas)
        }
    }*/

    private fun setUpDrawing(){
        mDrawPath = CustomPath(color,mBrushSize)
        mDrawPaint = Paint()
        // Paint(): Creates a new paint class with default settings.

        mDrawPaint!!.color = color
        // We add non-null asserted call because we know mDrawPaint is initialized.

        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND

        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        /*
           Paint flag that enables dithering when blitting.
           Enabling this flag applies a dither to any blit operation where the target's colour space
           is more constrained than the source.
          
           BLIT:
           To "blit" is to copy bits from one part of a computer's graphical memory to another part.
           This technique deals directly with the pixels of an image, and draws them directly to the
           screen, which makes it a very fast rendering technique that's often perfect for
           fast-paced 2D action games.
          
           Dithering is the attempt by a computer program to approximate a color from a mixture of
           other colors when the required color is not available. For example, dithering occurs when
           a color is specified for a Web page that a browser on a particular operating system
           can't support.
         */
    }

    /**
     * [onSizeChanged]:
     *
     * This is called during layout when the size of this view has changed. If you were just added
     * to the view hierarchy, you're called with the old values of 0.
     * It is called whenever the View's size changes, included when the View is first added to the
     * [View] hierarchy as the layout is inflated.
     *
     * Here, onSizeChanged will be called when we set the view to [DrawingView] when [MainActivity]
     * is executed; i.e., onCreate function is executed
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        /*
        * A bitmap configuration describes how pixels are stored.
        * This affects the quality (color depth) as well as
        * the ability to display transparent/translucent colors.
        */
        // Read Documentation of ARGB_8888

        // Setting our Canvas
        canvas = Canvas(mCanvasBitmap!!)
    }

    // Change Canvas to Canvas? (in parameters) if fails
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (path in mPaths){
            mDrawPaint!!.color = path.color
            /*
            Because our color can be different for every path and it is our app's feature to be
            able to change color.
            */
            mDrawPaint!!.strokeWidth = path.brushThickness
            // Same for this as its also a mutable property in terms of our app features.
            
            canvas.drawPath(path, mDrawPaint!!)
        }
        
        // canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        // Doubt
        
        if ( !(mDrawPath!!.isEmpty) ) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

/**
 [onTouchEvent] :

This method to handle touch screen motion events.
If this method is used to detect click actions, it is recommended that the actions be performed by
implementing and calling [performClick].

[MotionEvent] :

Motion events describe movements in terms of an action code and a set of axis values. The action
code specifies the state change that occurred such as a pointer going down or up. The axis values
describe the position and other movement properties.
 
 */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{     // When we touch the screen
                mDrawPath!!.color = color
                // Now we will be setting the thickness of the path
                // and not the paint.
                mDrawPath!!.brushThickness = mBrushSize
                mDrawPath!!.reset()
                if (touchX != null && touchY != null) {
                    mDrawPath!!.moveTo(touchX, touchY)
                }

            }
            MotionEvent.ACTION_MOVE ->{     // When we move our finger while touching the screen
                if (touchX != null && touchY != null) {
                    mDrawPath!!.lineTo(touchX, touchY)
                }

            }
            MotionEvent.ACTION_UP ->{       // When we lift our finger from the screen
                mPaths.add(mDrawPath!!)     // Stores our Drawn Path
                mDrawPath = CustomPath(color,mBrushSize)
                /*  If we don't write the above statement, our path will be stored for sure, but
                    when we draw again, that same path will be edited again; i.e., our mPaths array
                    list will only have one entry all the time being edited again and again.        */
            }
            else ->{
                return false
            }
        }
    
        invalidate()
    /*[invalidate] :
     *
     * Generally, [invalidate] means 'redraw on screen' and results to a call of the view's [onDraw]
     * method. So if something changes and it needs to be reflected on screen, you need to call
     * [invalidate]. However, for built-in widgets you rarely, if ever, need to call it yourself.
     * When you change the state of a widget, internal code will call [invalidate] as necessary and
     * your change will be reflected on screen. For example, if you call [TextView.setText], after
     * doing a lot of internal processing (will the text fit on screen, does it need to be ellipsed,
     * etc.), TextView will call [invalidate] before [setText] returns.
     * Similarly for other widgets.
     *
     * If you implement a custom view, you will need to call [invalidate] whenever the backing model
     * changes and you need to redraw your view. It can also be used to create simple animations,
     * where you change state, then call [invalidate], change state again, etc.
     */
    
        return true
    // Returns: True if the event was handled, false otherwise.
    }
    
    
    fun setBrushSize(newSize: Float){
        /*
        We can't just assign newSize to mBrushSize because we need to take the dimensions of the
        the screen into consideration.
        */
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize, resources.displayMetrics
        )
        mDrawPaint!!.strokeWidth = mBrushSize
    }
    
    fun setColor(newColor: String){
        color = Color.parseColor(newColor)      // Converts Hex-code to a color.
        mDrawPaint!!.color = color
    }
    
    fun undo(){
        if (mPaths.isNotEmpty()) {
            mPaths.removeLast()
            invalidate()
        }
    }
    
    fun eraser(){
        mPaths.removeAll(mPaths.toSet())
        invalidate()
    }

    /**
     * [CustomPath] is a derivative class of [Path].
     *
     * [Path] :
     *
     * The Path class encapsulates compound (multiple contour) geometric paths consisting of straight
     * line segments, quadratic curves, and cubic curves. It can be drawn with
     * [canvas.drawPath(path, paint)], either filled or stroked (based on the paint's Style), or it
     * can be used for clipping or to draw text on a path.
     * */
    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path()
    // This class is stores our color and strokeWidth as well as giving us functionality of Path() class.


}