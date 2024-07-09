package com.lxf.codelibserialport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.izis.serialport.connect.SerialConnect
import com.izis.serialport.connect.SerialConnectDirect
import com.izis.serialport.connect.SerialConnectService
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

class SecondActivity : AppCompatActivity() {

    private lateinit var connect: SerialConnect
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        connect = SerialConnectDirect(this)
        connect.setConnectListener {
            Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show()
            connect.addCommend("~LED11#")
            connect.addCommend("~LED21#")
        }
        connect.open()

        findViewById<View>(R.id.btnSecDestory).setOnClickListener {
            destroy()
        }
        findViewById<View>(R.id.btnCheckConnectMulti).setOnClickListener {
            val num = checkConnectsError()
            Toast.makeText(this, num.toString(), Toast.LENGTH_SHORT).show()
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

    private fun checkConnectsError(): Int {
        try {
            println("哈哈 开始检测")
            var num = 0
            //adb root & adb remount & adb shell lsof /dev/ttyS1
//            val process = Runtime.getRuntime().exec("lsof /dev/ttyS1")
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("lsof /dev/ttyS1\n")
            os.writeBytes("exit\n")
            os.flush()

            process.waitFor()
            var line: String?
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            while (reader.readLine().also { line = it } != null) {
                println("哈哈$line")
                if (line?.contains("/dev/ttyS1") == true) {
                    num++
                }
            }
            reader.close()
            return num
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return 0
    }
}