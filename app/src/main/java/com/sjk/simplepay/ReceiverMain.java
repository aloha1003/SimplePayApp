package com.sjk.simplepay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.sjk.simplepay.bll.ApiBll;
import com.sjk.simplepay.po.Configer;
import com.sjk.simplepay.po.Constants;
import com.sjk.simplepay.po.QrBean;
import com.sjk.simplepay.utils.LogUtils;
import com.sjk.simplepay.utils.PayUtils;

import static com.sjk.simplepay.HookMain.RECEIVE_BILL_ALIPAY;
import static com.sjk.simplepay.HookMain.RECEIVE_BILL_ALIPAY2;
import static com.sjk.simplepay.HookMain.RECEIVE_BILL_UNIONPAY;
import static com.sjk.simplepay.HookMain.RECEIVE_BILL_WECHAT;
import static com.sjk.simplepay.HookMain.RECEIVE_QR_ALIPAY;
import static com.sjk.simplepay.HookMain.RECEIVE_QR_WECHAT;
import static com.sjk.simplepay.HookMain.RECEIVE_QR_UNIONPAY;
import java.math.BigDecimal;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  ReceiverMain</p>
 * @ <p>Description: 当HOOK之后的处理结果，只能用此广播来接受，不然很多数据不方便共享的</p>
 * @ date:  2018/09/22
 */
public class ReceiverMain extends BroadcastReceiver {
    private ApiBll mApiBll;
    public static boolean mIsInit = false;
    private static String lastMsg = "";//防止重启接收广播，一定要用static
    private static long mLastSucc = 0;
    private static String cook = "";


    public ReceiverMain() {
        super();
        mIsInit = true;
        LogUtils.show("Receiver创建成功！");
        mApiBll = new ApiBll();
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            String data = intent.getStringExtra("data");
            //Log.d("arik", "测试接收广播：" + data);

            if (lastMsg.contentEquals(data) && !RECEIVE_BILL_ALIPAY.contentEquals(intent.getAction())) {//暂时不管
                return;
            }
            lastMsg = data;

            switch (intent.getAction()) {
                case RECEIVE_QR_WECHAT:
                    QrBean qrBean = JSON.parseObject(data, QrBean.class);
                    mApiBll.sendQR(qrBean.getUrl(), qrBean.getMark_sell(), context);
                    Log.d("arik", "发送了QR");
                    break;
                case RECEIVE_BILL_WECHAT:
                    qrBean = JSON.parseObject(data, QrBean.class);
                    mApiBll.payQR(qrBean, context);
                    break;
                case RECEIVE_QR_ALIPAY:
                    Log.d("arik", "发送了支付寶QR");
                    qrBean = JSON.parseObject(data, QrBean.class);
                    mApiBll.sendQR(qrBean.getUrl(), qrBean.getMark_sell(), context);
                    break;
                case RECEIVE_BILL_ALIPAY2:
                    qrBean = JSON.parseObject(data, QrBean.class);
                    LogUtils.show("支付宝收款成功2：" + qrBean.getOrder_id() + "|" + qrBean.getMark_sell() + "|" + qrBean.getMoney());
                    mApiBll.payQR(qrBean, context);
                    break;
                case Constants.CURRENT_AMOUNT_UPDATE_STEP_ONE:
                    String currentAmountText = Configer.getInstance().getCurrentAmount();
                    BigDecimal currentAmount  = new BigDecimal(currentAmountText);
                    LogUtils.show("updateCurrentAmount currentAmount"+currentAmount);
                    BigDecimal amount  = new BigDecimal(data);
                    currentAmount = currentAmount.add(amount);
                    Intent broadCastIntent = new Intent()
                            .putExtra("amount", currentAmount.toString())
                            .setAction(Constants.CURRENT_AMOUNT_UPDATE);
                    context.sendBroadcast(broadCastIntent);
                    break;
                case RECEIVE_BILL_ALIPAY:
                    cook = data;
                    mLastSucc = System.currentTimeMillis();
                    //PayUtils.dealAlipayWebTrade(context, data);
                    qrBean = JSON.parseObject(data, QrBean.class);
                    Log.d("arik", "支付宝银行收款：" + qrBean.getMark_sell() + "|" + qrBean.getMoney());
                    mApiBll.payQR(qrBean, context);
                    break;
                case RECEIVE_QR_UNIONPAY:
                    qrBean = JSON.parseObject(data, QrBean.class);
                    //mApiBll.payQR(qrBean);
                    mApiBll.sendQR(qrBean.getUrl(), qrBean.getMark_sell(), context);
                    Log.d("arik", "发送了云闪付QR");
                    break;
                case Constants.LOGINIDRECEIVED_ACTION:
                    String loginid = intent.getStringExtra("loginid");
                    String type = intent.getStringExtra("type");
                    PayUtils.sendmsg(context, "当前登录支付宝账号：" + loginid);
                    break;
                case RECEIVE_BILL_UNIONPAY:
                    cook = data;
                    mLastSucc = System.currentTimeMillis();
                    //PayUtils.dealAlipayWebTrade(context, data);
                    qrBean = JSON.parseObject(data, QrBean.class);
                    Log.d("arik", "云闪付收款：" + qrBean.getMark_sell() + "|" + qrBean.getMoney());
                    mApiBll.payQR(qrBean, context);
                    break;
                case Constants.SAVEALIPAYCOOKIE_ACTION:
                    PayUtils.updateAlipayCookie(context, lastMsg);
                    break;
            }

            switch (intent.getAction()) {
                case RECEIVE_BILL_WECHAT:
                case RECEIVE_BILL_ALIPAY:
                    LogUtils.show("处理未完成的支付宝订单通知");
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mApiBll.dealTaskList(context);
                        }
                    }, 5000);
            }
        } catch (Exception e) {
            LogUtils.show(e.getMessage());
        }
    }

    public static String getCook() {
        return cook == null ? "" : cook;
    }

    public static void setCook(String cook) {
        ReceiverMain.cook = cook;
    }

    public static long getmLastSucc() {
        return mLastSucc;
    }

    public static void setmLastSucc(long mLastSucc) {
        ReceiverMain.mLastSucc = mLastSucc;
    }

}
