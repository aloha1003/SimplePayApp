package com.sjk.simplepay.activity.fragments;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.hdl.logcatdialog.LogcatDialog;
import com.sjk.simplepay.ActMain;
import com.sjk.simplepay.HookMain;
import com.sjk.simplepay.ServiceMain;
import com.sjk.simplepay.ServiceProtect;
import com.sjk.simplepay.activity.MainActivity;
import com.sjk.simplepay.po.CommFunction;
import com.sjk.simplepay.po.Configer;
import com.sjk.simplepay.po.Constants;
import com.sjk.simplepay.utils.LogUtils;
import com.sjk.simplepay.utils.PayUtils;
import com.sjk.simplepay.utils.ReceiveUtils;
import com.sjk.simplepay.utils.SaveUtils;
import com.sjk.tpay.BuildConfig;
import com.sjk.tpay.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConfigureFragment extends Fragment {
    public static EditText mEdtUrl;


    public static  EditText mEdtToken;

    public static  EditText mEdtSN;

    public static  EditText mEdtUserWechat;

    public static  EditText mEdtUserAlipay;
    public static  EditText mEdtUserAlipayCurrentAmount;

    private Button mBtnSubmit;

    private Button mBtnWechat;

    private Button mBtnAlipay;

    private Button mBtnUnionpay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.act_main, container, false);


        return rootView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mEdtUrl = ((TextInputLayout) getView().findViewById(R.id.edt_act_main_url)).getEditText();
        mEdtToken = ((TextInputLayout) getView().findViewById(R.id.edt_act_main_token)).getEditText();
        mEdtSN = ((TextInputLayout) getView().findViewById(R.id.edt_act_main_sn)).getEditText();
        mEdtSN.setText(android.os.Build.SERIAL);
        mEdtUserWechat = ((TextInputLayout) getView().findViewById(R.id.edt_act_main_user_wechat)).getEditText();
        mEdtUserAlipay = ((TextInputLayout) getView().findViewById(R.id.edt_act_main_user_alipay)).getEditText();
        mEdtUserAlipayCurrentAmount = ((TextInputLayout) getView().findViewById(R.id.edt_act_main_alipay_current_amount)).getEditText();
        mBtnSubmit = getView().findViewById(R.id.btn_submit);
        mBtnSubmit.setOnClickListener(btnClickListen);

        mBtnWechat = getView().findViewById(R.id.btn_wechat);
        mBtnWechat.setOnClickListener(btnClickListen);
        mBtnAlipay = getView().findViewById(R.id.btn_alipay);
        mBtnAlipay.setOnClickListener(btnClickListen);
        mBtnUnionpay = getView().findViewById(R.id.btn_unionpay);
        mBtnUnionpay.setOnClickListener(btnClickListen);
        ((TextView) getView().findViewById(R.id.txt_version)).setText("Ver：" + BuildConfig.VERSION_NAME);
        getPermissions();
    }
    private Button.OnClickListener btnClickListen = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch(v.getId()) {
                case R.id.btn_submit:
                    // Which is supposed to be called automatically in your
                    // activity, which has now changed to a fragment.
                    clsSubmit(v);
                    break;
                case R.id.btn_alipay:
                    clsAlipayPay(v);
                    break;
            }
        }
    };
    /**
     * 切换APP服务的运行状态
     *
     * @return
     */
    private boolean changeStatus() {
        ServiceMain.mIsRunning = !ServiceMain.mIsRunning;
        String statusText = ServiceMain.mIsRunning ? "停止服务" : "确认配置并启动";
        mBtnSubmit.setText(statusText);
        if(!ServiceMain.mIsRunning)
        {
            if(ServiceMain.mIsWechatRunning) changeWechatStatus();
            if(ServiceMain.mIsAlipayRunning) changeAlipayStatus();
            if(ServiceMain.mIsUnionpayRunning) changeUnionpayStatus();
        }

        return ServiceMain.mIsRunning;
    }
    private boolean changeWechatStatus() {
        ServiceMain.mAccountChanged = true;
        ServiceMain.mIsWechatRunning = !ServiceMain.mIsWechatRunning;
        mBtnWechat.setText(ServiceMain.mIsWechatRunning ? "停止微信服务" : "启动微信收款");
        return ServiceMain.mIsWechatRunning;
    }
    private boolean changeAlipayStatus() {
        ServiceMain.mAccountChanged = true;
        ServiceMain.mIsAlipayRunning = !ServiceMain.mIsAlipayRunning;
        mBtnAlipay.setText(ServiceMain.mIsAlipayRunning ? "停止支付宝服务" : "启动支付宝收款");
        return ServiceMain.mIsAlipayRunning;
    }
    private boolean changeUnionpayStatus() {
        ServiceMain.mAccountChanged = true;
        ServiceMain.mIsUnionpayRunning = !ServiceMain.mIsUnionpayRunning;
        mBtnUnionpay.setText(ServiceMain.mIsUnionpayRunning ? "停止云闪付" : "启动云闪付收款");
        return ServiceMain.mIsUnionpayRunning;
    }


    private PackageInfo getPackageInfo(String packageName) {
        PackageInfo pInfo = null;
        try {
            //通过PackageManager可以得到PackageInfo
            PackageManager pManager = MainActivity.getAppContext().getPackageManager();
            pInfo = pManager.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS);
            return pInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pInfo;
    }

    /**
     * 点确认配置的操作
     *
     * @param view
     */
    public void clsSubmit(View view) {
//        LogUtils.show( "clsSubmit");
//        PayUtils.updateCurrentAmount(getContext(), "120");
//        LogUtils.sendmsg(getContext(), "clsSubmit", true);
        if (getPackageInfo(HookMain.WECHAT_PACKAGE) != null
                && !getPackageInfo(HookMain.WECHAT_PACKAGE).versionName.contentEquals("6.7.2")) {
            Toast.makeText(getContext(), "微信版本不对！官方下载版本号：6.7.2", Toast.LENGTH_SHORT).show();
        }
        if (getPackageInfo(HookMain.ALIPAY_PACKAGE) != null
                && !getPackageInfo(HookMain.ALIPAY_PACKAGE).versionName.contentEquals("10.1.38.2139")) {
//            Toast.makeText(MainActivity.getAppContext(), "支付宝版本不对！官方下载版本号：10.1.38.2139", Toast.LENGTH_SHORT).show();
        }
        //10.1.35.828
        if (!changeStatus()) {
            return;
        }

        mEdtUrl.setText(mEdtUrl.getText().toString().trim());
        mEdtToken.setText(mEdtToken.getText().toString().trim());
        if (mEdtUrl.length() < 2 || mEdtToken.length() < 1
                || mEdtUserWechat.length() < 2 || mEdtUserAlipay.length() < 2) {
            Toast.makeText(MainActivity.getAppContext(), "请先输入正确配置！", Toast.LENGTH_SHORT).show();
            changeStatus();
            return;
        }
//        if(!mEdtUrl.getText().toString().startsWith("http"))
//        {
//            Toast.makeText(MainActivity.getAppContext(), "请输入正确的网址！", Toast.LENGTH_SHORT).show();
//            changeStatus();
//            return;
//        }
//        if (!mEdtUrl.getText().toString().endsWith("/")) {
//            mEdtUrl.setText(mEdtUrl.getText().toString() + "/");//保持以/结尾的网址
//        }

        //下面开始获取最新配置并启动服务。
//        Configer.getInstance()
//                .setUrl(mEdtUrl.getText().toString());
        Configer.getInstance()
                .setToken(mEdtToken.getText().toString());
        Configer.getInstance()
                .setSN(mEdtSN.getText().toString());
        Configer.getInstance()
                .setUserWechat(mEdtUserWechat.getText().toString());
        Configer.getInstance()
                .setUserAlipay(mEdtUserAlipay.getText().toString());
        //更新餘額
        Configer.getInstance()
                .setCurrentAmount(mEdtUserAlipayCurrentAmount.getText().toString());
        Intent localIntent = new Intent();
        localIntent.putExtra("amount", mEdtUserAlipayCurrentAmount.getText().toString());

        localIntent.setAction(Constants.CURRENT_AMOUNT_UPDATE);
        getContext().sendBroadcast(localIntent);

        //保存配置
        new SaveUtils().putJson(SaveUtils.BASE, Configer.getInstance()).commit();


        //有的手机就算已经静态注册服务还是不行启动，再手动启动一下。
        MainActivity.getAppContext().startService(new Intent(MainActivity.getAppContext(), ServiceMain.class));
        MainActivity.getAppContext().startService(new Intent(MainActivity.getAppContext(), ServiceProtect.class));

        //广播也再次注册一下。。。机型兼容。。。
        ReceiveUtils.startReceive();

        addStatusBar();

    }

    /**
     * 测试微信获取二维码的功能
     *
     * @param view
     */
    public void clsWechatPay(View view) {
        if(!ServiceMain.mIsRunning)
        {
            Toast.makeText(MainActivity.getAppContext(), "请确认配置信息后，再启动微信收款功能", Toast.LENGTH_SHORT).show();
            return;
        }
        /*if (!changeWechatStatus()) {
            return;
        }*/
        changeWechatStatus();

        CommFunction.getInstance().postEventBus(CommFunction.getInstance().updateWechatStr(ServiceMain.mIsWechatRunning));
        //CommFunction.getInstance().postEventBus("updateActive");

        /*Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(HookMain.RECEIVE_BILL_WECHAT);
        HKApplication.app.sendBroadcast(broadCastIntent);*/

//        String time = System.currentTimeMillis() / 1000 + "";
//        PayUtils.getInstance().creatWechatQr(this, 12, "test" + time);
    }


    /**
     * 测试支付宝获取二维码的功能
     *
     * @param view
     */
    public void clsAlipayPay(View view) {
        if(!ServiceMain.mIsRunning)
        {
            Toast.makeText(MainActivity.getAppContext(), "请确认配置信息后，再启动支付宝收款功能", Toast.LENGTH_SHORT).show();
            return;
        }

        /*if (!changeAlipayStatus()) {
            return;
        }*/
        changeAlipayStatus();

        CommFunction.getInstance().postEventBus(CommFunction.getInstance().updateAlipayStr(ServiceMain.mIsAlipayRunning));

//        Intent broadCastIntent = new Intent();
//        broadCastIntent.setAction(HookMain.RECEIVE_BILL_ALIPAY2);
//        HKApplication.app.sendBroadcast(broadCastIntent);
//
//        String time = System.currentTimeMillis() / 1000 + "";
//        PayUtils.getInstance().creatAlipayQr(this, 12, "test" + time);
    }

    //云闪付
    public void clsUnionpayPay(View view) {
        if(!ServiceMain.mIsRunning)
        {
            Toast.makeText(MainActivity.getAppContext(), "请确认配置信息后，再启动云闪付收款功能", Toast.LENGTH_SHORT).show();
            return;
        }
        changeUnionpayStatus();

        CommFunction.getInstance().postEventBus(CommFunction.getInstance().updateUnionpayStr(ServiceMain.mIsUnionpayRunning));

        /*Intent intent = new Intent(UNIONPAY_CREAT_QR);
        String money ="12";
        String mark ="aaa";
        intent.putExtra("money",money);
        intent.putExtra("mark",mark);
        sendBroadcast(intent);*/
    }

    /**
     * 添加QQ群，保留版权哦。
     *
     * @param view
     */
    public void clsAddQq(View view) {
        LogcatDialog s= new LogcatDialog(getActivity());
        s.searchTag="arik";
        s.show();
    }


    /**
     * 当获取到权限后才操作的事情
     */
    public static void onPermissionOk() {
        mEdtUrl.setText(Configer.getInstance().getSiteName());
        mEdtToken.setText(Configer.getInstance().getToken());
        mEdtSN.setText(Configer.getInstance().getSN());
        mEdtUserWechat.setText(Configer.getInstance().getUserWechat());
        mEdtUserAlipay.setText(Configer.getInstance().getUserAlipay());
        mEdtUserAlipayCurrentAmount.setText(Configer.getInstance().getCurrentAmount());
//        if (getActivity().getIntent().hasExtra("auto")) {
//            clsSubmit(null);
//        }
    }
    private void getPermissions() {
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onPermissionOk();
            return;
        }
        List<String> sa = new ArrayList<>();
        if ( ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请READ_PHONE_STATE权限。。。。
            sa.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            sa.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            sa.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (sa.size() < 1) {
            onPermissionOk();
            return;
        }
        ActivityCompat.requestPermissions(getActivity(), sa.toArray(new String[]{}), 1);
    }

    /**
     * 在状态栏添加图标
     */
    public void addStatusBar() {

        NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
        PendingIntent pi = PendingIntent.getActivity(getActivity(), 0, getActivity().getIntent(), 0);
        Notification noti = new Notification.Builder(getActivity())
                .setTicker("程序启动成功")
                .setContentTitle("看到我，说明我在后台正常运行")
                .setContentText("始于：" + new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date()))
                .setSmallIcon(R.mipmap.ic_launcher)//设置图标
                .setDefaults(Notification.DEFAULT_SOUND)//设置声音
                .setContentIntent(pi)//点击之后的页面
                .build();

        manager.notify(17952, noti);
    }


}
