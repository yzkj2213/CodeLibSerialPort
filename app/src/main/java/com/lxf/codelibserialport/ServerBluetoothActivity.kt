package com.lxf.codelibserialport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class ServerBluetoothActivity : AppCompatActivity() {
    private var serverThread: AcceptThread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_bluetooth)

        findViewById<View>(R.id.btnStartServer).setOnClickListener {
            startServer()
        }
    }

    private fun startServer() {
        serverThread = AcceptThread()
        serverThread?.start()
    }
}