package com.example.webviewapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    // File chooser callback jo camera/gallery se photo choose hone par call hota hai
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 100
        private const val REQUEST_FILE_CHOOSER_CODE = 200
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        askPermissionsIfNeeded()
        setupWebView()

        val url = getString(R.string.website_url)
        webView.loadUrl(url)
    }

    private fun askPermissionsIfNeeded() {
        val notGranted = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), REQUEST_PERMISSIONS_CODE)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setSupportMultipleWindows(false)
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Normal page navigation isi WebView ke andar rakhne ke liye
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // false = WebView khud handle karega
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = android.view.View.GONE
                injectShareBridge()
            }
        }

        // Camera permission grant karne aur file input (<input type=file>) handle karne ke liye
        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.visibility = android.view.View.VISIBLE
                progressBar.progress = newProgress
                if (newProgress == 100) progressBar.visibility = android.view.View.GONE
            }

            // Jab website JS se camera/mic maange (getUserMedia)
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    request.grant(request.resources)
                }
            }

            // Jab website <input type="file" capture> jaisa field kholay
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback

                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                var cameraIntentAvailable = false
                if (takePictureIntent.resolveActivity(packageManager) != null) {
                    val photoFile = createImageFile()
                    photoFile?.let {
                        cameraImageUri = FileProvider.getUriForFile(
                            this@MainActivity,
                            "${packageName}.fileprovider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                        cameraIntentAvailable = true
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }

                val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
                    putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    putExtra(Intent.EXTRA_TITLE, "Photo chuno ya kheencho")
                    if (cameraIntentAvailable) {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))
                    }
                }

                startActivityForResult(chooserIntent, REQUEST_FILE_CHOOSER_CODE)
                return true
            }
        }

        // Website se koi bhi file download hone par (PDF, image, etc.)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("User-Agent", userAgent)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    setAllowedOverMeteredNetworks(true)
                    setAllowedOverRoaming(true)
                }
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, "Download shuru: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Download fail ho gaya: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Native Android share ke liye JS bridge
        webView.addJavascriptInterface(ShareBridge(), "AndroidNative")
    }

    // Web page load hone ke baad navigator.share() ko native share se jodta hai
    private fun injectShareBridge() {
        val js = """
            (function() {
                if (window.AndroidNative) {
                    navigator.share = function(data) {
                        return new Promise(function(resolve) {
                            window.AndroidNative.share(
                                (data && data.title) || '',
                                (data && data.text) || '',
                                (data && data.url) || ''
                            );
                            resolve();
                        });
                    };
                    navigator.canShare = function() { return true; };
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // Website ke JS se call hota hai: AndroidNative.share(title, text, url)
    inner class ShareBridge {
        @JavascriptInterface
        fun share(title: String, text: String, url: String) {
            runOnUiThread {
                val shareText = listOf(text, url).filter { it.isNotBlank() }.joinToString("\n")
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, shareText.ifBlank { title })
                }
                startActivity(Intent.createChooser(sendIntent, "Share karo"))
            }
        }
    }

    private fun createImageFile(): File? {
        return try {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("IMG_", ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_FILE_CHOOSER_CODE) {
            var results: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK) {
                if (data == null || data.data == null) {
                    // Camera se photo kheenchi gayi thi
                    cameraImageUri?.let { results = arrayOf(it) }
                } else {
                    // Gallery se file choose hui thi
                    data.data?.let { results = arrayOf(it) }
                }
            }
            fileChooserCallback?.onReceiveValue(results)
            fileChooserCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
