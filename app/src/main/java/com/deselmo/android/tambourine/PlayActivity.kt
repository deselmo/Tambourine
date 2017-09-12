package com.deselmo.android.tambourine

import android.content.*
import android.os.Bundle
import java.io.File
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_play.*


class PlayActivity : AppCompatActivity() {
    companion object {
        val UPDATE_UI_FILTER: String = "com.deselmo.android.PlayActivity.UPDATE_UI_FILTER"
        val ACTION: String = "ACTION"
        val FILE_ADDED: String = "FILE_ADDED"
        val FILE_ADDED_NAME: String = "FILE_ADDED_NAME"
        val FILE_RENAMED: String = "FILE_RENAMED"
        val FILE_RENAMED_NAME_FROM: String = "FILE_RENAMED_NAME_FROM"
        val FILE_RENAMED_NAME_TO: String = "FILE_RENAMED_NAME_TO"
        val FILE_DELETED: String = "FILE_DELETED"
        val FILE_DELETED_NAME: String = "FILE_DELETED_NAME"
    }

    private var mAdapter: AudioFileRecyclerViewAdapter? = null

    private var menuToolbar: Menu? = null
    private var searchViewIsActive: Boolean = false
    private var searchQuery: CharSequence = ""

    private var recordsFileList: ArrayList<File> = ArrayList()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.extras[PlayActivity.ACTION]) {
                FILE_ADDED ->
                    mAdapter?.add(File(intent.extras[PlayActivity.FILE_ADDED_NAME] as String))

                FILE_RENAMED ->
                    mAdapter?.rename(
                            File(intent.extras[PlayActivity.FILE_RENAMED_NAME_FROM] as String),
                            File(intent.extras[PlayActivity.FILE_RENAMED_NAME_TO] as String))

                FILE_DELETED ->
                    mAdapter?.remove(File(intent.extras[PlayActivity.FILE_DELETED_NAME] as String))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeChange.apply(this)
        setContentView(R.layout.activity_play)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeRecyclerView()
        registerForContextMenu(recyclerView)

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, IntentFilter(PlayActivity.UPDATE_UI_FILTER))
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadcastReceiver)

        super.onDestroy()
    }


    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)

        val searchItem: MenuItem = menuToolbar?.findItem(R.id.action_search)!!
        bundle.putBoolean("searchViewIsActive", searchItem.isActionViewExpanded)
        bundle.putCharSequence("searchQuery", (searchItem.actionView as SearchView).query)
        bundle.putParcelable("recyclerViewState", recyclerView.layoutManager.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)

        searchViewIsActive = bundle.getBoolean("searchViewIsActive")
        searchQuery = bundle.getCharSequence("searchQuery")
        recyclerView.layoutManager
                .onRestoreInstanceState(bundle.getParcelable("recyclerViewState"))
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.menu_play, menu)
        menuToolbar = menu

        val searchView: SearchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.queryHint = getString(R.string.action_search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean { return false }
            override fun onQueryTextChange(query: String): Boolean {
                mAdapter?.filter?.filter(query)
                return true
            }
        })

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        if(searchViewIsActive) {
            val searchItem: MenuItem? = menuToolbar?.findItem(R.id.action_search)
            searchItem?.expandActionView()
            if (searchQuery.isNotEmpty()) {
                (searchItem?.actionView as SearchView).setQuery(searchQuery, false)
            }
        }
        return true
    }


    private fun initializeRecyclerView() {
        val recordsDir: File? = getExternalFilesDir("Records")
        val records: Array<File>? = recordsDir?.listFiles()

        (0 until records!!.size)
                .map { records[it] }
                .filter { it.isFile }
                .forEach { recordsFileList.add(it) }

        mAdapter = AudioFileRecyclerViewAdapter(recordsFileList)
        recyclerView.adapter = mAdapter
    }
}
