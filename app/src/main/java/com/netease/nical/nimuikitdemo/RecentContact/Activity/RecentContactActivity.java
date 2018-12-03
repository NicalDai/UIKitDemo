package com.netease.nical.nimuikitdemo.RecentContact.Activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import android.widget.TextView;
import android.widget.Toast;

import com.netease.nical.nimuikitdemo.R;
import com.netease.nical.nimuikitdemo.RecentContact.RecentContactAdapter;
import com.netease.nical.nimuikitdemo.RecentContact.RecentContactItem;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthServiceObserver;
import com.netease.nimlib.sdk.auth.constant.LoginSyncStatus;
import com.netease.nimlib.sdk.friend.FriendService;
import com.netease.nimlib.sdk.friend.model.Friend;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.model.RecentContact;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.VISIBLE;

public class RecentContactActivity extends AppCompatActivity {

    //RecyclerView relevant
    private RecyclerView recentContactRycV;
    private List<RecentContactItem> recentContacts = new ArrayList<>();
    RecentContactAdapter recentContactAdapter;
    LinearLayoutManager linearLayoutManager;

    //View relevant
    ImageView yunxin_ImgV;
    ImageView contact_ImgV;
    ImageView friendCirc_ImgV;
    ImageView myProfie_ImgV;

    TextView yunxin_txV;
    TextView contact_txV;
    TextView friendCirc_txV;
    TextView myProfie_txV;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_contact);
        getSupportActionBar().hide();
        regRecentContactObserver(true);
        regLoginStepObserver(true);
        initView();
        initRecyclerView();
//        getRecentSessions();

    }

    private void initView(){
        yunxin_ImgV = (ImageView) findViewById(R.id.yunxin_btn);
        contact_ImgV = (ImageView) findViewById(R.id.contact_btn);
        friendCirc_ImgV = (ImageView) findViewById(R.id.friendcir_btn);
        myProfie_ImgV = (ImageView) findViewById(R.id.my_btn);

        yunxin_txV = (TextView) findViewById(R.id.total_unread_count_icon);
        contact_txV = (TextView) findViewById(R.id.total_friend_apply_icon);
        friendCirc_txV = (TextView) findViewById(R.id.total_friend_circle_icon);
        myProfie_txV = (TextView) findViewById(R.id.total_person_notification_icon);
        recentContactRycV = (RecyclerView) findViewById(R.id.recentcontact_recyclerview);
        //设置未读数

    }

    private void initRecyclerView(){
        linearLayoutManager = new LinearLayoutManager(this);
        recentContactAdapter = new RecentContactAdapter(recentContacts);
        recentContactAdapter.setContext(this);
        recentContactRycV.setAdapter(recentContactAdapter);
        recentContactRycV.setLayoutManager(linearLayoutManager);
        // 添加Item的分割线
        recentContactRycV.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        recentContactAdapter.setOnItemClickListener(new RecentContactAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                Log.d("RecentContactList", "onClick: " + position);
                switch (recentContacts.get(position).getRecentContact().getSessionType()){
                    case P2P:{
                        NimUIKit.startP2PSession(RecentContactActivity.this, recentContacts.get(position).getRecentContact().getContactId());
                        break;
                    }
                    case Team:{
                        NimUIKit.startTeamSession(RecentContactActivity.this,recentContacts.get(position).getRecentContact().getContactId());
                    }
                }
            }

            @Override
            public void onLongClick(int position) {

            }
        });
    }

    /**
     * 获取本地数据库的会话
     */
    private void getRecentSessions(){
        //捞一把未读数
        updateTotalUnreadCount();
        NIMClient.getService(MsgService.class).queryRecentContacts()
                .setCallback(new RequestCallbackWrapper<List<RecentContact>>() {
                    @Override
                    public void onResult(int code, List<RecentContact> recents, Throwable e) {
                        if(!(recents.size() == 0)){
                            for (int i = 0;i < recents.size(); i++){
                                RecentContactItem recentContactItemBuffer = new RecentContactItem(recents.get(i),true);
                                recentContacts.add(recentContactItemBuffer);
                            }
                            recentContactAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    /**
     * TODO 这里有个坑，是app重新安装后第一次登录，获取不到会话列表
     * 注册数据同步的观察者
     * @param register
     */
    private void regLoginStepObserver(Boolean register){
        NIMClient.getService(AuthServiceObserver.class).observeLoginSyncDataStatus(new Observer<LoginSyncStatus>() {
            @Override
            public void onEvent(LoginSyncStatus status) {
                if (status == LoginSyncStatus.BEGIN_SYNC) {
                    Toast.makeText(RecentContactActivity.this, "开始同步数据", Toast.LENGTH_SHORT).show();
                } else if (status == LoginSyncStatus.SYNC_COMPLETED) {
                    //获取本地数据库同步下来的会话做展示

                    getRecentSessions();
                    Toast.makeText(RecentContactActivity.this, "同步数据完成，获取到"+recentContacts.size()+"个会话", Toast.LENGTH_SHORT).show();

                }
            }
        }, register);
    }

    /**
     * 最近会话更新的回调，用于处理会话列表UI的展示
     * @param register
     */
    private void regRecentContactObserver(Boolean register) {
        //  创建观察者对象
        Observer<List<RecentContact>> messageObserver =
                new Observer<List<RecentContact>>() {
                    @Override
                    public void onEvent(List<RecentContact> onrecentContact) {
                        //这里有做去重的逻辑，如果有新的会话，插入，如果会话信息有更新，更新UI
                        notifyContactChanged(onrecentContact);
                        //更新未读总数
                        updateTotalUnreadCount();
                    }
                };
        NIMClient.getService(MsgServiceObserve.class).observeRecentContact(messageObserver, register);
    }

    /**
     * 最近会话变更的封装方法,需要判断群组和点对点会话
     * 需要判断会话是应该插入，还是应该更新
     * @param onrecentContact
     */
    private void notifyContactChanged(List<RecentContact> onrecentContact){
        if (!(recentContacts.size() == 0)) {
            Boolean flag = false;
            for (int i = 0; i < onrecentContact.size(); i++) {
                //遍历去重
                for(int j = 0;j < recentContacts.size();j++){
                    String onContactId = onrecentContact.get(i).getContactId();
                    String contactId = recentContacts.get(j).getRecentContact().getContactId();
                    if(onContactId.equals(contactId)){
                        flag = true;
                        pintTotop(recentContacts,j,onrecentContact.get(0));
                        recentContactAdapter.notifyDataSetChanged();
                    }
                }
                if(!flag){
                    RecentContactItem recentContactItemBuffer = new RecentContactItem(onrecentContact.get(i),true);
                    recentContacts =  insertToTop(recentContacts,recentContactItemBuffer);
                    recentContactAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    /**
     * 更新未读总数
     */
    private void updateTotalUnreadCount(){
        int unreadNum = NIMClient.getService(MsgService.class).getTotalUnreadCount();
        if(unreadNum > 0){
            yunxin_txV.setText(unreadNum+"");
            yunxin_txV.setVisibility(VISIBLE);
        } else {
            yunxin_txV.setVisibility(View.GONE);
        }
    }

    /**
     * 从列表中取一个会话置顶
     * @param recentContacts
     * @param position
     */
    private void pintTotop(List<RecentContactItem> recentContacts, int position,RecentContact onrecentContact){

        //构建一个自定义的会话对象
        RecentContactItem recentContactItem = new RecentContactItem();
        recentContactItem.setRecentContact(onrecentContact);
        recentContactItem.setNeedFresh(true);

        if(position == 0){
            recentContacts.set(0,recentContactItem);
            return;
        }

        for (int i = position ;i > 0;i--){
            recentContacts.set(i,recentContacts.get(i - 1));
        }
        recentContacts.set(0,recentContactItem);
        recentContacts.get(0).setNeedFresh(true);
    }

    /**
     * 插入到第一个
     * @param recentContacts
     * @param recentContacttoInsert
     */
    private List<RecentContactItem> insertToTop(List<RecentContactItem> recentContacts, RecentContactItem recentContacttoInsert){

        recentContacts.add(recentContacts.size(),recentContacttoInsert);
        //需要UI刷新
        recentContacttoInsert.setNeedFresh(true);

        for (int i = recentContacts.size() - 1;i > 0;i--){
            recentContacts.set(i ,recentContacts.get(i - 1));
        }
        recentContacts.set(0,recentContacttoInsert);

        return recentContacts;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //注销观察者
        regLoginStepObserver(false);
        regRecentContactObserver(false);
    }
}
