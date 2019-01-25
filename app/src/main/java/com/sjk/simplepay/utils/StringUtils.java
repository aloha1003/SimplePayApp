package com.sjk.simplepay.utils;

import android.util.Base64;
import android.util.Log;

import com.sjk.simplepay.po.Configer;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class StringUtils {
    static String TAG = "WTF";

    public static Map<String,String> signCreate(Map<String,String> arguments) {
//        arguments.put("alipay_account", PreferenceHelper.getString("account_id", AppConst.API_ACCOUNT_ID));
        arguments.put("alipay_account", Configer.getInstance().getUserAlipay());

        List<Map.Entry<String, String>> list =
                new ArrayList<Map.Entry<String, String>>(arguments.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> entry1, Map.Entry<String, String> entry2) {
                return (entry1.getKey().compareTo(entry2.getKey()));
            }
        });
        String query = obtainQueryStringByList(list);
        String hash = getSHA256(query);
        arguments.put("sign", hash);
        return arguments;
    }
    public static String obtainQueryStringByList(List<Map.Entry<String, String>> arguments) {
        String encode = getEncode();
        String str=null;                //用來存傳送參數
        try {
            if (arguments != null && !arguments.isEmpty()) {            //判斷map是否非null或有初始化
                for (Map.Entry<String, String> entry : arguments) {
                    if(str==null)    //判斷是否為第一次調用
                        str=entry.getKey()+"="+ URLEncoder.encode(entry.getValue(),encode).replace("*", "%2A").replace("%7E", "~");
                    else
                        str=str+"&"+entry.getKey()+"="+URLEncoder.encode(entry.getValue(),encode).replace("*", "%2A").replace("%7E", "~");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"Http 產生查詢字串有問題 : "+e.toString());
        }
        return str;
    }
    public static String getEncode() {
        return "UTF-8";
    }
    private static final char[] hexArray = "0123456789abcdef".toCharArray();

    public static String getSHA256(String data) {
        String hash = "";
        try {

//            String secret = PreferenceHelper.getString("api_key", AppConst.API_ACCOUNT_KEY);
            String secret = Configer.getInstance().getSN();


            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hashs = sha256_HMAC.doFinal(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte x : hashs) {
                String b = Integer.toHexString(x & 0XFF);
                if (b.length() == 1) {
                    b = '0' + b;
                }
//	          sb.append(String.format("{0:x2}", x));
                sb.append(b);
            }
            hash = md5(sb.toString());
//            hash = Base64.encodeToString(sha256_HMAC.doFinal(data.getBytes()), 0);
        }
        catch (Exception e){
            Log.e(TAG, "Error: "+ e.getMessage());
        }
        return hash;
    }

    public static String md5(String str) {
        String md5=null;
        try {
            MessageDigest md=MessageDigest.getInstance("MD5");
            byte[] barr=md.digest(str.getBytes());  //將 byte 陣列加密
            StringBuffer sb=new StringBuffer();  //將 byte 陣列轉成 16 進制
            for (int i=0; i < barr.length; i++) {sb.append(byte2Hex(barr[i]));}
            String hex=sb.toString();
            md5= hex; //一律轉成大寫
        }
        catch(Exception e) {e.printStackTrace();}
        return md5;
    }
    public static String byte2Hex(byte b) {
        String[] h={"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
        int i=b;
        if (i < 0) {i += 256;}
        return h[i/16] + h[i%16];
    }
}
