package com.acme.webviewfilechooseissue

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQ_CODE_CHOOSER = 1
    }

    val unencodedHtml = "<input type=file>"

    private val webClient = object : WebViewClient() {

        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, err: WebResourceError?) {
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        }

        override fun onPageFinished(view: WebView?, url: String?) {
        }

        @Suppress("DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
    }

    var webViewFileChooseCallback: ValueCallback<Array<Uri>>? = null
    private val chromeClient = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            request.grant(request.resources)
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            if (filePathCallback == null) return false
            webViewFileChooseCallback = filePathCallback
            startFileChooserActivity("*/*")
            return true
        }
    }

    fun startFileChooserActivity(mimeType: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = mimeType
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        // special intent for Samsung file manager
        val sIntent = Intent("com.sec.android.app.myfiles.PICK_DATA")
        // if you want any file type, you can skip next line
        sIntent.putExtra("CONTENT_TYPE", mimeType)
        sIntent.addCategory(Intent.CATEGORY_DEFAULT)

        val chooserIntent: Intent
        if (packageManager.resolveActivity(sIntent, 0) != null) {
            // it is device with Samsung file manager
            chooserIntent = Intent.createChooser(sIntent, "Open file")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intent))
        } else {
            chooserIntent = Intent.createChooser(intent, "Open file")
        }

        try {
            startActivityForResult(chooserIntent, REQ_CODE_CHOOSER)
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(applicationContext, "No suitable File Manager was found.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webView = findViewById<WebView>(R.id.webView)
        webView.webChromeClient = chromeClient
        webView.webViewClient = webClient

        if (savedInstanceState == null) {
            val encodedHtml = Base64.encodeToString(unencodedHtml.toByteArray(), Base64.NO_PADDING)
            webView.loadData(encodedHtml, "text/html", "base64")
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        findViewById<WebView>(R.id.webView).saveState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == REQ_CODE_CHOOSER && resultCode == Activity.RESULT_OK && data != null -> {
                val uri = when {
                    data.dataString != null -> arrayOf(Uri.parse(data.dataString))
                    data.clipData != null -> (0 until data.clipData!!.itemCount)
                        .mapNotNull { data.clipData?.getItemAt(it)?.uri }
                        .toTypedArray()
                    else -> null
                }
                webViewFileChooseCallback?.onReceiveValue(uri)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
