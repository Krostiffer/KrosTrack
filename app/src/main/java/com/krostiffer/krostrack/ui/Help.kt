package com.krostiffer.krostrack.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.krostiffer.krostrack.R

//Die Hilfe wird nur angezeigt, mehr nicht
class Help : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
    }
}