package com.idltd.ssdopenwithpwa

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
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
            text = "Bundled routes (.ssd → SSD PWA) are always active.\nUser routes here override or extend them.\nLong-press a route to delete."
            textSize = 11f
            setPadding(0, 12, 0, 0)
        }
        root.addView(hint)

        setContentView(root)
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

        layout.addView(label("Extension"))
        layout.addView(extInput)
        layout.addView(label("PWA URL"))
        layout.addView(urlInput)

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
                val route = Route(
                    extensions   = listOf(ext),
                    mimeTypes    = listOf("application/octet-stream"),
                    destination  = Destination(type = "url_param", url = url, param = "ssd")
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
            return row
        }
    }
}
