package com.sjk.simplepay;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sjk.simplepay.po.Constants;
import com.sjk.simplepay.po.QrBean;
import com.sjk.simplepay.utils.LogUtils;
import com.sjk.simplepay.utils.PayUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.sjk.simplepay.HookMain.RECEIVE_BILL_ALIPAY;
import static com.sjk.simplepay.HookMain.RECEIVE_BILL_ALIPAY2;
import static com.sjk.simplepay.HookMain.RECEIVE_QR_ALIPAY;
import static com.sjk.simplepay.HookMain.MSGRECEIVED_ACTION;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  HookAlipay</p>
 * @ <p>Description:支付宝的主要HOOK类</p>
 * @ date:  2018/9/28 22:57
 * @ QQ群：524901982
 */
public class HookAlipay {

    /**
     * 主要的hook函数
     *
     * @param appClassLoader
     * @param context
     */
    public void hook(ClassLoader appClassLoader, final Context context) {
        try {
            hookSafeCheck(appClassLoader);
            hookBill(appClassLoader, context);
            hookQRCreat(appClassLoader, context);
            hookQRWindows(appClassLoader);
            hookTest(appClassLoader,context);
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }

    private void hookTest(final ClassLoader classLoader, final Context context) {
        XposedHelpers.findAndHookMethod(TextView.class, "setText",CharSequence.class, TextView.BufferType.class, boolean.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
//                LogUtils.show("开始hook"+ "2、获得数据" + param.args[0]);
            }

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String text = (String) param.args[0];

//                if("0.00".equals(text)){
//                    param.args[0] = "10000000.00";
//                }if("可用余额 0.00元".equals(text)){
//                    param.args[0] = "可用余额 10000000.00元";
//                }
//                LogUtils.show("开始hook"+"3、获得数据！" + param.args[0]);
            }
        });
        XposedHelpers.findAndHookMethod("android.app.NotificationManager"
                , classLoader, "notify"
                , String.class, int.class, Notification.class
                , new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("methodHookParam.args:  " + Arrays.toString(param.args));
                        //通过param拿到第三个入参notification对象
                        Notification notification = (Notification) param.args[2];
                        //获得包名
                        String aPackage = notification.contentView.getPackage();
                        String title = "--";
                        String text = "--";
                        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            Bundle bundle = notification.extras;
                            title = (String) bundle.get("android.title");
                            text = (String) bundle.get("android.text");
                        }
                        LogUtils.show("收到通知"+ param.toString());
                        //    if ("com.tencent.mobileqq".equals(aPackage)) {
                        if ("com.eg.android.AlipayGphone".equals(aPackage)
                                && title.equals("支付宝消息") &&
                                text.endsWith("已成功向你转了1笔钱")) {
                            param.setResult(null);
                            return;
                        }
                        super.beforeHookedMethod(param);
                    }
                });
    }
    /**
     * 开始Hook二维码创建窗口，目的是为了创建生成二维码
     *
     * @param appClassLoader
     * @throws Exception
     */
    private void hookQRWindows(final ClassLoader appClassLoader) {
        XposedHelpers.findAndHookMethod("com.alipay.mobile.payee.ui.PayeeQRSetMoneyActivity"
                , appClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        LogUtils.show("Hook到支付宝窗口");

                        ((Activity) param.thisObject).getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

                        Intent intent = ((Activity) param.thisObject).getIntent();
                        String mark = intent.getStringExtra("mark");
                        String money = intent.getStringExtra("money");

                        //下面两行其实可以不要的
                        Field markField = XposedHelpers.findField(param.thisObject.getClass(),
                                "g");
                        markField.set(param.thisObject, money);


                        Field jinErField = XposedHelpers.findField(param.thisObject.getClass(), "b");
                        final Object jinErView = jinErField.get(param.thisObject);
                        Field beiZhuField = XposedHelpers.findField(param.thisObject.getClass(), "c");
                        final Object beiZhuView = beiZhuField.get(param.thisObject);
                        //设置支付宝金额和备注
                        XposedHelpers.callMethod(jinErView, "setText", money);
                        XposedHelpers.callMethod(beiZhuView, "setText", mark);

                        //点击确认，这个模拟方案本来淘汰了，觉得直接用Call更稳定，但其实performClick也是相当于call，并不是传统模拟
                        Field quRenField = XposedHelpers.findField(param.thisObject.getClass(), "e");
                        final Button quRenButton = (Button) quRenField.get(param.thisObject);
                        quRenButton.performClick();

                        //方式二是直接调用函数，直接可以替换上面模拟方案，各有各好吧。
                        //XposedHelpers.callMethod(param.thisObject, "a");
                    }
                });
    }


    /**
     * hook二维码生成后操作，目的是为了得到创建二维码的url链接
     *
     * @param appClassLoader
     * @param context
     */
    private void hookQRCreat(final ClassLoader appClassLoader, final Context context) {
        XposedHelpers.findAndHookMethod("com.alipay.mobile.payee.ui.PayeeQRSetMoneyActivity", appClassLoader, "a",
                XposedHelpers.findClass("com.alipay.transferprod.rpc.result.ConsultSetAmountRes", appClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        PayUtils.sendmsg(context, "Hook 二维码建成");
                        LogUtils.show("Hook 二维码建成");
//                        String cookie = PayUtils.getAlipayCookieStr(appClassLoader, context);
//                        String id = PayUtils.getAlipayLoginId(appClassLoader);
//                        PayUtils.twoPhaseCallDealAlipayWebTrade(context, cookie);
//                        PayUtils.sendmsg(context, "Id:"+id, true);
//                        PayUtils.sendmsg(context, "Cookie:"+cookie, true);
                        Field moneyField = XposedHelpers.findField(param.thisObject.getClass(), "g");
                        String money = (String) moneyField.get(param.thisObject);

                        Field markField = XposedHelpers.findField(param.thisObject.getClass(), "c");
                        Object markObject = markField.get(param.thisObject);
                        String mark = (String) XposedHelpers.callMethod(markObject, "getUbbStr");

                        Object consultSetAmountRes = param.args[0];
                        Field consultField = XposedHelpers.findField(consultSetAmountRes.getClass(), "qrCodeUrl");
                        String payurl = (String) consultField.get(consultSetAmountRes);
                        if(money==null || mark==null || payurl==null) return;

                        Field consultField2 = XposedHelpers.findField(consultSetAmountRes
                                .getClass(), "printQrCodeUrl");
                        String payurloffline = (String) consultField2.get(consultSetAmountRes);

                        LogUtils.show("支付宝生成二维码：" + money + "|"
                                + mark + "|" + payurl + "|" + payurloffline);

                        QrBean qrBean = new QrBean();
                        qrBean.setChannel(QrBean.ALIPAY);
                        qrBean.setMark_sell(mark);
                        qrBean.setUrl(payurl);
                        PayUtils.sendmsg(context, "支付宝生成二维码:"+qrBean.toString());
                        Intent broadCastIntent = new Intent()
                                .putExtra("data", qrBean.toString())
                                .setAction(RECEIVE_QR_ALIPAY);
                        context.sendBroadcast(broadCastIntent);
                        XposedHelpers.callMethod(param.thisObject, "finish", new Object[0]);
                    }
                });
    }

    /**
     * 当支付宝收到订单时，Hook订单信息
     *
     * @param appClassLoader
     * @param context
     */
    private void hookBill(final ClassLoader appClassLoader, final Context context) {
        XposedHelpers.findAndHookMethod(
                "com.alipay.android.phone.messageboxstatic.biz.dao.ServiceDao", appClassLoader,
                "insertMessageInfo",
                XposedHelpers.findClass(
                        "com.alipay.android.phone.messageboxstatic.biz.db.ServiceInfo",
                        appClassLoader), String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {

                            LogUtils.show("支付宝收到支付订单1");
                            Log.d("arik", "支付宝收到支付订单1");

                            String str = PayUtils.getMidText((String) XposedHelpers.callMethod(param.args[0],
                                    "toString", new Object[0]), "extraInfo='", "'").replace("\\", "");
                            PayUtils.sendmsg(context, "支付宝收到支付订单1:"+str, true);
                            if ((str.contains("你的银行卡")) || (str.contains("通过支付宝")) || str.contains("收款金额")) {
                                String cookie = PayUtils.getAlipayCookieStr(appClassLoader);
//                                PayUtils.sendmsg(context, "Cookie内容:"+cookie);
//                                PayUtils.twoPhaseCallDealAlipayWebTrade(context, cookie);
//                                PayUtils.test(context, cookie);
                                String money = PayUtils.getMidText(cookie,"转账","元已到账");
                                String mark = PayUtils.getMidText(cookie,"{\"assistMsg1\":\"","通过支付宝");
                                PayUtils.updateCurrentAmount(context, money);
                                QrBean qrBean = new QrBean();
                                qrBean.setMoney(PayUtils.formatMoneyToCent(money));
                                qrBean.setMark_sell(mark);
                                qrBean.setChannel(QrBean.ALIPAYBANK);
//
                                Intent broadCastIntent = new Intent()
                                        .putExtra("data", qrBean.toString())
                                        .setAction(RECEIVE_BILL_ALIPAY);
                                context.sendBroadcast(broadCastIntent);
                            }
                        } catch (Exception i) {
                            i.printStackTrace();
                            LogUtils.show("支付宝收到支付订单出错" + i.getMessage());
                        }
                    }
                }
        );
        Class<?> insertTradeMessageInfo = XposedHelpers.findClass("com.alipay.android.phone.messageboxstatic.biz.dao.TradeDao", appClassLoader);
        XposedBridge.hookAllMethods(insertTradeMessageInfo, "insertMessageInfo", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    LogUtils.show("支付宝收到支付订单2");
                    Object object = param.args[0];
                    String MessageInfo = (String) XposedHelpers.callMethod(object, "toString");
                    LogUtils.show(MessageInfo);
                    String content = PayUtils.getMidText(MessageInfo, "content='", "'");
//                    PayUtils.sendmsg(context, "支付宝收到支付订单2:"+content);
                    if (content.contains("二维码收款") || content.contains("收到一笔转账")
                            || content.contains("成功收款") || content.contains("收款金额")) {
                        String cookie = PayUtils.getAlipayCookieStr(appClassLoader);
//                        PayUtils.sendmsg(context, "Cookie内容:"+cookie);
//                        PayUtils.twoPhaseCallDealAlipayWebTrade(context, cookie);
                        JSONObject jsonObject = JSON.parseObject(content);
                        String money = jsonObject.getString("content");
                        String mark = jsonObject.getString("assistMsg2");
                        money = money.replace("￥", "").replace(" ", "");
                        String tradeNo = PayUtils.getMidText(MessageInfo, "tradeNO=", "&");
                        if (TextUtils.isEmpty(mark)) {
                            return;
                        }
                        LogUtils.show("收到支付宝支付订单：" + tradeNo + "|" + money + "|" + mark);
                        PayUtils.sendmsg(context, "收到支付宝支付订单: 訂單號:"+ mark +" 金額:"+money+", 支付寶訂單號"+tradeNo );
                        PayUtils.updateCurrentAmount(context, money);
                        QrBean qrBean = new QrBean();
                        qrBean.setOrder_id(tradeNo);
                        qrBean.setMoney(PayUtils.formatMoneyToCent(money));
                        qrBean.setMark_sell(mark);
                        qrBean.setChannel(QrBean.ALIPAY);

                        Intent broadCastIntent = new Intent()
                                .putExtra("data", qrBean.toString())
                                .setAction(RECEIVE_BILL_ALIPAY2);
                        context.sendBroadcast(broadCastIntent);
                        PayUtils.sendmsg(context, qrBean.toString(), true);
                    }
                } catch (Exception e) {
                    LogUtils.show("支付宝订单获取错误：" + e.getMessage());
                }
                super.beforeHookedMethod(param);
            }
        });

        // hook获取loginid
//        XposedHelpers.findAndHookMethod("com.alipay.mobile.quinox.LauncherActivity", appClassLoader, "onResume",
//                new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    LogUtils.show("WTF:LauncherActivity");
//                        String loginid=PayUtils.getAlipayLoginId(appClassLoader);
//                        PayUtils.sendLoginId(loginid, "alipay", context);
//                    }
//                });

        //拦截“人气大爆发，一会再试试”
        XposedHelpers.findAndHookMethod("com.alipay.mobile.antui.basic.AUDialog", appClassLoader, "show",
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Context mContext=(Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        if (mContext.getClass().getSimpleName().equals("PayeeQRSetMoneyActivity")){
                            XposedHelpers.setObjectField(param.thisObject, "mContext", null);
                        }
                    }

                });
    }


    /**
     * Hook掉支付宝的多项安全检查
     *
     * @param classLoader
     */
    private void hookSafeCheck(ClassLoader classLoader) {
        try {
            Class<?> securityCheckClazz = XposedHelpers.findClass("com.alipay.mobile.base.security.CI", classLoader);
            XposedHelpers.findAndHookMethod(securityCheckClazz, "a", String.class, String.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object object = param.getResult();
                    XposedHelpers.setBooleanField(object, "a", false);
                    param.setResult(object);
                    super.afterHookedMethod(param);
                }
            });
            XposedHelpers.findAndHookMethod(securityCheckClazz, "a", Class.class, String.class, String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return (byte) 1;
                }
            });
            XposedHelpers.findAndHookMethod(securityCheckClazz, "a", ClassLoader.class, String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    return (byte) 1;
                }
            });
            XposedHelpers.findAndHookMethod(securityCheckClazz, "a", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return false;
                }
            });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }




}
