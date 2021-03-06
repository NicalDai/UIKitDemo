package com.netease.nical.nimuikitdemo.Login.Activity;


import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.netease.nical.nimuikitdemo.Login.CustomBoolean;
import com.netease.nical.nimuikitdemo.Login.DataSaveToLocal;
import com.netease.nical.nimuikitdemo.Login.EditTextClearTools;
import com.netease.nical.nimuikitdemo.Login.MD5.MD5;
import com.netease.nical.nimuikitdemo.Login.permission.MPermission;
import com.netease.nical.nimuikitdemo.Login.permission.annotation.OnMPermissionDenied;
import com.netease.nical.nimuikitdemo.Login.permission.annotation.OnMPermissionGranted;
import com.netease.nical.nimuikitdemo.Login.permission.annotation.OnMPermissionNeverAskAgain;
import com.netease.nical.nimuikitdemo.NimSDKOptionConfig;

import com.netease.nical.nimuikitdemo.R;
import com.netease.nical.nimuikitdemo.RecentContact.Activity.RecentContactActivity;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.chatroom.ChatRoomService;
import com.netease.nimlib.sdk.chatroom.model.EnterChatRoomData;
import com.netease.nimlib.sdk.chatroom.model.EnterChatRoomResultData;
import com.netease.nimlib.sdk.friend.FriendService;
import com.netease.nimlib.sdk.friend.constant.VerifyType;
import com.netease.nimlib.sdk.friend.model.AddFriendData;
import com.netease.nimlib.sdk.friend.model.Friend;
import com.netease.nimlib.sdk.msg.SystemMessageService;
import com.netease.nimlib.sdk.msg.model.SystemMessage;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button Login_OK;    //登陆按钮
    private EditText account;   //账号输入框
    private EditText password;  //密码输入框
    private static final int BASIC_PERMISSION_REQUEST_CODE = 100;
    private String token;
    private String Appkey;
    private CheckBox needRecordPasswordCB;
    private Boolean needRecordPassword = false;
    private DataSaveToLocal dataSaveToLocal = new DataSaveToLocal();
    private CustomBoolean needMD5 = new CustomBoolean(false); //token是否需要MD5（仅仅针对demo的appkey而言），如果是本地文件读取的，就不需要MD5
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide(); //去除actionbar

        Appkey = getAppKey();
        filePath = NimSDKOptionConfig.getAppCacheDir(getApplicationContext())+"logininfo.txt";
        //权限信息
        authorityManage();
        //初始化界面
        initView();
        //获取基本权限（相机，麦克风等）
        requestBasicPermission();

        //保存按钮点击事件
        needRecordPasswordCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    needRecordPassword = true;
                }else {
                    needRecordPassword = false;
                }
            }
        });

        Login_OK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String accid = account.getText().toString();
                //判断是否为demo的appkey，是就加MD5，不是就直接透传
                if(Appkey.equals("45c6af3c98409b18a84451215d0bdd6e") && needMD5.getB()){

                    token = MD5.getStringMD5(password.getText().toString());
                }else {
                    token = password.getText().toString();
                }
                //实例化logininfo，执行登陆
                LoginInfo loginInfo = new LoginInfo(accid,token);
                doLogin(loginInfo);
            }
        });

    }

    /**
     * 从本地文本获取登陆信息
     * @return
     */
    private LoginInfo getLoginInfo(){
        String loginData = dataSaveToLocal.readDataToLocal(NimSDKOptionConfig.getAppCacheDir(getApplicationContext())+"logininfo.txt");
        if(!loginData.isEmpty()){
            int index1 = loginData.indexOf(",");
            String accid = loginData.substring(0,index1);
            String token = loginData.substring(index1+1,loginData.length());
            LoginInfo loginInfo = new LoginInfo(accid,token);
            return loginInfo;
        }else {
            return null;
        }
    }

    /**
     * 界面初始化
     */
    private void initView(){

        Login_OK = (Button) findViewById(R.id.Login_OK);
        account = (EditText) findViewById(R.id.account);
        password = (EditText) findViewById(R.id.password);

        ImageView unameClear = (ImageView) findViewById(R.id.iv_unameClear);
        ImageView pwdClear = (ImageView) findViewById(R.id.iv_pwdClear);

        //配置点击按钮清除EditText的内容
        EditTextClearTools.addClearListener(account,unameClear,needMD5);
        EditTextClearTools.addClearListener(password,pwdClear,needMD5);

        //从本地获取之前存储的logininfo
        LoginInfo loginInfo =  getLoginInfo();

        if(loginInfo != null){
            needMD5.setB(false);    //如果是demo 的appkey，从本地文件中获取的密码，已经是md5之后的结果。
            account.setText(loginInfo.getAccount());
            password.setText(loginInfo.getToken());
        }
        //记住密码的勾选框
        needRecordPasswordCB = findViewById(R.id.cb_checkbox);
    }


    /**
     * 权限申请
     */
    private void authorityManage(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }else {
                Toast.makeText(this, "权限已申请", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 基本权限管理
     */
    private final String[] BASIC_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private void requestBasicPermission() {
        MPermission.printMPermissionResult(true, this, BASIC_PERMISSIONS);
        MPermission.with(MainActivity.this)
                .setRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                .permissions(BASIC_PERMISSIONS)
                .request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        try {
            Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }

        MPermission.printMPermissionResult(false, this, BASIC_PERMISSIONS);
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    @OnMPermissionNeverAskAgain(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed() {
        try {
            //TODO 影响体验，先注释了
//            Toast.makeText(this, "未全部授权，部分功能可能无法正常运行！", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }

        MPermission.printMPermissionResult(false, this, BASIC_PERMISSIONS);
    }


    /**
     * 执行登陆
     * @param loginInfo
     */
    private void doLogin(LoginInfo loginInfo){
        RequestCallback<LoginInfo> callback = new RequestCallback<LoginInfo>() {
            @Override
            public void onSuccess(final LoginInfo loginInfo) {
                Toast.makeText(MainActivity.this, "登陆成功！", Toast.LENGTH_SHORT).show();
                //判断是否需要本地存储密码(CheckBox打钩)
                if (needRecordPassword){
                    dataSaveToLocal.saveDataToLocal(loginInfo.getAccount()+","+loginInfo.getToken(),filePath);
                }else {//删除文件
                    dataSaveToLocal.deleteFile(filePath);
                }
                //本地缓存当前登陆的账号
                NimUIKit.setAccount(loginInfo.getAccount());
//                NimUIKit.startP2PSession(MainActivity.this, "ahe2");
                Intent intent = new Intent(MainActivity.this, RecentContactActivity.class);
                startActivity(intent);
                finish();
                /** 跳转到拨打界面
                Intent intent = new Intent(MainActivity.this,TeamSelectActivity.class);
                intent.putExtra("currentLoginAccount",loginInfo.getAccount());
                startActivity(intent);
                finish();
                 */
            }

            @Override
            public void onFailed(int i) {
                Toast.makeText(MainActivity.this, "登陆失败，错误码："+i, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onException(Throwable throwable) {
                Toast.makeText(MainActivity.this, "登陆异常，" + throwable.toString(), Toast.LENGTH_SHORT).show();
            }
        };
        NIMClient.getService(AuthService.class).login(loginInfo).setCallback(callback);
    }

    /**
     * 点击空白位置 隐藏软键盘
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(null != this.getCurrentFocus()){
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        return super.onTouchEvent(event);
    }


    /**
     * 取出清单文件的appkey，用作MD5比较
     * @return
     */
    public String getAppKey() {
        String keyString = "";
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
            keyString = appInfo.metaData.getString("com.netease.nim.appKey");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return keyString;
    }
}
