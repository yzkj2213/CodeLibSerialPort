package com.lxf.codelibserialport

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.izis.serialport.connect.SerialConnectService
import com.izis.serialport.device_id.OSInfo
import com.izis.serialport.listener.SerialReceiveDataListener
import com.izis.serialport.protocol.BoardProtocol
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val textViewResponse = findViewById<TextView>(R.id.textViewResponse)

        val connect = SerialConnectService(this)
        connect.setConnectListener {
            Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show()
        }
        connect.setReceiveDataListener(object : SerialReceiveDataListener {
            override fun onReceiveNormalData(data: String) {
                runOnUiThread {
                    textViewResponse.text = data
                }
            }

            override fun onReceiveErrorData(data: String) {

            }
        })
        findViewById<View>(R.id.btnOpen).setOnClickListener {
            connect.open()
        }

        findViewById<View>(R.id.btnClose).setOnClickListener {
            connect.close()

        }

        findViewById<View>(R.id.btnWriteSTA).setOnClickListener {
            connect.addCommend(BoardProtocol.Down.requestAllChess())
        }
        findViewById<View>(R.id.btnWriteSAL).setOnClickListener {
            val list = arrayListOf<IntArray>()
            for (i in 0 until 10){
                list.add(IntArray(2) { index ->
                    if (index == 0)
                        (Random().nextInt(361) + 1)
                    else
                        (Random().nextInt(9) + 1)
                })
            }
            connect.addCommend(
                BoardProtocol.Down.lampMultiple(
                    19,
                    list,
                    3
                )
            )
        }

        findViewById<View>(R.id.btnSingleLamp).setOnClickListener {
            connect.addCommend(
                BoardProtocol.Down.lampPosition(
                    180,
                    Random().nextInt(2) + 1
                )
            )
        }

        findViewById<View>(R.id.btnRGC).setOnClickListener {
            connect.addCommend(BoardProtocol.Down.closeAllLamp())
        }

        findViewById<View>(R.id.btnEmpty).setOnClickListener {
            connect.addCommend("")
        }

        findViewById<View>(R.id.btnTime).setOnClickListener {
            connect.addCommend("")
            println(OSInfo.getDeviceId())
        }

        findViewById<View>(R.id.btnSecond).setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}