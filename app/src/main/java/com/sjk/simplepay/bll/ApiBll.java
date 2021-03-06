package com.sjk.simplepay.bll;


import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sjk.simplepay.ActMain;
import com.sjk.simplepay.ReceiverMain;
import com.sjk.simplepay.po.Constants;
import com.sjk.simplepay.po.OldQrBean;
import com.sjk.simplepay.request.FastJsonRequest;
import com.sjk.simplepay.HKApplication;
import com.sjk.simplepay.po.BaseMsg;
import com.sjk.simplepay.po.Configer;
import com.sjk.simplepay.po.QrBean;
import com.sjk.simplepay.utils.LogUtils;
import com.sjk.simplepay.utils.PayUtils;
import com.sjk.simplepay.utils.SaveUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sjk.simplepay.ServiceMain;
import com.sjk.simplepay.utils.StringUtils;
import java.sql.Timestamp;

import de.robv.android.xposed.XposedHelpers;

import static com.sjk.simplepay.HookMain.RECEIVE_QR_ALIPAY;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  ApiBll</p>
 * @ <p>Description: 和服务端交互的业务类</p>
 * @ date:  2018/9/21
 */
public class ApiBll {
    private RequestQueue mQueue;
    private static Context co;
    public String currentHandlerOrder = "";
    private static final String httpUrl = Configer.getInstance().getUrl() + "api/";
//    private static final String httpUrl = Configer.getInstance().getUrl() + "api/channel_notify/alipayFix";
    public ApiBll() {
        mQueue = Volley.newRequestQueue(HKApplication.app);
    }

    /**
     * 检查是否需要发送新二维码
     */
    public void checkQR(final Context context) {
//        if (!Configer.getInstance().getUrl().toLowerCase().startsWith("http")) {
//            return;//防止首次启动还没有配置，就一直去轮循
//        }
        co = context;
        Log.d("arik", "checkQR: 检查收款码任务");
        //增加判断，只有在启用状态才会轮循获取任务
        Map<String,String> apiRequestMap = new HashMap<>();
        apiRequestMap.put("alipay_account", Configer.getInstance().getUserAlipay());

        if(ServiceMain.mIsWechatRunning)
        {
            retrieveQrCodeReqest("wechat", apiRequestMap, context);
        }
        if(ServiceMain.mIsAlipayRunning)
        {
//            Intent broadCastIntent = new Intent()
////                    .putExtra("data", qrBean.toString())
//                    .putExtra("mark", "1131")
//                    .putExtra("money", "100")
//                    .setAction(Constants.ALIPAY_CREAT_QR);
//            co.sendBroadcast(broadCastIntent);
            retrieveQrCodeReqest("alipay", apiRequestMap, context );
        }
        if(ServiceMain.mIsUnionpayRunning)
        {
            retrieveQrCodeReqest("union", apiRequestMap, context);
        }
//        if(ServiceMain.mIsWechatRunning)mQueue.add(new FastJsonRequest(httpUrl + "retrieveOrderRequest?command=ask&sn=" + Configer.getInstance().getSN() + "&alipay_account=" + Configer.getInstance().getUserWechat(), succ, null, Request.Method.PATCH));
//        // 支付宝
//        if(ServiceMain.mIsAlipayRunning)mQueue.add(new FastJsonRequest(, succ, null, Request.Method.PATCH));
//        // 云闪付
//        if(ServiceMain.mIsUnionpayRunning)mQueue.add(new FastJsonRequest(httpUrl + "retrieveOrderRequest?command=ask&sn=" + Configer.getInstance().getSN() + "&alipay_account=" + Configer.getInstance().getUser_unionpay(), succ, null, Request.Method.PATCH));
////        mQueue.start();
    }

    private void retrieveQrCodeReqest(String payType, Map<String,String> requestMap, final Context context) {
        requestMap.put("payType", payType);
        mQueue.add(new FastJsonRequest(getRequestString(requestMap,"retrieveOrderRequest", context ), succ, null, Request.Method.POST));
    }


    /**
     * 发送服务器所需要的二维码字符串给服务器
     * 服务器如果有新订单，就会立马返回新的订单，手机端就不用再等下次轮循了
     *
     * @param url       当前二维码的字符串
     * @param mark_sell 收款方的备注
     */
    public void sendQR(String url, String mark_sell, final Context context)  {
        Map<String,String> apiRequestMap = new HashMap<>();
        apiRequestMap.put("command", "addqr");
        apiRequestMap.put("payUrl", url);
        apiRequestMap.put("orderInfo", url);
        apiRequestMap.put("c_o_id", mark_sell);

//        StringBuilder stringBuilder = new StringBuilder(httpUrl)
//                .append("?command=addqr")
//                .append("&url=")
//                .append(URLEncoder.encode(url, "utf-8"))
//                .append("&mark_sell=")
//                .append(URLEncoder.encode(mark_sell, "utf-8"))
//                .append("&sn=")
//                .append(URLEncoder.encode(Configer.getInstance().getSN(), "utf-8"));
        mQueue.add(new FastJsonRequest(getRequestString(apiRequestMap, "updateOrderRequest", context), succ, null, Request.Method.PATCH));

        dealTaskList(context);
    }

    public String getRequestString(Map<String,String> requestMap, String path, final Context context)  {
        StringBuilder stringBuilder = new StringBuilder(httpUrl);
        stringBuilder.append(path);
        stringBuilder.append("?");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        requestMap.put("_t", timestamp.toString());
        requestMap = StringUtils.signCreate(requestMap);
        for(Map.Entry<String, String> entry : requestMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            stringBuilder = stringBuilder.append(key);
            stringBuilder = stringBuilder.append("=");
            try {
                stringBuilder = stringBuilder.append(URLEncoder.encode(value, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                stringBuilder = stringBuilder.append("");
                PayUtils.sendmsg(context, e.getMessage(), true);
            }
            stringBuilder = stringBuilder.append("&");
        }
        LogUtils.show("取得連結收到查詢為：" + stringBuilder.toString());
        return stringBuilder.toString();
    }

    private String toMoney(int t) {
        DecimalFormat df = new DecimalFormat("0.0");
        String money = df.format((float)t/100);
        return money;
    }
    /**
     * 向服务器发送支付成功的消息
     * 如果因为一些原因，暂时没有通知成功，会保存任务，下次再尝试
     *
     * @param qrBean 订单详情信息
     */
    public void payQR(final QrBean qrBean, final Context context) {
        try {
            Map<String,String> apiRequestMap = new HashMap<>();
            apiRequestMap.put("command", "do");
            apiRequestMap.put("c_o_id", qrBean.getMark_sell());
            apiRequestMap.put("amount", toMoney(qrBean.getMoney()));
            apiRequestMap.put("p_o_id", qrBean.getOrder_id());
            apiRequestMap.put("alipay_account", Configer.getInstance().getUserAlipay());

//            url = new StringBuilder(httpUrl)
//                    .append("?command=do")
//                    .append("&mark_sell=")
//                    .append(URLEncoder.encode(qrBean.getMark_sell(), "utf-8"))
//                    .append("&money=")
//                    .append(qrBean.getMoney())
//                    .append("&order_id=")
//                    .append(URLEncoder.encode(qrBean.getOrder_id(), "utf-8"))
//                    .append("&mark_buy=")
//                    .append(URLEncoder.encode(qrBean.getMark_buy(), "utf-8"))
//                    .append("&sn=")
//                    .append(URLEncoder.encode(Configer.getInstance().getSN(), "utf-8"));


            String r = getRequestString(apiRequestMap, "channel_notify/alipay", context);
            PayUtils.sendmsg(context, "apiRequestMap:"+apiRequestMap.toString(), true);
            PayUtils.sendmsg(context, "getRequestString:"+r, true);
            mQueue.add(new FastJsonRequest(r, null, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("arik", "get出错: " + error);

                    if(error.networkResponse.data!=null) {
                        try {
                            String body = new String(error.networkResponse.data,"UTF-8");
                            PayUtils.sendmsg(context, "補單錯誤:"+body);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    if (error == null || error.networkResponse == null || error.networkResponse.statusCode < 500) {
                        //如果是服务器错误，自己检查代码，不然会一直发送成功订单造成多次支付！
//                        addTaskList(qrBean);
                    }
                }
            }, Request.Method.POST));
//            mQueue.start();
        } catch (Exception e) {
            PayUtils.sendmsg(context, "payQRError:"+e.getMessage(), true);
            addTaskList(qrBean);
        }
    }


    /**
     * 处理以前没有完成的任务
     */
    public void dealTaskList(final Context context) {
        SaveUtils saveUtils = new SaveUtils();
        List<QrBean> list = saveUtils.getJsonArray(SaveUtils.TASKL_LIST, QrBean.class);
        if (list != null) {
            //先清空任务，如果呆会儿在payQR里又失败的话，会自动又添加的。
            saveUtils.put(SaveUtils.TASKL_LIST, null).commit();
            for (QrBean qrBean : list) {
                payQR(qrBean, context);
            }
        }
    }


    /**
     * 添加未完成的任务列表
     * 一定要用static的synchronized方式，上面的dealTaskList在某情况下可能会有问题
     * 但个人方案就暂不考虑这么极端的情况了
     *
     * @param qrBean
     */
    private synchronized static void addTaskList(QrBean qrBean) {
        SaveUtils saveUtils = new SaveUtils();
        List<QrBean> list = saveUtils.getJsonArray(SaveUtils.TASKL_LIST, QrBean.class);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(qrBean);
        saveUtils.putJson(SaveUtils.TASKL_LIST, list).commit();
    }


    /**
     * 当询问是否需要生成二维码返回成功后的操作
     */
    private final Response.Listener<BaseMsg> succ = new Response.Listener<BaseMsg>() {
        @Override
        public void onResponse(BaseMsg response) {
            if (response == null) {
                return;
            }
            try {
                LogUtils.show("收到回應為：" + response.toString());
//            OldQrBean qrBean  = response.getData(OldQrBean.class);
//            //放棄用他的response.getData, 直接用JSON.parse去做就好
//            JsonElement jelement = new JsonParser().parse(response.toString());
//            JsonObject jobject = jelement.getAsJsonObject();
//            JsonObject qrBean = jobject.getAsJsonObject("data");
//
////            QrBean qrBean = response.getData(QrBean.class);
                QrBean qrBean = response.getData(QrBean.class);
                LogUtils.show("收到回應為oldQrBean：" + qrBean.toString());
                if (qrBean != null && qrBean.getMoney() > 0 && !TextUtils.isEmpty(qrBean.getMark_sell())) {
                    if (currentHandlerOrder.equals(qrBean.getMark_sell())) {
                        LogUtils.show(currentHandlerOrder+"還沒處理完");
                        return ;
                    }
                    currentHandlerOrder = qrBean.getMark_sell();
                    LogUtils.show("服务器需要新二维码：" + toMoney(qrBean.getMoney()) + "|" + qrBean.getMark_sell() + "|" + qrBean.getChannel());
                    if (qrBean.getChannel().contentEquals(QrBean.WECHAT)) {
                        PayUtils.getInstance().creatWechatQr(HKApplication.app, qrBean.getMoney()
                                , qrBean.getMark_sell());
                    } else if (qrBean.getChannel().contentEquals(QrBean.ALIPAY)) {
                        Intent broadCastIntent = new Intent()
                                .putExtra("data", qrBean.toString())
                                .putExtra("mark", qrBean.getMark_sell())
                                .putExtra("money", "0.1")
//                                .putExtra("money", toMoney(qrBean.getMoney()))
                                .setAction(Constants.ALIPAY_CREAT_QR);
                        co.sendBroadcast(broadCastIntent);
//                    Intent broadCastIntent = new Intent()
//                            .putExtra("data", qrBean.toString())
//                            .putExtra("mark", qrBean.getOrderInfo())
//                            .putExtra("money", qrBean.getAmount())
//                            .setAction(Constants.ALIPAY_CREAT_QR);
//                    co.sendBroadcast(broadCastIntent);
//                    Intent localIntent = new Intent(co, XposedHelpers.findClass("com.alipay.mobile.payee.ui.PayeeQRSetMoneyActivity", co.getClassLoader()));
//                    localIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
//                    localIntent.putExtra("mark", qrBean.getOrderInfo())
//                    localIntent.putExtra("money", qrBean.getAmount());
//                    co.startActivity(localIntent);

//                    PayUtils.getInstance().creatAlipayQr(co, qrBean.getAmount()
//                            , qrBean.getOrderInfo());
                    }
                }
            } catch (NullPointerException ne) {
                LogUtils.show("Null錯誤發生了：" + ne.toString());
            }  catch (Exception e) {
                LogUtils.show("錯誤發生了：" + e.toString());
            }

        }
    };

}
