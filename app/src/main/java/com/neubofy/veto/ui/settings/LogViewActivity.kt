package com.neubofy.veto.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.veto.R
import com.neubofy.veto.data.LogRepository
import com.neubofy.veto.ui.VetoActivity
import com.neubofy.veto.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.neubofy.veto.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import com.neubofy.veto.utils.log
import com.neubofy.veto.utils.writeToUri
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter


private const val EXPORT_REQ_CODE = 30

class LogViewActivity : VetoActivity() {

    companion object {
        private val TAG = LogViewActivity::class.simpleName
    }

    private lateinit var repo: LogRepository

    private lateinit var adapter: LogViewAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        setupEdgeToEdgeScrollView(findViewById(R.id.recycler_logs))

        repo = LogRepository.getInstance(this)

        // TODO: Observe list as LiveData or Flow
        adapter = LogViewAdapter()
        recyclerView = findViewById<RecyclerView>(R.id.recycler_logs)
        recyclerView.adapter = adapter

        synchronized(repo.list) { adapter.submitList(repo.list) }
        recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_log_view, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuExportLog) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.putExtra(Intent.EXTRA_TITLE, LogRepository.filenameForExport())
            intent.type = "*/*"
            startActivityForResult(intent, EXPORT_REQ_CODE)
        } else if (item.itemId == R.id.menuClearLog) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.log_view_clear))
                .setMessage(R.string.log_view_clear_confirm)
                .setPositiveButton(getString(R.string.Ok), { dialog, button ->
                    repo.clearLog()
                    // TODO: let adapter observe list, instead of explicitly updating the adapter
                    synchronized(repo.list) { adapter.submitList(repo.list) }
                    recyclerView.scrollToPosition(0)
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        this.log().d(TAG, "requestCode=$requestCode resultCode=$resultCode")

        if (requestCode == EXPORT_REQ_CODE && resultCode == RESULT_OK) {
            if (data == null) {
                this.log().d(TAG, "data is null")
                return
            }

            val uri = data.data
            if (uri == null) {
                this.log().d(TAG, "uri is null")
                return
            }

            // This should be "safe" to log (no personal information), because the format is:
            // content://org.nextcloud.documents/document/d88e9571089101a5c6407b061422b6a4%2F1968
            // content://com.android.externalstorage.documents/document/primary%3Aveto-logs-2025-08-31.json
            this.log().d(TAG, "exporting logs to $uri")

            lifecycleScope.launch {
                writeToUri(this@LogViewActivity, uri) { outputStream ->
                    synchronized(repo.list) {
                        val writer = OutputStreamWriter(outputStream)
                        repo.writeAsJson(writer)
                        writer.close()
                    }
                }
            }
        }
    }
}
