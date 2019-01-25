package com.sjk.simplepay.module.presenter;

import com.sjk.simplepay.module.view.IBaseView;

/**
 * Created by zhoup on 2017/6/21.
 */

public interface BasePresenter<T extends IBaseView> {
    void attachView(T view);
    void detachView();
}
