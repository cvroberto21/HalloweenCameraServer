package com.example.halloweencameraserver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.halloweencameraserver.logging.LogFragment
import com.example.halloweencameraserver.logging.LogWrapper
import com.example.halloweencameraserver.logging.MessageOnlyLogFilter
import com.example.halloweencameraserver.ui.main.MainFragment
import com.example.halloweencameraserver.logging.Log
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.main_fragment.*

class MainActivity : AppCompatActivity() {
    val TAG = "MainAct"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace( main_fragment.id, MainFragment.newInstance())
                    .commitNow()
        }
    }

    fun initializeLogging() {
        // Wraps Android's native log framework.
        val logWrapper = LogWrapper()
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.logNode = logWrapper

        // Filter strips out everything except the message text.
        val msgFilter = MessageOnlyLogFilter()
        logWrapper.next = msgFilter

        // On screen logging via a fragment with a TextView.
        val logFragment = log_fragment as LogFragment
        msgFilter.next = logFragment.logView
        Log.i(TAG, "Ready")
    }

}
