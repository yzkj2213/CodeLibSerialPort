> 提供三种方式连接隐智电子棋盘。

- 使用PL2303官方提供的jar包：SerialConnectPl2303
- 使用google开源的android-serial-port（需要root权限）：SerialConnectJNI
- 使用android提供的API：SerialConnectAPI

# 推荐的使用方式：
```java
    public static SerialConnect newInstance(Context context){
        //return new SerialConnectPl2303(context);
        //return new SerialConnectJNI(context);
        return new SerialConnectAPI(context);
    }

    //...

    SerialConnect serialConnect = newInstance(context);
    serialConnect.setConnectListener();
    serialConnect.setReceiveDataListener();
    serialConnect.setSendDataListener();
    serialConnect.open();

    //...

    //sendNumMax为0时，两者等价
    //写入指令
    serialConnect.writeAndFLush();
    //已应答的方式写入指令，未收到响应最多发送3次
    serialConnect.addCommend();

    //...

    serialConnect.close();
```