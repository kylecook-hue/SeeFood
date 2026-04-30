package com.seefood.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.seefood.app.databinding.ActivityMainBinding
import com.seefood.app.ui.ResultAnimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var apiClient: AnthropicApiClient

    private var cameraOutputUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraOutputUri?.let { processImageUri(it) }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImageUri(it) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else showSnackbar(getString(R.string.error_permission_camera))
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchGallery()
        else showSnackbar(getString(R.string.error_permission_storage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.getString(KEY_CAMERA_URI)?.let {
            cameraOutputUri = Uri.parse(it)
        }

        apiClient = AnthropicApiClient(BuildConfig.ANTHROPIC_API_KEY)

        binding.btnTakePhoto.setOnClickListener { checkCameraPermissionAndLaunch() }
        binding.btnChooseGallery.setOnClickListener { checkStoragePermissionAndLaunch() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cameraOutputUri?.let { outState.putString(KEY_CAMERA_URI, it.toString()) }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showSnackbar(getString(R.string.permission_camera_rationale))
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkStoragePermissionAndLaunch() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED -> launchGallery()
            shouldShowRequestPermissionRationale(permission) -> {
                showSnackbar(getString(R.string.permission_storage_rationale))
                storagePermissionLauncher.launch(permission)
            }
            else -> storagePermissionLauncher.launch(permission)
        }
    }

    private fun launchCamera() {
        val uri = ImageUtils.createCameraOutputUri(this)
        cameraOutputUri = uri
        cameraLauncher.launch(uri)
    }

    private fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun processImageUri(uri: Uri) {
        binding.ivPlaceholder.visibility = View.GONE
        binding.ivSelectedImage.visibility = View.VISIBLE
        binding.ivSelectedImage.load(uri) { crossfade(true) }
        binding.layoutResult.visibility = View.GONE
        binding.tvStatusMessage.visibility = View.GONE

        showLoadingState()

        lifecycleScope.launch {
            val bitmap: Bitmap? = withContext(Dispatchers.IO) {
                ImageUtils.uriToBitmap(this@MainActivity, uri)
            }
            if (bitmap == null) {
                showError("Failed to load image. Please try another.")
                return@launch
            }
            val base64 = withContext(Dispatchers.Default) {
                ImageUtils.bitmapToBase64(bitmap)
            }
            handleResult(apiClient.classifyImage(base64))
        }
    }

    private fun handleResult(result: ClassificationResult) {
        hideLoadingState()
        when (result) {
            is ClassificationResult.Hotdog -> {
                binding.layoutResult.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.hotdog_green)
                )
                binding.tvResult.text = getString(R.string.result_hotdog)
                binding.tvResultSubtitle.text = getString(R.string.result_hotdog_subtitle)
                ResultAnimator.popIn(binding.layoutResult)
                ResultAnimator.pulse(binding.cardImagePreview)
            }
            is ClassificationResult.NotHotdog -> {
                binding.layoutResult.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.not_hotdog_red)
                )
                binding.tvResult.text = getString(R.string.result_not_hotdog)
                binding.tvResultSubtitle.text = getString(R.string.result_not_hotdog_subtitle)
                ResultAnimator.popIn(binding.layoutResult)
                ResultAnimator.shake(binding.cardImagePreview)
            }
            is ClassificationResult.Error -> showError(result.message)
        }
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnTakePhoto.isEnabled = false
        binding.btnChooseGallery.isEnabled = false
    }

    private fun hideLoadingState() {
        binding.progressBar.visibility = View.GONE
        binding.btnTakePhoto.isEnabled = true
        binding.btnChooseGallery.isEnabled = true
    }

    private fun showError(message: String) {
        binding.tvStatusMessage.text = message
        binding.tvStatusMessage.visibility = View.VISIBLE
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val KEY_CAMERA_URI = "camera_output_uri"
    }
}
