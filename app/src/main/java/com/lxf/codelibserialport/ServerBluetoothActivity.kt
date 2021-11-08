package com.lxf.codelibserialport

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class ServerBluetoothActivity : AppCompatActivity() {
    private var serverThread: AcceptThread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_bluetooth)

        findViewById<View>(R.id.btnStartServer).setOnClickListener {
//            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600)
//            startActivity(discoverableIntent);
            startServer()
        }
    }

    private fun startServer() {
        serverThread = AcceptThread()
        serverThread?.start()
    }
}