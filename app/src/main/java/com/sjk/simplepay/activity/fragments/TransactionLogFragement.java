package com.sjk.simplepay.activity.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sjk.simplepay.activity.MainActivity;
import com.sjk.simplepay.po.Constants;
import com.sjk.simplepay.utils.PayUtils;
import com.sjk.tpay.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TransactionLogFragement extends Fragment {
    public static TextView console;
    public static TextView consoleDebug;
    public static TextView currentAmountView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.transaction, container, false);
        return rootView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        console = getView().findViewById(R.id.console);
        consoleDebug = getView().findViewById(R.id.console_debug);
        console.setMovementMethod(ScrollingMovementMethod.getInstance());
        console.setGravity(80);
        consoleDebug.setMovementMethod(ScrollingMovementMethod.getInstance());
        consoleDebug.setGravity(80);
        currentAmountView = getView().findViewById(R.id.currentamount);
    }
}
