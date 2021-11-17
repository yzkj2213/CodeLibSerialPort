package com.lxf.codelibserialport

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.izis.serialport.connect.SerialConnectBLE
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val connect = SerialConnectBLE(this)
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
        findViewById<View>(R.id.btnWriteSAL).setOnClickListener {
            var s = "~SAL0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000#";

            for (i in (0..20)) {
                val index = Random().nextInt(360) + 4
                s = s.replaceRange((index..index), "1")
            }
            connect.writeAndFlush(s)
        }

        findViewById<View>(R.id.btnRGC).setOnClickListener {
            connect.writeAndFlush("~RGC#")
        }
    }
}