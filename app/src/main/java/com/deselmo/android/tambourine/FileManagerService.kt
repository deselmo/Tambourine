package com.deselmo.android.tambourine

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import java.io.File
import android.widget.Toast
import java.lang.ref.WeakReference


class FileManagerService: Service() {
    companion object {
        val RENAME: String = "com.deselmo.android.tambourine.FileManagerService.RENAME"
        val RENAME_FROM_EXTRA: String = "RENAME_FROM_EXTRA"
        val RENAME_TO_EXTRA: String = "RENAME_TO_EXTRA"

        val DELETE: String = "com.deselmo.android.tambourine.FileManagerService.DELETE"
        val DELETE_EXTRA: String = "DELETE_EXTRA"
    }

    private var prefix: String = ""

    override fun onCreate() {
        prefix = getExternalFilesDir("Records").absolutePath + "/"

        super.onCreate()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            when (intent.action) {
                RENAME -> {
                    val fileNameFrom = prefix + intent.getStringExtra(RENAME_FROM_EXTRA)
                    val fileNameTo = prefix + intent.getStringExtra(RENAME_TO_EXTRA)

                    RenameFileAsyncTask(this).execute(fileNameFrom, fileNameTo)
                }
                DELETE -> {
                    val fileName = prefix + intent.getStringExtra(DELETE_EXTRA)

                    DeleteFileAsyncTask(this).execute(fileName)
                }
            }
        }

        return Service.START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    private class RenameFileAsyncTask internal constructor(context: Context) :
            AsyncTask<String, Void, Array<Any?>>() {

        private val WeakReferenceContext: WeakReference<Context> = WeakReference(context)

        override fun doInBackground(vararg fileNames: String?): Array<Any?> {
            return arrayOf(File(fileNames[0]).renameTo(File(fileNames[1])), fileNames[0], fileNames[1])
        }

        override fun onPostExecute(result: Array<Any?>?) {
            val message = if(result != null && result[0] == true) {
                LocalBroadcastManager.getInstance(WeakReferenceContext.get())
                        .sendBroadcast(Intent(PlayActivity.UPDATE_UI_FILTER)
                                .putExtra(PlayActivity.ACTION, PlayActivity.FILE_RENAMED)
                                .putExtra(PlayActivity.FILE_RENAMED_NAME_FROM, result[1] as String)
                                .putExtra(PlayActivity.FILE_RENAMED_NAME_TO, result[2] as String))

                WeakReferenceContext.get()?.getString(R.string.file_renamed)
            } else {
                WeakReferenceContext.get()?.getString(R.string.audio_renaming_failed)
            }

            Toast.makeText(WeakReferenceContext.get(), message, Toast.LENGTH_SHORT).show()
        }
    }


    private class DeleteFileAsyncTask internal constructor(context: Context) :
            AsyncTask<String, Void, Array<Any?>>() {

        private val WeakReferenceContext: WeakReference<Context> = WeakReference(context)

        override fun doInBackground(vararg fileName: String?): Array<Any?> {
            return arrayOf(File(fileName[0]).delete(), fileName[0])
        }

        override fun onPostExecute(result: Array<Any?>?) {
            val message = if(result != null && result[0] == true) {
                LocalBroadcastManager.getInstance(WeakReferenceContext.get())
                        .sendBroadcast(Intent(PlayActivity.UPDATE_UI_FILTER)
                                .putExtra(PlayActivity.ACTION, PlayActivity.FILE_DELETED)
                                .putExtra(PlayActivity.FILE_DELETED_NAME, result[1] as String))

                WeakReferenceContext.get()?.getString(R.string.audio_removed)
            } else {
                WeakReferenceContext.get()?.getString(R.string.audio_removed_failed)
            }

            Toast.makeText(WeakReferenceContext.get(), message, Toast.LENGTH_SHORT).show()
        }
    }
}