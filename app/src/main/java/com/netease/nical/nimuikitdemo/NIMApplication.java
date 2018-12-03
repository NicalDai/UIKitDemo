package com.netease.nical.nimuikitdemo;

import android.app.Application;
import android.util.Log;

import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.msg.SystemMessageObserver;
import com.netease.nimlib.sdk.msg.model.SystemMessage;
import com.netease.nimlib.sdk.util.NIMUtil;

public class NIMApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        //初始化sdk
        NIMClient.init(this,null,null);

        if(NIMUtil.isMainProcess(this)){
            NimUIKit.init(this);
        }
    }
}
