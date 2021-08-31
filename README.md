> 提供三种方式连接隐智电子棋盘。

- 使用PL2303官方提供的jar包：SerialConnectPl2303
- 使用google开源的android-serial-port（需要root权限）：SerialConnectJNI
- 使用android提供的API：SerialConnectAPI

# 推荐的使用方式：
```java
    public static SerialConnect newInstance(Context context){
        if (Build.VERSION.RELEASE.equals("8.2.0"))
            return new SerialConnectAPI(context);
        else
            return new SerialConnectPl2303(context);
    }

```