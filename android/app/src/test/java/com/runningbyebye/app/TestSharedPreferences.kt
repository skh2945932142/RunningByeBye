package com.runningbyebye.app

import android.content.SharedPreferences

class TestSharedPreferences : SharedPreferences {
    private val values = linkedMapOf<String, String>()

    override fun getString(key: String?, defValue: String?): String? {
        return values[key] ?: defValue
    }

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
    override fun getFloat(key: String?, defValue: Float): Float = defValue
    override fun getInt(key: String?, defValue: Int): Int = defValue
    override fun getLong(key: String?, defValue: Long): Long = defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, String?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) {
                pending[key] = value
            }
            return this
        }

        override fun apply() {
            commit()
        }

        override fun commit(): Boolean {
            if (clearRequested) {
                values.clear()
            }
            pending.forEach { (key, value) ->
                if (value == null) {
                    values.remove(key)
                } else {
                    values[key] = value
                }
            }
            return true
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) {
                pending[key] = null
            }
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
    }
}
