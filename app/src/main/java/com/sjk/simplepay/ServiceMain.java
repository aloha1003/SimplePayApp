package com.sjk.simplepay;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.sjk.simplepay.bll.ApiBll;
import com.sjk.simplepay.po.Configer;
import com.sjk.simplepay.po.DataSynEvent;
import com.sjk.simplepay.utils.LogUtils;
import com.sjk.simplepay.utils.PayUtils;
import com.sjk.simplepay.utils.ReceiveUtils;
import com.sjk.simplepay.po.CommFunction;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;


/**
 * @ Created by Dlg
 * @ <p>TiTle:  ServiceMain</p>
 * @ <p>Description: 这个类就一直轮循去请求是否需要二维码</p>
 * @ date:  2018/09/22
 */
public class ServiceMain extends Service {

    //是否启动了检测二维码需求的功能
    public static Boolean mIsRunning = false;
    public static Boolean mIsWechatRunning = false;
    public static Boolean mIsAlipayRunning = false;
    public static Boolean mIsUnionpayRunning = false;
    public static Boolean mAccountChanged = false;
    public static Boolean useWithServer = false;

    // 增加WebSocket方式，提高获单效率
    public WebSocketClient socketConnect;
//    private static final String wsUrl = Configer.getInstance().getUrl().replaceAll("http","ws") + "Alipay/deviceHub";
    private static final String wsUrl = Configer.getInstance().getSocketUrl() ;
    private static long lastHeartbeat = 0;
    private static int socketNum = 0;

    //上次询问服务器是否需要二维码的时间
    public static long mLastQueryTime = 0;

    //防止被休眠，你们根据情况可以开关，我是一直打开的，有点费电是必然的，哈哈
    private PowerManager.WakeLock mWakeLock;

    private ApiBll mApiBll;
    private static Service context;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        mLastQueryTime = System.currentTimeMillis();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP, "com.sjk.tpay:waketag");
        mWakeLock.acquire();

        mIsRunning = true;
        LogUtils.sendmsg(this, "onCreate: 服务启动", true);
        LogUtils.show("服务启动");
        try {
            //LogUtils.sendmsg(this, "onCreate: 尝试连接" + wsUrl);
//            LogUtils.sendmsg(this, "onCreate: 尝试连接" + wsUrl);
            socketNum = 1;
            if (useWithServer) {
//                openSocket();
            }

        }catch (Exception e){
//            Log.e("arik", "onCreate 出错:" + e.toString());
//            LogUtils.sendmsg(this, "服务结果:"+ e.toString());
//            reconnectToServer();
        }
        ReceiveUtils.startReceive();
        EventBus.getDefault().register(this);
        mApiBll = new ApiBll();
        if (!handler.hasMessages(0)) {
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.sendEmptyMessage(0);
        return START_STICKY;
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mIsRunning && System.currentTimeMillis() - lastHeartbeat > 20000) {//停止任务或WebSocket正常连接的时候，不会去轮循
//                Log.e("arik", "handler添加二维码任务"+msg);

//                mApiBll.checkQR(ServiceMain.this);
//                reconnectToServer();
            }
            if (handler.hasMessages(0)) {
                return;
            }
            if (ReceiverMain.getmLastSucc() != 0 && System.currentTimeMillis() - ReceiverMain.getmLastSucc() > 5000
			&& System.currentTimeMillis() - ReceiverMain.getmLastSucc() < 30000) {
                PayUtils.dealAlipayWebTrade(ServiceMain.this, ReceiverMain.getCook());
            }

            mLastQueryTime = System.currentTimeMillis();
            //0-7点的时候就慢速轮循
            handler.sendEmptyMessageDelayed(0,
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY) > 7 ? 5000
                            : 10000);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mWakeLock != null)
                mWakeLock.release();
            mWakeLock = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        LogUtils.show("服务被杀死");
        Intent intent = new Intent(this.getApplicationContext(), ServiceMain.class);
        this.startService(intent);
    }

    public void openSocket() throws URISyntaxException {
        //if(!mIsRunning) return;
        LogUtils.show( "openSocket");
        if(socketConnect==null){
//            LogUtils.sendmsg(context, "openSocket: 尝试连接 ");
            LogUtils.show( "openSocket: 尝试连接");
            socketConnect = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    LogUtils.show( "WebSocket onOpen");
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("protocol", "json");
                        obj.put("version", 1);
                        obj.put("command", "open");
                        sendMsg(obj.toString()+(char)0x1e);
                    }catch (Exception e){
                        e.printStackTrace();

                    }
                }
                @Override
                public void onMessage(String message) {
                    LogUtils.show("onMessage"+message);;
                    // 心跳包
                    if(CommFunction.getType(message)==6){
                        lastHeartbeat = System.currentTimeMillis();


                        if(mAccountChanged)
                        {
                            UpdateAccounts();
                            mAccountChanged = false;
                        }

                        LogUtils.sendmsg(context, "心跳包", true);
                        return;
                    }

//                    LogUtils.sendmsg(context, "服务器返回：" + message);

                    // 握手成功后，登陆
                    if(CommFunction.getType(message)==0){
//                        LogUtils.sendmsg(context, "握手成功");
                        try {
                            sendMsg(CommFunction.getInstance().getLoginString());
//                            Timer timer = new Timer();
//                            timer.schedule(new TimerTask() {
//                                @Override
//                                public void run() {
//                                    sendMsg(CommFunction.formateMessage("hello"));
//                                }
//                            }, 5000);
                        }catch (Exception e){

                        }
                        lastHeartbeat = System.currentTimeMillis();
                    }

                    //收到生成二维码任务
                    JSONArray qrTask = CommFunction.getArgument(message,"ReceiveQRTaskFromServer");
                    LogUtils.sendmsg(context, "收到生成二维码任务:"+message, true);
                    if(qrTask!=null){
                        try {
                            switch (qrTask.get(0).toString().toLowerCase()){
                                case "wechat":
                                    LogUtils.sendmsg(context, "准备生成微信二维码...", true);
                                    PayUtils.getInstance().creatWechatQr(HKApplication.app, (int)qrTask.get(1), (String) qrTask.get(2));
                                    break;
                                case "alipay":
                                    LogUtils.sendmsg(context, "准备生成支付寶二维码...");
                                    String v = CommFunction.formateMessage(
                                            "ReceiptCodeFinished",
                                            "alipay",Configer.getInstance().getUserAlipay(),"",(String) qrTask.get(2),(int)qrTask.get(1)
                                    );

                                    PayUtils.getInstance().creatAlipayQr(context, (int)qrTask.get(1), (String) qrTask.get(2));
                                    LogUtils.sendmsg(context, "收到要付錢了: "+v );
                                    sendMsg(v);
                                    break;
                                case "alibank":
                                    String v2 = CommFunction.formateMessage(
                                            "ReceiptCodeFinished",
                                            "alibank",Configer.getInstance().getUserAlipay(),"",(String) qrTask.get(2),(int)qrTask.get(1)
                                    );
                                    sendMsg(v2);
                                    break;
                                case "unionpay":
                                    LogUtils.sendmsg(context, "准备生成云闪付二维码...");
                                    PayUtils.getInstance().creatUnionpayQr(HKApplication.app, (int)qrTask.get(1), (String) qrTask.get(2));
                                    break;
                            }
                        }catch (Exception e){

                        }
                    }

                    final String state = CommFunction.getState(message);
                    if(state==null) return;
                    switch (state){
                        case "[40001]"://设备登录成功
                            LogUtils.sendmsg(context, "登陆成功", true);
                            mApiBll.checkQR(context);
                            UpdateAccounts();
                            break;
                        case "[10001]"://设备登录失败
                            LogUtils.sendmsg(context, "设备登陆失败");
                            break;
                        case "[10002]"://设备登录过期
                            LogUtils.sendmsg(context, "设备连接状态过期，需要关闭后重新连接");
                            //socketConnect.reconnect();
                            sendMsg(CommFunction.getInstance().getLoginString());
                            break;
                    }
                }

                @Override
                public void reconnect() {
                    Log.e("arik", "重连...");
                    super.reconnect();

                    socketNum -= 1;
                    if(socketNum == 0) {
                        final Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                reconnectToServer();
                            }
                        }, (int) ((Math.random() * 9 + 1) * 1000));
                    }
                }
                @Override
                public void onClose(int code, String reason, boolean remote) {
//                    LogUtils.sendmsg(context, "WebSocket onClose,reason:"+reason + "   " + remote + "   "+code);
                    socketNum -= 1;
                    if(socketNum == 0) {
                        final Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                reconnectToServer();
                            }
                        }, (int) ((Math.random() * 9 + 1) * 300));
                    }
                }
                @Override
                public void onError(Exception ex) {
                    LogUtils.show("onError:" + ex.toString());

//                    socketNum -= 1;
//                    if(socketNum == 0) {
//                        reconnectToServer();
//                    }
                }
            };

            socketConnect.connect();

        }
        else
        {
            LogUtils.sendmsg(this,"已建立连接，不重复操作");
        }
    }

    public void UpdateAccounts()
    {
        // 发送微信账号信息
//        sendMsg(CommFunction.getInstance().updateWechatStr(mIsWechatRunning));
        // 发送支付宝账号信息
        if (useWithServer) {
            sendMsg(CommFunction.getInstance().updateAlipayStr(mIsAlipayRunning));
        }

        // 发送云闪付账号信息
//        sendMsg(CommFunction.getInstance().updateUnionpayStr(mIsUnionpayRunning));

        /*// 发送微信账号信息
        String strAccountUpdateWechat = CommFunction.formateMessage("DeviceAccountUpdate","wechat",mIsWechatRunning.toString(),Configer.getInstance().getUserWechat());
        sendMsg(strAccountUpdateWechat);
        // 发送支付宝账号信息
        String strAccountUpdateAlipay = CommFunction.formateMessage("DeviceAccountUpdate","alipay",mIsAlipayRunning.toString(),Configer.getInstance().getUserAlipay());
        sendMsg(strAccountUpdateAlipay);
        // 发送云闪付账号信息*/
    }

    public void reconnectToServer()
    {
//        LogUtils.sendmsg(this, "reconnectToServer: 进入函数，lockReconnect为" + socketNum);
        if(socketNum != 0) return;

        socketNum = 1;
        // 延迟重连，避免过多连接
        socketConnect = null;

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                LogUtils.sendmsg(context, "reconnectToServer:重连服务器");
                try {
                    LogUtils.show( "reconnect: 尝试用WebSocket连接服务器");
//                    openSocket();
                } catch (Exception e) {
                    Log.e("arik", "reconnectToServer 出错:" + e.toString());
                    reconnectToServer();
                }
            }
        }, 5000);
    }

    public void closeSocket()
    {
        socketConnect.reconnect();
    }

    /**
     *发送消息
     */
    public void sendMsg(String msg) {
        try {
//            socketConnect.send(msg);
//            LogUtils.show( "发送信息: " + msg);
//            LogUtils.sendmsg(this, "发送信息: " + msg);
        } catch (Exception e) {
            LogUtils.sendmsg(this, "sendMsg error: " + e.toString(), true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN) //在ui线程执行
    public void onDataSynEvent(DataSynEvent event) {
        final String str = event.getMessage();
        LogUtils.show("收到的信息main:"+ str);

//        LogUtils.sendmsg(context, "收到的信息main:"+str);
        if(str!=null) sendMsg(str);
    }
}
