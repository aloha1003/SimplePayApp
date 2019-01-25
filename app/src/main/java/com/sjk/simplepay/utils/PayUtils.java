package com.sjk.simplepay.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.sjk.simplepay.ActMain;
import com.sjk.simplepay.HKApplication;
import com.sjk.simplepay.bll.ApiBll;
import com.sjk.simplepay.po.AliBillList;
import com.sjk.simplepay.po.Configer;
import com.sjk.simplepay.po.Constants;
import com.sjk.simplepay.po.QrBean;
import com.sjk.simplepay.request.StringRequestGet;
import com.sjk.simplepay.HookMain;
import com.sjk.simplepay.ReceiverMain;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XposedHelpers;
import okhttp3.internal.http.HttpMethod;


/**
 * @ Created by Dlg
 * @ <p>TiTle:  PayUtils</p>
 * @ <p>Description: </p>
 * @ date:  2018/9/23
 */
public class PayUtils {

    //软件首次启动后，只处理支付最近xxx秒的订单，默认为只处理最近20分钟的订单
    private final static int ALIPAY_BILL_TIME = 1200 * 1000;

    private static PayUtils mPayUtils;

    public synchronized static PayUtils getInstance() {
        if (mPayUtils == null) {
            mPayUtils = new PayUtils();
        }
        return mPayUtils;
    }


    /**
     * @param context
     * @param money   金额，单位为分，范围1-30000000
     * @param mark    收款备注，最长30个字符，不能为空
     */
    public void creatWechatQr(Context context, Integer money, String mark) {
        if (money == null || TextUtils.isEmpty(mark)) {
            return;
        }
        if (mark.length() > 30 || money > 30000000 || money < 1) {
            return;
        }
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(HookMain.WECHAT_CREAT_QR);
        broadCastIntent.putExtra("mark", mark);
        broadCastIntent.putExtra("money", String.valueOf(money / 100.0f));
        context.sendBroadcast(broadCastIntent);
    }


    /**
     * 这里为了统一，要求就设置为和微信一样了。
     *
     * @param context
     * @param money   金额，单位为分，范围1-30000000
     * @param mark    收款备注，最长30个字符，不能为空
     */
    public void creatAlipayQr(Context context, Integer money, String mark) {
        if (money == null || TextUtils.isEmpty(mark)) {
            return;
        }
        if (mark.length() > 30 || money > 30000000 || money < 1) {
            return;
        }
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(HookMain.ALIPAY_CREAT_QR);
        broadCastIntent.putExtra("mark", mark);
        broadCastIntent.putExtra("money", String.valueOf(money / 100.0f));
        context.sendBroadcast(broadCastIntent);
    }

    /*云闪*/
    public void creatUnionpayQr(Context context, Integer money, String mark) {
        if (money == null || TextUtils.isEmpty(mark)) {
            return;
        }
        if (mark.length() > 30 || money > 30000000 || money < 1) {
            return;
        }
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(HookMain.UNIONPAY_CREAT_QR);
        broadCastIntent.putExtra("mark", mark);
        broadCastIntent.putExtra("money", String.valueOf(money / 100.0f));
        context.sendBroadcast(broadCastIntent);
    }


    /**
     * 通过支付宝APP得到web访问的Cookies数据
     * 因为全是static方法，还是很方便获取的
     *
     * @param paramClassLoader
     * @return 成功返回cookies，失败返回空文本，非null
     */
    public static String getAlipayCookieStr(ClassLoader paramClassLoader) {
        XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.common.transportext.biz.appevent.AmnetUserInfo", paramClassLoader), "getSessionid", new Object[0]);
        Context localContext = (Context) XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.common.transportext.biz.shared.ExtTransportEnv", paramClassLoader), "getAppContext", new Object[0]);
        if (localContext != null) {
            if (XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.common.helper.ReadSettingServerUrl", paramClassLoader), "getInstance", new Object[0]) != null) {
                String cookie =  (String) XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.common.transport.http.GwCookieCacheHelper", paramClassLoader), "getCookie", new Object[]{".alipay.com"});
                return cookie;
            }
            LogUtils.show("支付宝订单Cookies获取异常");
            sendmsg(localContext, "支付宝订单Cookies获取异常", true);
            return "";
        }
        LogUtils.show("支付宝Context获取异常");
        sendmsg(localContext, "支付宝订单Cookies获取异常2", true);
        return "";
    }

    public static String getAlipayCookieStr(ClassLoader paramClassLoader, final Context context) {
        String cookie = getAlipayCookieStr(paramClassLoader);
        Intent cookieIntent = new Intent()
                .putExtra("data", cookie)
                .setAction(Constants.SAVEALIPAYCOOKIE_ACTION);
        context.sendBroadcast(cookieIntent);
        return cookie;
    }



    /**
     * 通过网络请求获取最近的20个订单号
     * 把最近xx分钟内的订单传号传给getAlipayTradeDetail函数处理
     *
     * @param context
     * @param cookies
     */
    public static void dealAlipayWebTrade(final Context context, final String cookies) {
        long l = System.currentTimeMillis() + 200000;//怕手机的时间比支付宝慢了点，刚产生的订单就无法获取到
        String getUrl = "https://mbillexprod.alipay.com/enterprise/simpleTradeOrderQuery.json?beginTime=" + (l - 2592000000L)
                + "&limitTime=" + l + "&pageSize=20&pageNum=1&channelType=ALL";

        //改用 fundAccountDetail
//         getUrl = "https://mbillexprod.alipay.com/enterprise/fundAccountDetail.json?startDateInput=" + (l - 864000000L)
//                + "&endDateInput=" + l + "&pageSize=20&pageNum=1&channelType=ALL&queryEntrance=1&billUserId="+ Configer.getInstance().getUserAlipay()+"showType=0&type=trade&sortTarget=tradeTime&order=descend&sortType=0&_input_charset=gbk";

//        StringBuilder stringBuilder = new StringBuilder(httpUrl);
//        stringBuilder.append("?");
//        requestMap = StringUtils.signCreate(requestMap);
//        for(Map.Entry<String, String> entry : requestMap.entrySet()) {
//            String key = entry.getKey();
//            String value = entry.getValue();
//            stringBuilder = stringBuilder.append(key);
//            stringBuilder = stringBuilder.append("=");
//            stringBuilder = stringBuilder.append(URLEncoder.encode(value, "utf-8"));
//            stringBuilder = stringBuilder.append("&");
//        }
        StringRequestGet request = new StringRequestGet(getUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
//                    sendmsg(context, "dealAlipayWebTradeResponse:"+response);
                    LogUtils.show("dealAlipayWebTradeResponse："+response);
                    JSONObject jsonObject = JSON.parseObject(response);
                    List<AliBillList> aliBillLists = jsonObject.getJSONObject("result")
                            .getJSONArray("list").toJavaList(AliBillList.class);

                    SaveUtils saveUtils = new SaveUtils();
                    List<String> list = saveUtils.getJsonArray(SaveUtils.BILL_LIST_LAST, String.class);
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    for (AliBillList aliBillList : aliBillLists) {
                        //20分钟前的订单就忽略
                        if (System.currentTimeMillis() - aliBillList.getGmtCreateStamp().getTime() > ALIPAY_BILL_TIME) {
                            break;
                        }

                        //首次，或者上次一样，就返回
                        if (list.contains(aliBillList.getTradeNo())) {
                            LogUtils.show("最新的订单都已经处理过，那就直接返回");
//                            sendmsg(context, "最新的订单都已经处理过，那就直接返回");
                            continue;//最新的订单都已经处理过，那就直接返回
                        }
                        list.add(aliBillList.getTradeNo());
                        getAlipayTradeDetail(context, aliBillList.getTradeNo()
                                , formatMoneyToCent(aliBillList.getTotalAmount() + "")
                                , cookies);
                    }
                    if (list.size() > 100) {
                        list.subList(0, 50).clear();
                    }
                    saveUtils.putJson(SaveUtils.BILL_LIST_LAST, list).commit();
                } catch (Exception e) {
                    LogUtils.show("支付宝订单获取网络错误" + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                sendmsg(context, "支付宝订单获取网络错误"+error.getMessage(), true);
                LogUtils.show("支付宝订单获取网络错误，请不要设置代理");
            }
        });
        Date localDate = new Date(System.currentTimeMillis());
        String dataNow = new SimpleDateFormat("yyyy-MM-dd").format(localDate);
        String dataLastDay = new SimpleDateFormat("yyyy-MM-dd").format(new Date(l - 864000000L));

        request.addHeaders("Cookie", cookies)
                .addHeaders("Referer", "https://render.alipay.com/p/z/merchant-mgnt/simple-order.html?beginTime="
                        + dataLastDay + "&endTime=" + dataNow + "&fromBill=true&channelType=ALL");

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
        queue.start();
    }

    public static void startAlipayMonitor(final Context context){
        try {
            Timer timer=new Timer();
            //默认APP接口
            setAPI("PC");
            TimerTask timerTask=new TimerTask() {
                @Override
                public void run() {
                    final DBManager dbManager=new DBManager(context);
                    dbManager.saveOrUpdateConfig("time", System.currentTimeMillis()/1000+"");
                    if(getAPI().equals("APP")){
                        getTradeInfoFromAPP(context, "COOKIE");
                    }else if(getAPI().equals("PC")){
//                        getTradeInfoListFromPC(context);
                    }
                }
            };
            int triggerTime=10;
            timer.schedule(timerTask, 0, triggerTime*1000);
        } catch (Exception e) {
            sendmsg(context, "startAlipayMonitor->>"+e.getMessage());
        }
    }
    public static String getAPI(){
        String txt="";
        try {
            File file = new File(Environment.getExternalStorageDirectory(),"abc.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String readline = "";
            StringBuffer sb = new StringBuffer();
            while ((readline = br.readLine()) != null) {
                System.out.println("readline:" + readline);
                sb.append(readline);
            }
            br.close();
            txt=sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return txt;
    }
    public static void setAPI(String API){
        try {
//            LogToFile.i("payhelper", "切换接口："+API);
            File file = new File(Environment.getExternalStorageDirectory(),"abc.txt");
            if (!file.exists()) {
                file.createNewFile();;
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
            bw.write(API);
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void updateAlipayCookie(Context context,String cookie){
        DBManager dbManager=new DBManager(context);
        if(dbManager.getConfig("cookie").equals("null")){
            dbManager.addConfig("cookie", cookie);
        }else{
            dbManager.updateConfig("cookie", cookie);
        }
    }
    public static String getAlipayCookie(Context context){
        DBManager dbManager=new DBManager(context);
        String cookie=dbManager.getConfig("cookie");
        return cookie;
    }
    public static void getBill(final Context context,final String cookie,String alipayUserId){
        String api="APP";
//        LogToFile.i("payhelper", "getBill获取订单，当前使用API"+api);
        if(api.equals("APP")){
            getTradeInfoFromAPP(context, cookie);
        }else if(api.equals("PC")){
//            getTradeInfoFromPC(context, cookie, alipayUserId);
        }
    }
    public static String getTradeInfoFromAPP(final Context context , final String cookies) {
//        String  cookies=getAlipayCookie(context);
        String getUrl="https://mbillexprod.alipay.com/enterprise/walletTradeList.json?lastTradeNo=&lastDate=&pageSize=20&shopId=&_inputcharset=gbk&ctoken&source=&_ksTS="+System.currentTimeMillis()+"_49&_callback=&_input_charset=utf-8&sortTarget=tradeTime";
        sendmsg(context, "getUrl:"+getUrl);
        //改用 fundAccountDetail
//         getUrl = "https://mbillexprod.alipay.com/enterprise/fundAccountDetail.json?startDateInput=" + (l - 864000000L)
//                + "&endDateInput=" + l + "&pageSize=20&pageNum=1&channelType=ALL&queryEntrance=1&billUserId="+ Configer.getInstance().getUserAlipay()+"showType=0&type=trade&sortTarget=tradeTime&order=descend&sortType=0&_input_charset=gbk";

//        StringBuilder stringBuilder = new StringBuilder(httpUrl);
//        stringBuilder.append("?");
//        requestMap = StringUtils.signCreate(requestMap);
//        for(Map.Entry<String, String> entry : requestMap.entrySet()) {
//            String key = entry.getKey();
//            String value = entry.getValue();
//            stringBuilder = stringBuilder.append(key);
//            stringBuilder = stringBuilder.append("=");
//            stringBuilder = stringBuilder.append(URLEncoder.encode(value, "utf-8"));
//            stringBuilder = stringBuilder.append("&");
//        }
        StringRequestGet request = new StringRequestGet(getUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {


                    sendmsg(context, "dealAlipayWebTradeResponse:"+response);
                    LogUtils.show("dealAlipayWebTradeResponse："+response);
                     response = response.replace("/**/(", "").replace("})", "}");
                    JSONObject jsonObject = JSON.parseObject(response);
                    LogUtils.show("dealAlipayWebTradeResponse New Prs："+response);
                    String status=jsonObject.getString("status");
                    LogUtils.show("Status New Prs："+status);
                    if (status.equals("succeed")) {
                        List<AliBillList> aliBillLists = jsonObject.getJSONObject("result")
                                .getJSONArray("list").toJavaList(AliBillList.class);
//
                        SaveUtils saveUtils = new SaveUtils();
                        List<String> list = saveUtils.getJsonArray(SaveUtils.BILL_LIST_LAST, String.class);
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                        for (AliBillList aliBillList : aliBillLists) {
                            LogUtils.show("aliBillLists："+aliBillList.toString());
                            //20分钟前的订单就忽略
//                        if (System.currentTimeMillis() - aliBillList.getGmtCreateStamp().getTime() > ALIPAY_BILL_TIME) {
//                            break;
//                        }

                            //首次，或者上次一样，就返回
//                            if (list.contains(aliBillList.getTradeNo())) {
////                            sendmsg(context, "最新的订单都已经处理过，那就直接返回");
//                                continue;//最新的订单都已经处理过，那就直接返回
//                            }
//                            list.add(aliBillList.getTradeNo());
                            getAlipayTradeDetail(context, aliBillList.getTradeNo()
                                    , formatMoneyToCent(aliBillList.getTradeTransAmount() + "")
                                    , cookies);
                        }
                        if (list.size() > 100) {
                            list.subList(0, 50).clear();
                        }
                        saveUtils.putJson(SaveUtils.BILL_LIST_LAST, list).commit();
                    }
                } catch (Exception e) {
                    LogUtils.show("支付宝订单获取网络错误" + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                sendmsg(context, "支付宝订单获取网络错误"+error.getMessage(), true);
                LogUtils.show("支付宝订单获取网络错误，请不要设置代理");
            }
        });
        LogUtils.show("cookies:"+cookies);
        request.addHeaders("Cookie", cookies)
                .addHeaders("User-Agent", "Mozilla/5.0 (Linux; U; Android 7.1.1; zh-CN; 1605-A01 Build/NMF26F) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 UCBrowser/11.8.8.968 UWS/2.13.1.39 Mobile Safari/537.36 UCBS/2.13.1.39_180615144818 NebulaSDK/1.8.100112 Nebula AlipayDefined(nt:WIFI,ws:360|0|3.0) AliApp(AP/10.1.22.835) AlipayClient/10.1.22.835 Language/zh-Hans useStatusBar/true isConcaveScreen/false")
//                .addHeaders("X-Alipay-Client-Session", "check")
                .addHeaders("Referer", "https://render.alipay.com/p/z/merchant-mgnt/simple-order.html");

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
        queue.start();
        return "";
    }
    public static String getAlipayLoginId(ClassLoader classLoader) {
        String loginId="";
        try {
            Class<?> AlipayApplication = XposedHelpers.findClass("com.alipay.mobile.framework.AlipayApplication",
                    classLoader);
            Class<?> SocialSdkContactService = XposedHelpers
                    .findClass("com.alipay.mobile.personalbase.service.SocialSdkContactService", classLoader);
            Object instace = XposedHelpers.callStaticMethod(AlipayApplication, "getInstance");
            Object MicroApplicationContext = XposedHelpers.callMethod(instace, "getMicroApplicationContext");
            Object service = XposedHelpers.callMethod(MicroApplicationContext, "findServiceByInterface",
                    SocialSdkContactService.getName());
            LogUtils.show("MyAccountInfoModel START:");
            Object MyAccountInfoModel = XposedHelpers.callMethod(service, "getMyAccountBasicInfoModelByRpc");
			String userId = XposedHelpers.getObjectField(MyAccountInfoModel, "userId").toString();
            Gson gson = new Gson();
            loginId = gson.toJson(MyAccountInfoModel);
			LogUtils.show("MyAccountInfoModel:"+loginId);

//            loginId = XposedHelpers.getObjectField(MyAccountInfoModel, "loginId").toString();
        } catch (Exception e) {
            LogUtils.show("MyAccountInfoModel:error"+e.getLocalizedMessage());
        }
        return loginId;
    }



    public static void sendLoginId(String loginId, String type, Context context) {
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(Constants.LOGINIDRECEIVED_ACTION);
        broadCastIntent.putExtra("type", type);
        broadCastIntent.putExtra("loginid", loginId);
        context.sendBroadcast(broadCastIntent);
    }
    public static String getAlipayUserId(ClassLoader classLoader) {
        String userId="";
        try {
            Class<?> AlipayApplication = XposedHelpers.findClass("com.alipay.mobile.framework.AlipayApplication",
                    classLoader);
            Class<?> SocialSdkContactService = XposedHelpers
                    .findClass("com.alipay.mobile.personalbase.service.SocialSdkContactService", classLoader);
            Object instace = XposedHelpers.callStaticMethod(AlipayApplication, "getInstance");
            Object MicroApplicationContext = XposedHelpers.callMethod(instace, "getMicroApplicationContext");
            Object service = XposedHelpers.callMethod(MicroApplicationContext, "findServiceByInterface",
                    SocialSdkContactService.getName());
            Object MyAccountInfoModel = XposedHelpers.callMethod(service, "getMyAccountInfoModelByLocal");
            userId = XposedHelpers.getObjectField(MyAccountInfoModel, "userId").toString();
        } catch (Exception e) {
        }
        return userId;
    }
    /**
     * 获取指定订单号的订单信息，如果是已收款状态，则发送给服务器，
     * 失败的会自动加数据库以后补发送。
     *
     * @param context
     * @param tradeNo
     * @param money   单位为分
     * @param cookies
     */
    private static void getAlipayTradeDetail(final Context context, final String tradeNo, final int money, String cookies) {
        String getUrl = "https://tradeeportlet.alipay.com/wireless/tradeDetail.htm?tradeNo=" + tradeNo + "&source=channel&_from_url=https%3A%2F%2Frender.alipay.com%2Fp%2Fz%2Fmerchant-mgnt%2Fsimple-order._h_t_m_l_%3Fsource%3Dmdb_card";
//        sendmsg(context, "getAlipayTradeDetail Url"+getUrl);
        StringRequestGet request = new StringRequestGet(getUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    String html = response.toLowerCase();
                    LogUtils.show(html);
                    html = html.replace(" ", "")
                            .replace("\r", "")
                            .replace("\n", "")
                            .replace("\t", "");
                    html = getMidText(html, "\"id=\"j_logourl\"/>", "j_maskcode\"class=\"maskcodemain\"");

                    String tmp;
                    QrBean qrBean = new QrBean();
                    qrBean.setChannel(QrBean.ALIPAY);
                    qrBean.setOrder_id(tradeNo);

                    tmp = getMidText(html, "<divclass=\"am-flexbox\">当前状态</div>", "<divclass=\"am-list-itemtrade-info-item\">");
                    qrBean.setMark_buy(getMidText(tmp, "<divclass=\"trade-info-value\">", "</div>"));

                    tmp = getMidText(html, "<divclass=\"am-flexbox-item\">说</div><divclass=\"am-flexbox-item\">明", "<divclass=\"am-list-itemtrade-info-item\">");
                    qrBean.setMark_sell(getMidText(tmp, "<divclass=\"trade-info-value\">", "</div"));
                    sendmsg(context,"qrBean："+qrBean.toString(), true);
                    //tmp = getMidText(html, "am-flexbox-item\">金</div><divclass=\"am-flexbox-item\">额", "<divclass=\"am-list-itemtrade-info-item\">");
                    //Float money = Float.valueOf(getMidText(tmp, "<divclass=\"trade-info-value\">", "</div")) * 100;
                    qrBean.setMoney(money);

                    if (TextUtils.isEmpty(qrBean.getMark_sell())
                            || !qrBean.getMark_buy().contentEquals("已收款")) {
                        return;
                    }
                    ReceiverMain.setmLastSucc(0);
                    LogUtils.show("支付宝发送支付成功任务：" + tradeNo + "|" + qrBean.getMark_sell() + "|" + qrBean.getMoney());
                    sendmsg(context,"支付宝发送支付成功任务：" + tradeNo + "|" + qrBean.getMark_sell() + "|" + qrBean.getMoney(), true);
//                    new ApiBll().payQR(qrBean, context);
                } catch (Exception ignore) {
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                sendmsg(context, "支付宝订单详情获取错误"+tradeNo + "-->" + error.getMessage(), true);
                LogUtils.show("支付宝订单详情获取错误：" + tradeNo + "-->" + error.getMessage());
            }
        });

        request.addHeaders("Cookie", cookies);
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
        queue.start();
    }


    /**
     * 获取指定文本的两指定文本之间的文本
     *
     * @param text
     * @param begin
     * @param end
     * @return
     */
    public static String getMidText(String text, String begin, String end) {
        try {
            int b=0;
            if(begin!=""){
                b = text.indexOf(begin) + begin.length();
            }
            int e = text.indexOf(end, b);
            return text.substring(b, e);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
	
    /**
     * 格式化金钱，把元变为分的单位
     *
     * @param money
     * @return
     */
    public static Integer formatMoneyToCent(String money) {
        return Integer.valueOf(new DecimalFormat("#").format(Float.valueOf(money.trim()) * 100));
    }

    /**
     * 格式化金钱，把分变为元的单位
     *
     * @param money
     * @return
     */
    public static String formatMoneyToYuan(String money) {
        String yuan = new DecimalFormat("#.00").format(Float.valueOf(money.trim()) / 100f);
        if (yuan.startsWith(".")) {
            yuan = "0" + yuan;
        }
        return yuan;
    }

    public static void updateCurrentAmount(final Context context, String amountText)  {
        try {

            Intent broadCastIntent = new Intent()
                    .putExtra("data", amountText)
                    .setAction(Constants.CURRENT_AMOUNT_UPDATE_STEP_ONE);
            context.sendBroadcast(broadCastIntent);
        } catch (Exception ne) {
            LogUtils.show("updateCurrentAmountEror:"+ne.getLocalizedMessage());
        }

        return ;
    }

    public static void sendmsg(final Context context, String data) {
        Intent broadCastIntent = new Intent()
                .putExtra("data", data)
                .setAction(Constants.MSGRECEIVED_ACTION);
        context.sendBroadcast(broadCastIntent);
    }
    public static void sendmsg(final Context context, String data, Boolean isDebug) {
        Intent broadCastIntent = new Intent()
                .putExtra("data", data)
                ;
        if (isDebug) {
            broadCastIntent.putExtra("isDebug", true);
        }
        broadCastIntent.setAction(Constants.MSGRECEIVED_ACTION);
        context.sendBroadcast(broadCastIntent);
    }
    public static void twoPhaseCallDealAlipayWebTrade(final Context context, final String cookies) {
        Intent broadCastIntent = new Intent()
                .putExtra("fun", "dealAlipayWebTrade")
                .putExtra("cookie",  cookies)
                .setAction(Constants.QUEUE_RECEIVE_ACTION);
        context.sendBroadcast(broadCastIntent);
    }

    public static void startAPP()
    {
        try
        {
            Intent localIntent = new Intent(HKApplication.getInstance().getApplicationContext(), ActMain.class);
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            HKApplication.getInstance().getApplicationContext().startActivity(localIntent);
            return;
        }
        catch (Exception localException) {}
    }

    public static void startAPP(Context paramContext, String paramString)
    {
        try
        {
            paramContext.startActivity(paramContext.getPackageManager().getLaunchIntentForPackage(paramString));
            return;
        }
        catch (Exception ex) {}
    }

    public static void sendAppMsg(String paramString1, String paramString2, String paramString3, Context paramContext)
    {
        Intent localIntent = new Intent();
        if (paramString3.equals("alipay")) {
            localIntent.setAction(Constants.ALIPAYSTART_ACTION);
        }

            localIntent.putExtra("type", "qrset");
            localIntent.putExtra("mark", paramString2);
            localIntent.putExtra("money", paramString1);
            paramContext.sendBroadcast(localIntent);
//            if (paramString3.equals("wechat")) {
//                localIntent.setAction(Constants.WECHATSTART_ACTION);
//            } else if (paramString3.equals("qq")) {
//                localIntent.setAction(Constants.QQSTART_ACTION);
//            }

    }

    public static boolean isAppRunning(Context paramContext, String paramString)
    {
        List<ActivityManager.RunningTaskInfo> t = ((ActivityManager)paramContext.getSystemService("activity")).getRunningTasks(100);
        if (t.size() <= 0) {}
        Iterator it = t.iterator();
        do
        {
            while (!it.hasNext())
            {
                return false;
            }
        } while (!((ActivityManager.RunningTaskInfo) it.next()).baseActivity.getPackageName().equals(paramString));
        return true;
    }
}
