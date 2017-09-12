package com.deselmo.android.tambourine

import android.app.Dialog
import android.support.v4.app.DialogFragment
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog


class DeleteConfirmationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(context.getString(R.string.delete) + " \"$tag\"?")
                .setPositiveButton(context.getString(R.string.delete), { _, _ ->
                    context.startService(Intent(context, FileManagerService::class.java)
                            .setAction(FileManagerService.DELETE)
                            .putExtra(FileManagerService.DELETE_EXTRA, tag))
                })
                .setNegativeButton(context.getString(R.string.cancel), { _, _ -> })
        return builder.create()
    }
}