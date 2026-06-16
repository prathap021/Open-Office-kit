package com.example.openofficekit

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<CardView>(R.id.btnPickCard).setOnClickListener {
            filePicker.launch("*/*")
        }
    }

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult

        val intent = Intent(this, DocumentViewerActivity::class.java).apply {
            putExtra("document_uri", uri.toString())
        }
        startActivity(intent)
    }
}
