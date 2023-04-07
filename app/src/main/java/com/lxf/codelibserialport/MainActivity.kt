package com.lxf.codelibserialport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.izis.serialport.connect.SerialConnectService
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
            connect.writeAndFlush(BoardProtocol.Down.requestAllChess())
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
            connect.writeAndFlush(
                BoardProtocol.Down.lampMultiple(
                    19,
                    list,
                    3
                )
            )
        }

        findViewById<View>(R.id.btnSingleLamp).setOnClickListener {
            connect.writeAndFlush(
                BoardProtocol.Down.lampPosition(
                    180,
                    Random().nextInt(2) + 1
                )
            )
        }

        findViewById<View>(R.id.btnRGC).setOnClickListener {
            connect.writeAndFlush(BoardProtocol.Down.closeAllLamp())
        }
    }
}