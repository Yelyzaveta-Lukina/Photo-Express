package edu.lukina.photoexpress

/**
 * Created by Yelyzaveta Lukina on 11.27.2025.
 */

// Imports the ContentValues class, used to store a set of values that the ContentResolver can process.
import android.content.ContentValues
// Imports the Bitmap class, which represents a bitmap image.
import android.graphics.Bitmap
// Imports the BitmapFactory class, which creates Bitmap objects from various sources.
import android.graphics.BitmapFactory
// Imports the Canvas class, which is used for drawing 2D graphics.
import android.graphics.Canvas
// Imports the Color class, which handles color-related operations.
import android.graphics.Color
// Imports a color filter that can be used to simulate lighting effects.
import android.graphics.LightingColorFilter
// Imports the Paint class, which holds style and color information for drawing.
import android.graphics.Paint
// Imports the Build class, providing information about the current device build.
import android.os.Build
// Imports the Bundle class, used for passing data and saving instance state.
import android.os.Bundle
// Imports the Environment class, which provides access to environment variables.
import android.os.Environment
// Imports the MediaStore class, providing access to the device's media library.
import android.provider.MediaStore
// Imports the View class, the basic building block for UI components.
import android.view.View
// Imports the Button widget class.
import android.widget.Button
// Imports the ImageView widget class, used to display images.
import android.widget.ImageView
// Imports the SeekBar widget class, a draggable bar for selecting a value.
import android.widget.SeekBar
// Imports the listener interface for SeekBar changes.
import android.widget.SeekBar.OnSeekBarChangeListener
// Imports the Toast class for showing short, non-disruptive messages to the user.
import android.widget.Toast
// Imports a contract for taking a picture with the camera and receiving the result.
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
// Imports AppCompatActivity, a base class for activities using support library features.
import androidx.appcompat.app.AppCompatActivity
// Imports the FileProvider class for securely sharing files between apps.
import androidx.core.content.FileProvider
// Imports Kotlin Coroutines components for managing asynchronous tasks.
import kotlinx.coroutines.CoroutineScope
// Imports the Dispatchers object to specify the thread for coroutine execution.
import kotlinx.coroutines.Dispatchers
// Imports the launch coroutine builder.
import kotlinx.coroutines.launch
// Imports the withContext function to switch coroutine contexts.
import kotlinx.coroutines.withContext
// Imports the File class for file and directory pathnames.
import java.io.File
// Imports a class for formatting and parsing dates.
import java.text.SimpleDateFormat
// Imports the Date class, representing a specific instant in time.
import java.util.Date
// Imports the Locale class, for language and regional conventions.
import java.util.Locale

// Declares the MainActivity class, inheriting from AppCompatActivity.
class MainActivity : AppCompatActivity() {

    // Declares a private, nullable property to hold the file where the photo will be stored.
    private var photoFile: File? = null
    // Declares a private, late-initialized property to hold the ImageView for displaying the photo.
    private lateinit var photoImageView: ImageView
    // Declares a private, late-initialized property to hold the SeekBar for adjusting brightness.
    private lateinit var brightnessSeekBar: SeekBar
    // Declares a private, late-initialized property to hold the Save button.
    private lateinit var saveButton: Button

    // Declares a private property for the multiplication component of the brightness color filter.
    private var multColor = -0x1
    // Declares a private property for the additive component of the brightness color filter.
    private var addColor = 0

    // Overrides the onCreate method, which is called when the activity is first created.
    override fun onCreate(savedInstanceState: Bundle?) {
        // Calls the superclass's implementation of onCreate.
        super.onCreate(savedInstanceState)
        // Sets the user interface layout for this activity from the specified XML file.
        setContentView(R.layout.activity_main)

        // Finds and assigns the ImageView from the layout.
        photoImageView = findViewById(R.id.photo)

        // Finds and assigns the Save button from the layout.
        saveButton = findViewById(R.id.save_button)
        // Sets a click listener on the Save button to call the 'savePhotoClick' method.
        saveButton.setOnClickListener { savePhotoClick() }
        // Disables the Save button initially, as there is no photo to save.
        saveButton.isEnabled = false

        // Finds the "Take Photo" button and sets a click listener to call the 'takePhotoClick' method.
        findViewById<Button>(R.id.take_photo_button).setOnClickListener { takePhotoClick() }

        // Finds and assigns the brightness SeekBar from the layout.
        brightnessSeekBar = findViewById(R.id.brightness_seek_bar)
        // Hides the brightness SeekBar initially.
        brightnessSeekBar.visibility = View.INVISIBLE

        // Sets a listener to handle changes to the SeekBar's progress.
        brightnessSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            // Overrides the method called when the SeekBar's progress value changes.
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Calls the function to apply the new brightness level to the photo.
                changeBrightness(progress)
            }

            // Overrides the method called when the user starts touching the SeekBar. (No action needed here)
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            // Overrides the method called when the user stops touching the SeekBar. (No action needed here)
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    // Defines a private function to handle the click of the "Take Photo" button.
    private fun takePhotoClick() {

        // Calls the function to create a unique file to save the new photo.
        photoFile = createImageFile()

        // Creates a content URI for the photo file, required to grant the camera app write permissions.
        val photoUri = FileProvider.getUriForFile(
            // Provides the application context.
            this,
            // Specifies the authority of the FileProvider, defined in the AndroidManifest.
            "edu.lukina.photoexpress.fileprovider",
            // The file for which to get a URI. The '!!' asserts it's not null.
            photoFile!!
        )

        // Launches the camera app, passing the URI where the photo should be saved.
        takePicture.launch(photoUri)
    }

    // Registers a callback for the result of the TakePicture activity contract.
    private val takePicture = registerForActivityResult(
        // Specifies the contract type as taking a picture.
        TakePicture()
        // A lambda that is executed when the camera app returns a result.
    ) { success ->
        // Checks if the photo was successfully taken and saved.
        if (success) {
            // Calls the function to display the captured photo in the ImageView.
            displayPhoto()
            // Resets the brightness SeekBar to the middle (neutral) position.
            brightnessSeekBar.progress = 100
            // Makes the brightness SeekBar visible to the user.
            brightnessSeekBar.visibility = View.VISIBLE
            // Applies the initial brightness setting to the new photo.
            changeBrightness(brightnessSeekBar.progress)
            // Enables the Save button since there is now a photo.
            saveButton.isEnabled = true
        }
    }

    // Defines a private function to create a new, uniquely named image file.
    private fun createImageFile(): File {

        // Creates a unique filename using a timestamp.
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        // Constructs the full filename for the JPEG image.
        val imageFilename = "photo_$timeStamp.jpg"

        // Gets the app-specific directory for storing pictures.
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // Creates and returns a File object representing the new image file in the storage directory.
        return File(storageDir, imageFilename)
    }

    // Defines a private function to decode and display the photo in the ImageView.
    private fun displayPhoto() {
        // Gets the current width of the ImageView.
        val targetWidth = photoImageView.width
        // Gets the current height of the ImageView.
        val targetHeight = photoImageView.height

        // Creates a BitmapFactory.Options object to configure bitmap decoding.
        val bmOptions = BitmapFactory.Options()
        // Sets inJustDecodeBounds to true to read image dimensions without loading it into memory.
        bmOptions.inJustDecodeBounds = true
        // Decodes the file to get the image's original dimensions.
        BitmapFactory.decodeFile(photoFile!!.absolutePath, bmOptions)
        // Gets the original width of the photo.
        val photoWidth = bmOptions.outWidth
        // Gets the original height of the photo.
        val photoHeight = bmOptions.outHeight

        // Calculates the scaling factor to shrink the image while maintaining aspect ratio.
        val scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight)

        // Resets inJustDecodeBounds to false to actually load the image.
        bmOptions.inJustDecodeBounds = false
        // Sets the sample size, which decodes a smaller version of the image to save memory.
        bmOptions.inSampleSize = scaleFactor
        // Decodes the image file into a Bitmap object at the calculated smaller size.
        val bitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath, bmOptions)

        // Sets the decoded, smaller bitmap to be displayed in the ImageView.
        photoImageView.setImageBitmap(bitmap)
    }

    // Defines a private function to change the photo's brightness based on the SeekBar's progress.
    private fun changeBrightness(brightness: Int) {
        // A brightness of 100 is considered the neutral middle value.
        if (brightness > 100) {
            // If brightness is over 100, we add color to make it brighter.
            // Calculates a multiplier based on how far past 100 the progress is.
            val addMult = brightness / 100f - 1
            // Calculates the additive color component for the filter.
            addColor = Color.argb(
                255, (255 * addMult).toInt(), (255 * addMult).toInt(),
                (255 * addMult).toInt()
            )
            // Resets the multiplicative color component to neutral (no change).
            multColor = -0x1
        } else {
            // If brightness is 100 or less, we scale the color down to make it darker.
            // Calculates a multiplier based on the progress percentage.
            val brightMult = brightness / 100f
            // Calculates the multiplicative color component to darken the image.
            multColor = Color.argb(
                255, (255 * brightMult).toInt(), (255 * brightMult).toInt(),
                (255 * brightMult).toInt()
            )
            // Resets the additive color component to neutral (no change).
            addColor = 0
        }

        // Creates a new color filter with the calculated multiplicative and additive colors.
        val colorFilter = LightingColorFilter(multColor, addColor)
        // Applies the color filter to the ImageView to change the displayed photo's brightness.
        photoImageView.colorFilter = colorFilter
    }

    // Defines a private function to handle the click of the Save button.
    private fun savePhotoClick() {
        // Disables the Save button to prevent multiple clicks while saving.
        saveButton.isEnabled = false

        // Checks if there is a valid photo file to save.
        if (photoFile != null) {

            // Launches a coroutine on the Main thread to handle the save operation without blocking the UI.
            CoroutineScope(Dispatchers.Main).launch {
                // Calls the suspending function to save the photo with the applied brightness filter.
                saveAlteredPhoto(photoFile!!, multColor, addColor)

                // Shows a "Photo saved" message to the user.
                Toast.makeText(applicationContext, R.string.photo_saved, Toast.LENGTH_LONG).show()

                // Re-enables the Save button now that the operation is complete.
                saveButton.isEnabled = true
            }
        }
    }

    // Defines a private suspending function to save the altered photo. It's marked to run on the IO thread.
    private suspend fun saveAlteredPhoto(photoFile: File, filterMultColor: Int,
                                         filterAddColor: Int) = withContext(Dispatchers.IO) {
        // Reads the original, full-resolution image from the file.
        val origBitmap = BitmapFactory.decodeFile(photoFile.absolutePath, null)

        // Creates a new, blank bitmap with the same dimensions and configuration as the original.
        val alteredBitmap = Bitmap.createBitmap(
            origBitmap.width,
            origBitmap.height,
            origBitmap.config ?: Bitmap.Config.ARGB_8888  )

        // Creates a Canvas to draw on the new, blank bitmap.
        val canvas = Canvas(alteredBitmap)
        // Creates a Paint object to hold the drawing configuration.
        val paint = Paint()
        // Creates a color filter with the current brightness settings.
        val colorFilter = LightingColorFilter(filterMultColor, filterAddColor)
        // Applies the color filter to the paint object.
        paint.colorFilter = colorFilter
        // Draws the original bitmap onto the new canvas, applying the paint's color filter in the process.
        canvas.drawBitmap(origBitmap, 0f, 0f, paint)

        // Creates a ContentValues object to hold metadata for the MediaStore.
        val imageValues = ContentValues()
        // Sets the display name for the image in the media gallery.
        imageValues.put(MediaStore.MediaColumns.DISPLAY_NAME, photoFile.name)
        // Sets the MIME type for the image.
        imageValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

        // Checks if the Android version is Q (API 29) or higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For modern Android versions, specifies the relative path
            // for saving in the public Pictures directory.
            imageValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        // Gets an instance of the ContentResolver to interact with the MediaStore.
        val resolver = this@MainActivity.applicationContext.contentResolver
        // Inserts a new entry into the device's external image gallery and gets its URI.
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)

        // Safely unwraps the nullable URI with a 'let' block.
        uri?.let {
            // Executes the following code, catching any exceptions that might occur during the file stream operation.
            runCatching {
                // Opens an output stream to the new MediaStore entry's URI.
                resolver.openOutputStream(it)?.use { outStream ->
                    // Compresses and writes the altered bitmap to the output stream as a JPEG with 100% quality.
                    alteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                }
            }
        }
    }
}


