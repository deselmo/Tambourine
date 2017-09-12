package com.deselmo.android.tambourine

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout


class RenameAudioDialog  : DialogFragment() {
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val linearLayout: LinearLayout = LayoutInflater
                .from(context)
                .inflate(R.layout.rename_dialog, null) as LinearLayout

        val editText = (linearLayout.getChildAt(0) as EditText)
        editText.setText(tag.toCharArray(), 0, tag.length)
        editText.setSelection(0, tag.length-4)


        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.rename))
                .setPositiveButton(context.getString(R.string.rename), { _, _ ->
                    context.startService(Intent(context, FileManagerService::class.java)
                            .setAction(FileManagerService.RENAME)
                            .putExtra(FileManagerService.RENAME_FROM_EXTRA, tag)
                            .putExtra(FileManagerService.RENAME_TO_EXTRA, editText.text.toString()))
                })
                .setNegativeButton(context.getString(R.string.cancel), { _, _ -> })
                .setView(linearLayout)

        val dialog = builder.create()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        return dialog
    }
}