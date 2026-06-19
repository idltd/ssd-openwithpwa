package com.idltd.ssdopenwithpwa

import android.content.Context
import org.json.JSONObject

data class Destination(
    val type: String,
    val url: String,
    val param: String
)

data class Route(
    val extensions: List<String>,
    val mimeTypes: List<String>,
    val destination: Destination
)

data class DispatcherConfig(
    val version: Int,
    val routes: List<Route>
) {
    fun findRoute(extension: String, mimeType: String?): Route? {
        val ext = extension.lowercase()
        val mime = mimeType?.lowercase()
        return routes.firstOrNull { route ->
            route.extensions.any { it.lowercase() == ext } ||
            (mime != null && mime != "application/octet-stream" &&
             route.mimeTypes.any { it.lowercase() == mime })
        }
    }

    companion object {
        fun load(context: Context): DispatcherConfig {
            val json = context.assets.open("dispatcher-config.json")
                .bufferedReader().readText()
            return parse(JSONObject(json))
        }

        private fun parse(root: JSONObject): DispatcherConfig {
            val routesArr = root.getJSONArray("routes")
            val routes = (0 until routesArr.length()).map { i ->
                val r = routesArr.getJSONObject(i)
                val exts  = r.getJSONArray("extensions")
                val mimes = r.getJSONArray("mime_types")
                val dest  = r.getJSONObject("destination")
                Route(
                    extensions = (0 until exts.length()).map  { exts.getString(it) },
                    mimeTypes  = (0 until mimes.length()).map { mimes.getString(it) },
                    destination = Destination(
                        type  = dest.getString("type"),
                        url   = dest.getString("url"),
                        param = dest.getString("param")
                    )
                )
            }
            return DispatcherConfig(version = root.getInt("version"), routes = routes)
        }
    }
}
