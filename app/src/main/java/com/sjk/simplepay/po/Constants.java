package com.sjk.simplepay.po;

public class Constants {
    public static final String MSGRECEIVED_ACTION = "com.sjk.simplepay.msgreceived";
    public static final String QUEUE_RECEIVE_ACTION = "com.sjk.simplepay.queuereceived";
    public static final String CURRENT_AMOUNT_UPDATE = "com.sjk.simplepay.currenamountupdate";
    public static final String CURRENT_AMOUNT_UPDATE_STEP_ONE = "com.sjk.simplepay.currenamountupdatestepone";

    //被申请要创建二维码的广播
    public static final String WECHAT_CREAT_QR = "com.wechat.qr.create";
    public static final String ALIPAY_CREAT_QRFIX = "com.alipay.qr.createFix";
    public static final String ALIPAY_CREAT_QR = "com.alipay.qr.create";
    public static final String UNIONPAY_CREAT_QR = "com.unionpay.qr.create";

    //成功生成二维码的HOOK广播消息
    public static final String RECEIVE_QR_WECHAT = "com.wechat.qr.receive";
    public static final String RECEIVE_QR_ALIPAY = "com.alipay.qr.receive";
    public static final String RECEIVE_QR_UNIONPAY = "com.unionpay.qr.receive";

    //接收到新订单的HOOK广播消息
    public static final String RECEIVE_BILL_WECHAT = "com.wechat.bill.receive";
    public static final String RECEIVE_BILL_ALIPAY = "com.alipay.bill.receive";
    public static final String RECEIVE_BILL_ALIPAY2 = "com.alipay.bill.receive2";
    public static final String RECEIVE_BILL_UNIONPAY = "com.unionpay.bill.receive";


    public static final String WECHAT_PACKAGE = "com.tencent.mm";
    public static final String ALIPAY_PACKAGE = "com.eg.android.AlipayGphone";
    public static final String UNIONPAY_PACKAGE = "com.unionpay";


    public static String ALIPAYSTART_ACTION = "com.payhelper.alipay.start";;
    public static final String LOGINIDRECEIVED_ACTION = "com.payhelper.alipay.id";;
    public static String QQSTART_ACTION = "com.tools.payhelper.loginidreceived";;
    public static String TRADENORECEIVED_ACTION = "com.tools.payhelper.tradenoreceived";;
    public static String WECHATSTART_ACTION = "com.payhelper.wechat.start";

    public static final String Tab_TITLE_CONFIGURE = "參數設定";
    public static final String SAVEALIPAYCOOKIE_ACTION = "save.cookie";
    public static final String Tab_TITLE_TRANSACTION_LOG = "交易記錄區";
}
