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

    private fun getInstalledBrowsers(): List<Pair<String, String?>> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val infos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val browsers = infos
            .filter { it.activityInfo.packageName != packageName }
            .map { it.loadLabel(packageManager).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first }
        return listOf("Default browser" to null) + browsers
    }

    private fun showEditDialog(position: Int?) {
        val existing = position?.let { userRoutes[it] }
        val browsers = getInstalledBrowsers()

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

        val browserSpinner = Spinner(this)
        val browserLabels = browsers.map { it.first }
        browserSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, browserLabels)
        val existingPkg = existing?.destination?.browser
        val selectedIndex = browsers.indexOfFirst { it.second == existingPkg }.takeIf { it >= 0 } ?: 0
        browserSpinner.setSelection(selectedIndex)

        layout.addView(label("Extension"))
        layout.addView(extInput)
        layout.addView(label("PWA URL"))
        layout.addView(urlInput)
        layout.addView(label("Open with browser"))
        layout.addView(browserSpinner)

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
                val selectedBrowser = browsers[browserSpinner.selectedItemPosition].second
                val route = Route(
                    extensions  = listOf(ext),
                    mimeTypes   = listOf("application/octet-stream"),
                    destination = Destination(type = "url_param", url = url, param = "ssd", browser = selectedBrowser)
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
