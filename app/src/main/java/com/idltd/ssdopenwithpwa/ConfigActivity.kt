package com.idltd.ssdopenwithpwa

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {

    private val userRoutes = mutableListOf<Route>()
    private lateinit var listView: ListView
    private lateinit var adapter: RouteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "PWA→ Routes"

        userRoutes.addAll(DispatcherConfig.loadUserRoutes(this))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val header = TextView(this).apply {
            text = "Extension → PWA URL"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        root.addView(header)

        listView = ListView(this)
        adapter = RouteAdapter()
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, pos, _ -> showEditDialog(pos) }
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            confirmDelete(pos)
            true
        }
        root.addView(listView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val addBtn = Button(this).apply {
            text = "+ Add route"
            setOnClickListener { showEditDialog(null) }
        }
        root.addView(addBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.topMargin = 16 })

        val hint = TextView(this).apply {
            text = "Long-press a route to delete."
            textSize = 11f
            setPadding(0, 12, 0, 0)
        }
        root.addView(hint)

        setContentView(root)
    }

    private fun getAppsForUrl(url: String): List<Pair<String, String?>> {
        val uri = try { Uri.parse(url.trim()) } catch (e: Exception) { return listOf("Default" to null) }

        val browserPkgs = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com")), PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }.toSet()

        val all = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_VIEW, uri), PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString() }

        val browsers  = all.filter {  browserPkgs.contains(it.activityInfo.packageName) }
        val nativeApps = all.filter { !browserPkgs.contains(it.activityInfo.packageName) }

        return listOf("Default" to null) +
            browsers.map  { it.loadLabel(packageManager).toString()         to it.activityInfo.packageName } +
            nativeApps.map { "App: ${it.loadLabel(packageManager)}"         to it.activityInfo.packageName }
    }

    private fun showEditDialog(position: Int?) {
        val existing = position?.let { userRoutes[it] }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        val extInput = EditText(this).apply {
            hint = "Extension (e.g. .ssd)"
            setText(existing?.extensions?.firstOrNull() ?: "")
        }
        val urlInput = EditText(this).apply {
            hint = "PWA URL (e.g. https://example.com/)"
            setText(existing?.destination?.url ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_URI
        }

        var apps = getAppsForUrl(existing?.destination?.url ?: "")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, apps.map { it.first }.toMutableList())
        val appSpinner = Spinner(this).apply { adapter = spinnerAdapter }
        val existingPkg = existing?.destination?.browser
        appSpinner.setSelection(apps.indexOfFirst { it.second == existingPkg }.takeIf { it >= 0 } ?: 0)

        val refreshBtn = Button(this).apply {
            text = "Find apps for this URL"
            setOnClickListener {
                apps = getAppsForUrl(urlInput.text.toString())
                spinnerAdapter.clear()
                spinnerAdapter.addAll(apps.map { it.first })
                spinnerAdapter.notifyDataSetChanged()
                appSpinner.setSelection(0)
            }
        }

        layout.addView(label("Extension"))
        layout.addView(extInput)
        layout.addView(label("PWA URL"))
        layout.addView(urlInput)
        layout.addView(refreshBtn)
        layout.addView(label("Open with"))
        layout.addView(appSpinner)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add route" else "Edit route")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val ext = extInput.text.toString().trim().let {
                    if (it.startsWith(".")) it else ".$it"
                }
                val url = urlInput.text.toString().trim()
                if (ext.length < 2 || url.isEmpty()) {
                    Toast.makeText(this, "Extension and URL are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val selectedApp = apps[appSpinner.selectedItemPosition].second
                val route = Route(
                    extensions  = listOf(ext),
                    mimeTypes   = listOf("application/octet-stream"),
                    destination = Destination(type = "url_param", url = url, param = "ssd", browser = selectedApp)
                )
                if (position != null) userRoutes[position] = route else userRoutes.add(route)
                save()
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(position: Int) {
        val ext = userRoutes[position].extensions.firstOrNull() ?: "?"
        AlertDialog.Builder(this)
            .setTitle("Delete route")
            .setMessage("Remove route for \"$ext\"?")
            .setPositiveButton("Delete") { _, _ ->
                userRoutes.removeAt(position)
                save()
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun save() = DispatcherConfig.saveUserConfig(this, userRoutes)

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setPadding(0, 12, 0, 2)
    }

    inner class RouteAdapter : BaseAdapter() {
        override fun getCount() = userRoutes.size
        override fun getItem(pos: Int) = userRoutes[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
            val route = userRoutes[pos]
            val row = LinearLayout(this@ConfigActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 20, 16, 20)
            }
            val ext = TextView(this@ConfigActivity).apply {
                text = route.extensions.joinToString(", ")
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val url = TextView(this@ConfigActivity).apply {
                text = route.destination.url
                textSize = 13f
            }
            row.addView(ext)
            row.addView(url)
            val browserPkg = route.destination.browser
            if (browserPkg != null) {
                val browserLabel = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(browserPkg, 0)
                    ).toString()
                } catch (e: Exception) { browserPkg }
                val browserView = TextView(this@ConfigActivity).apply {
                    text = "via $browserLabel"
                    textSize = 11f
                    setTextColor(android.graphics.Color.GRAY)
                }
                row.addView(browserView)
            }
            return row
        }
    }
}
