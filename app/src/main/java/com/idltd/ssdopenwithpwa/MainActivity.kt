package com.idltd.ssdopenwithpwa

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatch()
        finish()
    }

    private fun dispatch() {
        val uri = intent?.data ?: return

        val filename = resolveFilename(uri)
        val extension = filename
            ?.substringAfterLast('.', "")
            ?.let { if (it.isNotEmpty()) ".$it" else "" }
            ?: ""
        val mimeType = intent.type

        val config = try {
            DispatcherConfig.load(this)
        } catch (e: Exception) {
            toast("Config load failed: ${e.message}")
            return
        }

        val route = config.findRoute(extension, mimeType)
        if (route == null) {
            // Silently finish for non-routable types (common with the octet-stream fallback filter)
            return
        }

        try {
            when (route.destination.type) {
                "url_param" -> dispatchViaUrlParam(uri, route.destination)
                else -> toast("Unknown route type: ${route.destination.type}")
            }
        } catch (e: ActivityNotFoundException) {
            toast("No browser found to open the PWA")
        } catch (e: Exception) {
            toast("Dispatch failed: ${e.message}")
        }
    }

    private fun dispatchViaUrlParam(fileUri: Uri, dest: Destination) {
        val bytes = when (fileUri.scheme) {
            "content" -> contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
            "file"    -> java.io.File(fileUri.path!!).readBytes()
            else      -> throw IllegalArgumentException("Unsupported URI scheme: ${fileUri.scheme}")
        } ?: throw IllegalStateException("Cannot read file")

        val encoded = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val targetUrl = "${dest.url.trimEnd('/')}/?${dest.param}=$encoded"
        val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
        dest.browser?.let { launchIntent.setPackage(it) }
        startActivity(launchIntent)
    }

    private fun resolveFilename(uri: Uri): String? {
        if (uri.scheme == "content") {
            contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(
                        cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    )
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
