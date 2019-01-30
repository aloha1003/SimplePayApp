package com.sjk.simplepay.activity;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.sjk.simplepay.EchoService;
import com.sjk.simplepay.ServiceMain;
import com.sjk.simplepay.activity.fragments.ConfigureFragment;
import com.sjk.simplepay.activity.fragments.TransactionLogFragement;
import com.sjk.simplepay.module.presenter.MainPresenter;
import com.sjk.simplepay.module.view.IMainView;
import com.sjk.simplepay.po.Configer;
import com.sjk.simplepay.po.Constants;
import com.sjk.simplepay.utils.HTTPSTrustManager;
import com.sjk.simplepay.utils.LogUtils;
import com.sjk.simplepay.utils.PayUtils;
import com.sjk.tpay.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends BaseActivity implements IMainView {
    private static Context context;
    private MainPresenter mMainPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        HTTPSTrustManager.allowAllSSL();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.MSGRECEIVED_ACTION);
        filter.addAction(Constants.QUEUE_RECEIVE_ACTION);
        filter.addAction(Constants.CURRENT_AMOUNT_UPDATE);
//        filter.addAction(Constants.ALIPAY_CREAT_QR);
        registerReceiver(broadcastReceiver, filter);



    }

    public static Context getAppContext() {
        return context;
    }
    @Override
    protected void initViews(Bundle savedInstanceState) {
        mMainPresenter = new MainPresenter();
        mMainPresenter.attachView(this);
    }


    @Override
    public int getContentViewId() {
        return R.layout.act_main_v2;
    }



    @Override
    public void showOpenServiceDialog() {

    }

    @Override
    public void showErrorMessage(String e) {

    }

    @Override
    public void checkService() {

    }

    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    };

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.MSGRECEIVED_ACTION:
                    Boolean isDebug = intent.hasExtra("isDebug") ? intent.getExtras().getBoolean("isDebug") : false;
                    LogUtils.show("WTF"+intent.getExtras().toString());
                    sendmsg(intent.getExtras().getString("data"), isDebug);
                    break;
                case Constants.QUEUE_RECEIVE_ACTION:
                    doQueueAction(intent);
                    break;
                case Constants.CURRENT_AMOUNT_UPDATE:
                    updateCurrentAmount(intent.getExtras().getString("amount"));
                    break;
            }
        }
    };
    private void doQueueAction(Intent intent) {
        switch (intent.getExtras().getString("fun")) {
            case "dealAlipayWebTrade":
                PayUtils.dealAlipayWebTrade(MainActivity.getAppContext(), intent.getExtras().getString("cookie"));
                LogUtils.show("------------------dealAlipayWebTrade--------------");
//                PayUtils.getTradeInfoFromAPP(MainActivity.getAppContext(), intent.getExtras().getString("cookie"));
                break;
        }
    }
    @SuppressLint("HandlerLeak")
    public static Handler handler = new Handler()
    {
        public void handleMessage(Message paramAnonymousMessage)
        {
            String act = paramAnonymousMessage.getData().getString("action");
            String str;
            LogUtils.show("handleMessage Act" + act);
            switch (act) {
                case "updateCurrentAmount":
                    str = paramAnonymousMessage.getData().getString("currentAmount");
                    TransactionLogFragement.currentAmountView.setText("當前餘額:"+str);
                    ConfigureFragment.mEdtUserAlipayCurrentAmount.setText(str);
                    Configer.getInstance().setCurrentAmount(str);
                    break;
                case "log":
                    str = paramAnonymousMessage.getData().getString("log");
                    TextView v;

                    if (paramAnonymousMessage.getData().getBoolean("isDebug")) {

                        v = TransactionLogFragement.consoleDebug;
                    } else {
                        v = TransactionLogFragement.console;
                    }
                    LogUtils.show("content:" + str);
                    if ( v != null)
                    {
                        LogUtils.show("VT:"+v.getText());
                        if (v.getText() == null) {
                            v.setText(str);
                        }
                        if (v.getText().toString().length() <= 20000) {
                            v.append("\n" + str);
                        } else {
                            v.setText("日志定时清理完成...\n" + str);
                        }
                        v.setMovementMethod(ScrollingMovementMethod.getInstance());
                    } else {
                        LogUtils.show("竟然是空:" + str);
                    }
                    break;
            }

            for (;;)
            {
                super.handleMessage(paramAnonymousMessage);
                return;
            }
        }
    };
    public static void sendmsg(String paramString, Boolean isDebug)
    {
        Message localMessage = new Message();
        localMessage.what = 1;
        Bundle localBundle = new Bundle();
        Object localObject = new Date(System.currentTimeMillis());
        localObject = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)localObject);
        localBundle.putString("log", (String)localObject + ":" + paramString);
        localBundle.putBoolean("isDebug", isDebug);
        localBundle.putString("action", "log");
        localMessage.setData(localBundle);
        try
        {
            handler.sendMessage(localMessage);
            return;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void updateCurrentAmount(String amountText)
    {
//        LogUtils.show("updateCurrentAmount @ Main"+paramString );
//                    String currentAmountText = Configer.getInstance().getCurrentAmount();
//            BigDecimal currentAmount  = new BigDecimal(currentAmountText);
//            LogUtils.show("updateCurrentAmount currentAmount"+currentAmount);
//            BigDecimal amount  = new BigDecimal(amountText);
//            currentAmount = currentAmount.add(amount);
//            LogUtils.show("updateCurrentAmount currentAmount2"+currentAmount);
        Message localMessage = new Message();
        localMessage.what = 1;
        Bundle localBundle = new Bundle();
        localBundle.putString("action", "updateCurrentAmount");
        localBundle.putString("currentAmount", amountText);
        localMessage.setData(localBundle);
        try
        {
            handler.sendMessage(localMessage);
            return;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }




}
