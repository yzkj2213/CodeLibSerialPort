package com.lxf.codelibserialport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.izis.serialport.connect.SerialConnectBluetooth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val connect = SerialConnectBluetooth(this)
        findViewById<View>(R.id.btnOpen).setOnClickListener {
            connect.open()
        }

        findViewById<View>(R.id.btnClose).setOnClickListener {
            connect.close()
        }

        findViewById<View>(R.id.btnWriteSTA).setOnClickListener {
//            connect.writeAndFlush("~TLO010,r255g000b000,1#")
//            connect.writeAndFlush("~TLO010,r255g000b000,1#")
////            connect.writeAndFlush("~STA#")
//            connect.writeAndFlush("~RGC#")
//            connect.writeAndFlush("~RGC#")
//
//            connect.writeAndFlush("~BOD19#")
            connect.writeAndFlush("~STA#")
        }
    }
}