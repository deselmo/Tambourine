package com.deselmo.android.tambourine

import android.content.Context
import android.preference.PreferenceManager
import java.util.ArrayList

class SoundLogic(var context: Context) {
    private val list: ArrayList<Float> = ArrayList()
    private val maxSize: Int = 5

    fun add(e: Float) {
        list.add(e)
        if(list.size > maxSize) {
            list.removeAt(0)
        }
    }

    fun checkHit(): String {
        if(list.size < maxSize
                || list[maxSize - 1] > list[maxSize - 2]
                || list[maxSize - 3] > list[maxSize - 2])
            return ""

        var returnValue = ""

        val maxPower = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("sensitivity", "50").toFloat()

        when {
            list[maxSize - 2] >= (maxPower / 6) * 5.8 -> returnValue = "powa.m4a"
            list[maxSize - 2] >= (maxPower / 6) * 5 -> returnValue = "central_sound.m4a"
            list[maxSize - 2] >= (maxPower / 6) * 4.5 -> returnValue = "upper2_sound.m4a"
            list[maxSize - 2] >= (maxPower / 6) * 3.8 -> returnValue = "upper_sound.m4a"
            list[maxSize - 2] >= (maxPower / 6) * 2 -> returnValue = "lower_sound.m4a"
        }

        if(returnValue != "") {
            (0..maxSize-4)
                    .filter { list[maxSize - 2] - list[it] < list[maxSize - 2] / 3 }
                    .forEach { return "" }
            println(list)
            list.clear()
            return returnValue
        }
        return ""
    }
}
