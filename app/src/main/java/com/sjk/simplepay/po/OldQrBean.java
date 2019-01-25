package com.sjk.simplepay.po;

import com.alibaba.fastjson.JSON;

import java.text.DecimalFormat;

public class OldQrBean {
    private String amount;
    private Integer id;

    /**
     * 渠道类型
     */
    private String channel ;//wechat,alipay


    /**
     * 二维码的收款方备注
     */
    private String orderInfo;



    /**
     * 订单id
     */
    private String order_id;

    public Integer getId() {
        return id == null ? 0 : id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAmount() {
        return Integer.valueOf(amount)*100;
    }

    public void setAmount(String  money) {
        this.amount = amount;
    }

    public String getOrderInfo() {
        return orderInfo == null ? "" : orderInfo;
    }

    public void setOrderInfo(String orderInfo) {
        this.orderInfo = orderInfo;
    }

    public String getChannel() {
        return channel == null ? "" : channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
