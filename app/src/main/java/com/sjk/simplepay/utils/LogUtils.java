package com.sjk.simplepay.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.robv.android.xposed.XposedBridge;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  LogUtils</p>
 * @ <p>Description: 懒得去判断这个日志是哪个进程发送的了，统一下日志接口吧。</p>
 * @ date:  2018/9/22
 * @ QQ群：524901982
 */
public class LogUtils {
    public static String MSGRECEIVED_ACTION = "com.sjk.simplepay.msgreceived";
    public static void show(String tips) {
        try {
            XposedBridge.log(tips);
        } catch (NoClassDefFoundError ignore) {

        }
        Log.e("LogUtils", tips);
    }

    public static void sendmsg(Context paramContext, String paramString)
    {
        Log.d("LogUtils", "sendmsg");
        Log.d("LogUtils", paramString);
        Intent localIntent = new Intent();
        localIntent.putExtra("data", paramString);

        localIntent.setAction(MSGRECEIVED_ACTION);
        paramContext.sendBroadcast(localIntent);
    }
    public static void sendmsg(Context paramContext, String paramString, Boolean isDebug)
    {
        Log.d("LogUtils", "sendmsg");
        Log.d("LogUtils", paramString);
        Intent localIntent = new Intent();
        localIntent.putExtra("data", paramString);
        if (isDebug) {
            localIntent.putExtra("isDebug", isDebug);
        }
        localIntent.setAction(MSGRECEIVED_ACTION);
        paramContext.sendBroadcast(localIntent);
    }

}
