package com.deselmo.android.tambourine

import android.content.ActivityNotFoundException
import android.content.Intent
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.io.File
import android.view.ContextMenu
import android.widget.Filter
import android.widget.Toast


open class AudioFileRecyclerViewAdapter(private var _mFileList: ArrayList<File>) :
        RecyclerView.Adapter<AudioFileRecyclerViewAdapter.ViewHolder>() {

    private fun copyToFileListFiltered() { mFileList.mapTo(mFileListFiltered) { File(it.path) } }

    var mFileList: ArrayList<File>
        get() = _mFileList
        set(newFileList) {
            _mFileList = newFileList
            mFileListFiltered.clear()
            copyToFileListFiltered()
            notifyDataSetChanged()
        }
    private var mFileListFiltered = ArrayList<File>()
    private var lastQuery: CharSequence? = null

    init { copyToFileListFiltered() }

    private val _filter: Filter = AudioFileFilter()
    val filter: Filter
        get() = _filter


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_audio, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file: File = mFileListFiltered[position]
        holder.bind(file)
    }

    override fun getItemCount(): Int {
        return mFileListFiltered.size
    }

    fun add(file: File) {
        mFileList.add(file)
        filter.filter(lastQuery)
    }

    fun remove(file: File) {
        mFileList.remove(file)
        filter.filter(lastQuery)
    }

    fun rename(fileFrom: File, fileTo: File) {
        mFileList.remove(fileTo)
        mFileList[mFileList.indexOf(fileFrom)] = fileTo
        filter.filter(lastQuery)
    }


    inner class ViewHolder(private val mView: View) : RecyclerView.ViewHolder(mView),
            View.OnCreateContextMenuListener {
        private var file: File? = null
        private val mContentView: TextView = mView.findViewById(R.id.textView)

        init {
            mView.setOnCreateContextMenuListener(this)
        }


        fun bind(file: File) {
            this.file = file
            mContentView.text = file.name

            mView.setOnClickListener { view ->
                val context = view.context

                val intent = Intent(Intent.ACTION_VIEW)
                val uri = FileProvider.getUriForFile(
                        context,
                        context.applicationContext.packageName + ".provider",
                        file)
                intent.data = uri
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    context.startActivity(intent)
                } catch(e: ActivityNotFoundException) {
                    Toast.makeText(context, context.getString(R.string.unable_to_open_file),
                            Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu?,
                                         v: View?,
                                         menuInfo: ContextMenu.ContextMenuInfo?) {
            if(menu != null && v != null && file != null) {
                val context = v.context
                val fileName = file?.name
                val uri = FileProvider
                        .getUriForFile(context, context.packageName + ".provider", file)

                menu.setHeaderTitle(fileName)
                menu.add(context.getString(R.string.play))
                        .setOnMenuItemClickListener {
                            context.startActivity(Intent(Intent.ACTION_VIEW)
                                    .setData(uri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))

                            true
                        }

                menu.add(context.getString(R.string.share))
                        .setOnMenuItemClickListener {
                            val sharingIntent = Intent(Intent.ACTION_SEND)
                            sharingIntent.type = "audio/w4a"
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
                            context.startActivity(Intent.createChooser(sharingIntent,
                                            context.getString(R.string.share_audio_using)))

                            true
                        }

                menu.add(context.getString(R.string.rename))
                        .setOnMenuItemClickListener {
                            val dialog = RenameAudioDialog()
                            dialog.show((context as AppCompatActivity)
                                    .supportFragmentManager, fileName)

                            true
                        }

                menu.add(context.getString(R.string.delete))
                        .setOnMenuItemClickListener {
                            val dialog = DeleteConfirmationDialog()
                            dialog.show((context as AppCompatActivity)
                                    .supportFragmentManager, fileName)

                            true
                        }
            }
        }
    }

    inner class AudioFileFilter: Filter() {
        override fun performFiltering(query: CharSequence?): FilterResults {
            lastQuery = query
            val results = Filter.FilterResults()
            if (query == null || query.isEmpty()) {
                results.values = mFileList
                results.count = mFileList.size
            } else {
                val fRecords = ArrayList<File>()

                mFileList.filterTo(fRecords) { file ->
                    file.name.toUpperCase().trim({ it <= ' ' })
                            .contains(query.toString().toUpperCase().trim())
                }
                results.values = fRecords
                results.count = fRecords.size
            }
            return results
        }

        override fun publishResults(query: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            mFileListFiltered = results?.values as ArrayList<File>
            notifyDataSetChanged()
        }

    }
}