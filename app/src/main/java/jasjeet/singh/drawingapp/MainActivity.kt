package jasjeet.singh.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PermissionResult
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.security.Permission


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var ibBrushSize :ImageButton? = null
    private var ibUndo: ImageButton? = null
    private var ibGallery: ImageButton? = null
    
    // Variables for save functionality
    private var ibSave: ImageButton? = null
    private lateinit var progressDialog: Dialog
    
    // Variables for paint functionality
    private var mImageButtonCurrentPaint: ImageButton? = null
    
    private val openGalleryLauncher : ActivityResultLauncher<Intent> =
            registerForActivityResult( ActivityResultContracts.StartActivityForResult() ){
                result ->
                if( result.resultCode == RESULT_OK && result.data != null){
                    val sivBackground : ShapeableImageView = findViewById(R.id.siv_background)
                    sivBackground.setImageURI(result.data?.data)
                }
            }
    
    // Permissions
    private val requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val isGranted = it.value
                if (!isGranted) {
                    Snackbar.make(ibGallery!!, "Access Denied", Snackbar.LENGTH_SHORT).show()
                }
            }
            
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
        ibBrushSize = findViewById(R.id.ib_BrushSize)
        ibUndo = findViewById(R.id.ib_undo)
        ibGallery = findViewById(R.id.ib_gallery)
        ibSave = findViewById(R.id.ib_save)
        
        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        
        // Initial settings
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
        drawingView?.setBrushSize(10.toFloat())
        
        // Brush Size Function
        ibBrushSize?.setOnClickListener{
            showBrushSizeChooserDialog()
        }
        
        // Undo Function
        ibUndo?.setOnClickListener{
            drawingView?.undo()
        }
        
        // Eraser Functionality
        ibUndo?.setOnLongClickListener {
            drawingView?.eraser()
            true
            // Sets the button to long Clickable.
        }
        
        // Save functionality
        ibSave?.setOnClickListener{
            val permission: String =
                if(Build.VERSION.SDK_INT < 29) {    // As SDK 29 and below require write permission.
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }else{
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            when{
                ContextCompat.checkSelfPermission(this@MainActivity,permission)
                        == PackageManager.PERMISSION_GRANTED
                -> {
                    progressDialog = Dialog(this).apply {
                        setContentView(R.layout.progress_dialog)
                        setCanceledOnTouchOutside(false)
                        setCancelable(false)
                        window?.setBackgroundDrawableResource(android.R.color.transparent)
                        show()
                    }
                    lifecycleScope.launch {
                        saveBitmapFile(getBitmapFromView(findViewById(R.id.fl_drawing_view_container)))
                    }
                }
                shouldShowRequestPermissionRationale(permission) -> showRationaleDialog()
                else -> requestPermission.launch( arrayOf(permission) )
            }
        }
        
        // Gallery Function
        ibGallery?.setOnClickListener{
            val permission : String = Manifest.permission.READ_EXTERNAL_STORAGE
            when{
                ContextCompat.checkSelfPermission(this@MainActivity,permission)
                        == PackageManager.PERMISSION_GRANTED
                -> {
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    // URI is path of the image inside external storage
                    openGalleryLauncher.launch(pickIntent)
                }
                shouldShowRequestPermissionRationale(permission) -> showRationaleDialog()
                else -> requestPermission.launch( arrayOf(permission) )
            }
        }
    }
    
    private fun showRationaleDialog() {
        val dialog = AlertDialog.Builder(this)
        with(dialog){
            setTitle("No Storage Access Granted")
            setMessage("Cannot access gallery due to permission restrictions from system.")
            setPositiveButton("Ask Again"){ _, _ ->
                requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
            setNegativeButton("Cancel"){dialog, _->
                dialog.dismiss()
            }
            setNeutralButton("Help"){_,_ ->
                val helpDialog = AlertDialog.Builder(this@MainActivity)
                with(helpDialog){
                    setTitle("Grant Access Manually")
                    setMessage("To grant access manually, Hold app icon > Click \"App info\" > Permissions")
                    setPositiveButton("OK"){helpDialog , _ ->
                        helpDialog.dismiss()
                    }
                    show()
                }
            }
            show()
        }
    }
    
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this).apply {
            //brushDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            setTitle("Brush size: ")
            setContentView(R.layout.diaglog_brush_size)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        //brushDialog.setCancelable(true)   -> Default setting
        
        val ibSmall = brushDialog.findViewById<ImageButton>(R.id.ib_smallBrush)
        val ibMedium = brushDialog.findViewById<ImageButton>(R.id.ib_mediumBrush)
        val ibLarge = brushDialog.findViewById<ImageButton>(R.id.ib_largeBrush)
    
        ibLarge.setOnClickListener {
            ibBrushSize?.setImageResource(R.drawable.large)
            drawingView?.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        }
        
        ibMedium.setOnClickListener {
            ibBrushSize?.setImageResource(R.drawable.medium)
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        
        ibSmall.setOnClickListener {
            ibBrushSize?.setImageResource(R.drawable.small)
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }
        
        brushDialog.show()
        
    }
    
    fun paintClicked(view: View) {
        // Cannot make paintClicked private aa then it cannot be shared to the xml where OnClick is called.
        if (view !== mImageButtonCurrentPaint){
            mImageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            val imageButton = view as ImageButton       // Store the image buttons reference in a variable.
            val colorTag = imageButton.tag.toString()   // here colorTag looks something like #FO8F3F
            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint = imageButton
        }
    }
    
    // Getting bitmap from view
    private fun getBitmapFromView(view: View) : Bitmap {
        val returnedBitmap = Bitmap.createBitmap(
            view.width,
            view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(returnedBitmap)
        // This has been created for when we save the image, the saved image should have blacked out
        // curved frame.
        val border = view.findViewById<ImageView>(R.id.iv_drawingView_outline).apply {
            setImageResource(R.color.black)
        }
        val bgDrawable = view.background
        
        //val bgDrawable = findViewById<ShapeableImageView>(R.id.siv_background)
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
                // This draw function basically means "draw into" the canvas.
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        // resetting border.
        border.setImageResource(R.drawable.drawing_view_outline)
        
        return returnedBitmap
    }
    
    // Saving bitmap to a predefined folder, here its is the directory pictures.
    private suspend fun saveBitmapFile(mBitmap: Bitmap?) {
        lateinit var result : String
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                // Things usually go wrong in these cases so we use try block.
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    
                    val f = File(
                        Environment.getExternalStorageDirectory().toString() + File.separator
                                + Environment.DIRECTORY_PICTURES + File.separator
                                + "DrawingApp_"
                                + System.currentTimeMillis() / 1000
                                + ".png")
    
                    /*val f = File(
                        ContextCompat.getExternalFilesDirs(this@MainActivity,Environment.DIRECTORY_PICTURES).toString()
                                + File.separator
                                + "DrawingApp_"
                                + System.currentTimeMillis() / 1000
                                + ".png")*/
                    
                    /*val f = File(
                        externalCacheDir?.absolutePath + File.separator
                                + "DrawingApp_"
                                + System.currentTimeMillis() / 1000
                                + ".png")*/
                    
                    val fo = FileOutputStream(f)        // Permission Error in legacy devices
                    fo.write(bytes.toByteArray())
                    fo.close()
                    
                    result = f.absolutePath
                    
                    runOnUiThread{
                        progressDialog.dismiss()
                        if (result.isNotEmpty()){
                            // Share file
                            shareFile(result)
                            
                            Toast.makeText(this@MainActivity,
                                "File save successfully : $result",
                                Toast.LENGTH_LONG
                            ).show()
                        }else{
                            Toast.makeText(this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    
                }catch (e: FileNotFoundException){
                    progressDialog.dismiss()
                    Log.e("NoFile", "File not Found.")
                    e.printStackTrace()
                }catch (e: Exception) {
                    progressDialog.dismiss()
                    Log.e("Exception", "Unknown Exception")
                    e.printStackTrace()
                }
            }
        }
        
    }
    
    /**For More about mime types :
     * [Mime Types](https://stackoverflow.com/questions/23385520/android-available-mime-types)
     */
    private fun shareFile(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null) { /*path*/ _ , uri ->
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"  // This is mime type
            }
            startActivity(Intent.createChooser(shareIntent, "title"))
        }
    }
}






