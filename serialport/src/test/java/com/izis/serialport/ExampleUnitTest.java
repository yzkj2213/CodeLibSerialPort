package com.izis.serialport;

import com.izis.serialport.util.Log;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testSync() throws InterruptedException {

//        String data = "1HAA0023#~HOTA001B~HOT002#";
//        String p = "~?[A-Z]{3}[^~#]*#";
//        Pattern r = Pattern.compile(p);
//        Matcher m = r.matcher(data);
//        while (m.find()) {
//            System.out.println(m.group());
//        }
//
        String data = "~HOT002.#";
        Pattern pattern = Pattern.compile("[^A-Za-z0-9~#.,]");
        Matcher matcher = pattern.matcher(data);
        System.out.println(matcher.find());



//        TestSync testSync = new TestSync();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                testSync.test1();
//            }
//        }).start();
//        testSync.test3();
//
//        Thread.sleep(10 * 1000);

    }
}

class TestSync {
    private final LinkedList<String> list = new LinkedList<>();

    public synchronized void test1() {
        System.out.println("test1===>start");
        list.add("1");
        System.out.println("test1===>end");
    }

    public  void test2() throws InterruptedException {
        System.out.println("test2===>start");
        if (!list.isEmpty())
            list.removeFirst();

        Thread.sleep(3000);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("test2===>");
            }
        }, 3000);
        System.out.println("test2===>end");
    }

    public synchronized void test3() throws InterruptedException {
        System.out.println("test3===>start");
        test2();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("test3===>");
            }
        }, 3000);
        System.out.println("test3===>end");
    }
}