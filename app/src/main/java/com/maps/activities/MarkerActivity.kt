package com.maps.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_marker.*
import java.io.*
import java.nio.ByteBuffer
import kotlin.collections.ArrayList


open class MarkerActivity : AppCompatActivity() {
    private lateinit var cameraId: String
    private lateinit var mCamera: CameraDevice
    private lateinit var takePictureButton: Button
    private lateinit var cameraPreview: TextureView

    private val orientations = mapOf(
        Surface.ROTATION_0 to 90,
        Surface.ROTATION_0 to 0,
        Surface.ROTATION_180 to 270,
        Surface.ROTATION_270 to 180
    )
    protected var cameraDevice: CameraDevice? = null
    protected var cameraCaptureSessions: CameraCaptureSession? = null
    protected var captureRequest: CaptureRequest? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private val file: File? = null
    private val REQUEST_CAMERA_PERMISSION = 200
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        cameraPreview = findViewById(R.id.cameraPreviewTextureView)
        cameraPreview.surfaceTextureListener =
            TextureListener { openCamera() }

        takePictureButton = findViewById(R.id.buttonCapture)
        takePictureButton.setOnClickListener { takePicture() }

        this.textView.text = intent.getStringExtra("com.maps.maps.NAME")
    }

    class TextureListener(val openCamera: () -> Unit) : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) {
            openCamera()
        }
    }

    private val stateCallback = object:CameraDevice.StateCallback() {
        override fun onOpened(camera:CameraDevice) {
            //This is called when the camera is open
            Log.i(R.string.log_tag.toString(), "onOpened")
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera:CameraDevice) {
            cameraDevice?.close()
        }
        override fun onError(camera:CameraDevice, error:Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    val captureCallbackListener: CaptureCallback = object: CaptureCallback() {
        override fun onCaptureCompleted(session:CameraCaptureSession, request:CaptureRequest, result:TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            Toast.makeText(this@MarkerActivity, "Saved:$file", Toast.LENGTH_SHORT).show()
            createCameraPreview()
        }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try
        {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        }
        catch (e:InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun takePicture() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(this.mCamera.id)
            val jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?.getOutputSizes(ImageFormat.JPEG)
            var width = 640
            var height = 480
            if (jpegSizes != null && jpegSizes.isNotEmpty()) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }
            val reader: ImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces: MutableList<Surface> = ArrayList<Surface>(2)
            outputSurfaces.add(reader.surface)
            if (cameraPreviewTextureView != null) {
                outputSurfaces.add(Surface(cameraPreviewTextureView.surfaceTexture))
            }
            val captureBuilder: CaptureRequest.Builder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            // Orientation
            val rotation = windowManager.defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientations.get(rotation))

            val file = File(Environment.getExternalStorageState().toString() + "/pic.jpg")
            val readerListener: OnImageAvailableListener = object : OnImageAvailableListener {
                override fun onImageAvailable(reader: ImageReader) {
                    var image: Image? = null
                    try {
                        image = reader.acquireLatestImage()
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)
                        save(bytes)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        image?.close()
                    }
                }

                @Throws(IOException::class)
                private fun save(bytes: ByteArray) {
                    var output: OutputStream? = null
                    try {
                        output = FileOutputStream(file)
                        output.write(bytes)
                    } finally {
                        output?.close()
                    }
                }
            }
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
            val captureListener: CaptureCallback = object : CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    Toast.makeText(this@MarkerActivity, "Saved:$file", Toast.LENGTH_SHORT).show()
                    createCameraPreview()
                }
            }
            cameraDevice!!.createCaptureSession(
                outputSurfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.capture(
                                captureBuilder.build(),
                                captureListener,
                                mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    protected fun createCameraPreview() {
        try {
            val texture: SurfaceTexture = cameraPreviewTextureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)
            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) { //The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }
                        // When the session is ready, we start displaying the preview.
                        cameraCaptureSessions = cameraCaptureSession
                        updatePreview()
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(
                            this@MarkerActivity,
                            "Configuration change",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    protected fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.e(R.string.log_tag.toString(), "is camera open")
        try {
            cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MarkerActivity,
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        Log.e(R.string.log_tag.toString(), "openCamera X")
    }

    protected fun updatePreview() {
        if (null == cameraDevice) {
            Log.e(R.string.log_tag.toString(), "updatePreview error, return")
        }
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        captureRequestBuilder?.build()?.let {
            cameraCaptureSessions?.setRepeatingRequest(
                it,
                null,
                mBackgroundHandler
            )
        }
    }

    private fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) { // close the app
                Toast.makeText(
                    this@MarkerActivity,
                    "Sorry!!!, you can't use this app without granting permission",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e(R.string.log_tag.toString(), "onResume")
        startBackgroundThread()
        if (cameraPreviewTextureView.isAvailable) {
            openCamera()
        } else {
            cameraPreviewTextureView.surfaceTextureListener =
                TextureListener { openCamera() }
        }
    }

    override fun onPause() {
        Log.e(R.string.log_tag.toString(), "onPause")
        //closeCamera();
        stopBackgroundThread()
        super.onPause()
    }
}