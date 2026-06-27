package com.idltd.ssdopenwithpwa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Destination(
    val type: String,
    val url: String,
    val param: String,
    val browser: String? = null
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

    fun toJson(): JSONObject = JSONObject().apply {
        put("version", version)
        put("routes", JSONArray().also { arr ->
            routes.forEach { route ->
                arr.put(JSONObject().apply {
                    put("extensions", JSONArray(route.extensions))
                    put("mime_types", JSONArray(route.mimeTypes))
                    put("destination", JSONObject().apply {
                        put("type",  route.destination.type)
                        put("url",   route.destination.url)
                        put("param", route.destination.param)
                        route.destination.browser?.let { put("browser", it) }
                    })
                })
            }
        })
    }

    companion object {
        private const val USER_CONFIG = "user-config.json"

        fun load(context: Context): DispatcherConfig {
            val userFile = File(context.filesDir, USER_CONFIG)
            if (userFile.exists()) return parse(JSONObject(userFile.readText()))
            return parseBundled(context)
        }

        fun initIfNeeded(context: Context) {
            val userFile = File(context.filesDir, USER_CONFIG)
            if (!userFile.exists()) {
                val bundled = parseBundled(context)
                userFile.writeText(bundled.toJson().toString(2))
            }
        }

        fun saveUserConfig(context: Context, routes: List<Route>) {
            val cfg = DispatcherConfig(version = 1, routes = routes)
            File(context.filesDir, USER_CONFIG).writeText(cfg.toJson().toString(2))
        }

        fun loadUserRoutes(context: Context): MutableList<Route> {
            initIfNeeded(context)
            return load(context).routes.toMutableList()
        }

        private fun parseBundled(context: Context) = parse(JSONObject(
            context.assets.open("dispatcher-config.json").bufferedReader().readText()
        ))

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
                        type    = dest.getString("type"),
                        url     = dest.getString("url"),
                        param   = dest.getString("param"),
                        browser = if (dest.has("browser")) dest.getString("browser") else null
                    )
                )
            }
            return DispatcherConfig(version = root.getInt("version"), routes = routes)
        }
    }
}
