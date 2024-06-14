package com.lxf.codelibserialport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.izis.serialport.connect.SerialConnect
import com.izis.serialport.connect.SerialConnectService

class SecondActivity : AppCompatActivity() {

    private lateinit var connect: SerialConnect
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        connect = SerialConnectService(this)
        connect.setConnectListener {
            Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show()
            connect.addCommend("~LED11#")
            connect.addCommend("~LED21#")
        }
        connect.open()

        findViewById<View>(R.id.btnSecDestory).setOnClickListener {
            destroy()
        }
    }

    private fun destroy() {
        connect.addCommend("~RGC#")
        connect.addCommend("~LED10#")
        connect.addCommend("~LED20#")
        connect.addCommend("~CAL#")
        connect.addCommend("~CTS1#")
        connect.close(false)
    }

    override fun onDestroy() {
        destroy()
        super.onDestroy()
    }
}