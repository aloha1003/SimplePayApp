package com.sjk.simplepay;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sjk.simplepay.po.Configer;
import com.sjk.simplepay.utils.LogUtils;
import com.sjk.simplepay.utils.PayUtils;
import com.sjk.simplepay.utils.SaveUtils;
import com.sjk.tpay.R;

import net.mrbin99.laravelechoandroid.Echo;
import net.mrbin99.laravelechoandroid.EchoCallback;
import net.mrbin99.laravelechoandroid.EchoOptions;
import net.mrbin99.laravelechoandroid.channel.SocketIOPrivateChannel;

public class EchoService {
    private static EchoService echoServiceInstance;
    private static Context co;
    private static Echo echo = null;
    public static void printEchoServerResponse(Object... args) {
        PayUtils.sendmsg(co,"printEchoServerResponse Start", true);
        for (int i = 0 ; i< args.length; i++) {
            LogUtils.show(args[i].toString());
            PayUtils.sendmsg(co, args[i].toString(), true);
        }
        PayUtils.sendmsg(co,"printEchoServerResponse End", true);
    }
    public void init(final Context context) {
        co = context;
        if (echo == null || !echo.isConnected()) {
            LogUtils.show("EchoService Init");
            EchoOptions options = new EchoOptions();
            options.host = Configer.getInstance().getSocketUrl();
            echo = new Echo(options);
            echo.connect(new EchoCallback() {
                @Override
                public void call(Object... args) {
                    printEchoServerResponse("EchoService連線成功");
                }
            }, new EchoCallback() {
                @Override
                public void call(Object... args) {
                    printEchoServerResponse(args);
                }
            });
            //支付寶通道
//            echo.channel("OrderRequestCloud.alipay."+ Configer.getInstance().getUserAlipay())
//                    .listen("OrderRequestEvent", new EchoCallback() {
//                        @Override
//                        public void call(Object... args) {
//                            if (args.length >=2) {
//                                try {
//                                    JsonElement jelement = new JsonParser().parse(args[1].toString());
//                                    JsonObject jo = jelement.getAsJsonObject();
//                                    jo = jo.get("notice").getAsJsonObject();
//                                    Log.e("getMesg", "notice" + jo.get("desc"));
//                                    LogUtils.show("收到回應" + jo.get("desc"));
//                                } catch (Exception e) {
//                                    Log.e("getMesg", e.getMessage());
//                                    e.printStackTrace();
//                                }
//                            } else {
//                                LogUtils.show("收到回應" + args);
//                            }
//                        }
//                    });
            //雲閃付
            //App\Events\OrderRequestEvent
            LogUtils.show("join:OrderRequestCloud@"+ Configer.getInstance().getUser_unionpay());
            echo.channel("OrderRequestCloud@"+ Configer.getInstance().getUser_unionpay())
                    .listen("OrderRequest", new EchoCallback() {
                        @Override
                        public void call(Object... args) {
                            LogUtils.show("WTFOrderRequest");;
                            printEchoServerResponse(args);
                            JsonElement jelement = new JsonParser().parse(args[1].toString());
                            JsonObject jo = jelement.getAsJsonObject();
                            jo = jo.get("notice").getAsJsonObject();
//                            LogUtils.sendmsg(context, "准备生成云闪付二维码..."+jo.get("url").getAsString()+"付了"+jo.get("type").getAsInt());
                            //Integer money, String mark

                            PayUtils.getInstance().creatUnionpayQr(HKApplication.app, jo.get("type").getAsInt(), (String) jo.get("url").getAsString());
                        }
                    })
                    ;
        }
    }

    public synchronized static EchoService getInstance() {
        if (echoServiceInstance == null) {
            echoServiceInstance = new SaveUtils().getJson(SaveUtils.BASE, EchoService.class);
            if (echoServiceInstance == null) {
                echoServiceInstance = new EchoService();
            }
        }
        LogUtils.show("EchoService getInatce");
        return echoServiceInstance;
    }
    public void stop() {
        if (echo.isConnected()) {
            echo.disconnect();
        }
    }
}
