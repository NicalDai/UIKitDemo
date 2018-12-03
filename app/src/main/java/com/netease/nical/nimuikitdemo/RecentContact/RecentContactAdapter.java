package com.netease.nical.nimuikitdemo.RecentContact;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.netease.nical.nimuikitdemo.CustomView.RoundImageView;
import com.netease.nical.nimuikitdemo.NimSDKOptionConfig;
import com.netease.nical.nimuikitdemo.R;

import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nimlib.sdk.AbortableFuture;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.nos.NosService;
import com.netease.nimlib.sdk.team.TeamService;
import com.netease.nimlib.sdk.team.constant.TeamFieldEnum;
import com.netease.nimlib.sdk.team.model.Team;
import com.netease.nimlib.sdk.team.model.TeamMember;
import com.netease.nimlib.sdk.uinfo.UserService;
import com.netease.nimlib.sdk.uinfo.constant.UserInfoFieldEnum;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;
import com.othershe.combinebitmap.CombineBitmap;
import com.othershe.combinebitmap.layout.WechatLayoutManager;
import com.othershe.combinebitmap.listener.OnProgressListener;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.netease.nical.nimuikitdemo.NimSDKOptionConfig.getAppCacheDir;

public class RecentContactAdapter extends RecyclerView.Adapter<RecentContactAdapter.ViewHolder> {

    private List<RecentContactItem> recentContacts;
    OnItemClickListener onItemClickListener;
    Context context;  //对应activity的Context
    private String[] teamMemberAvatars;


//    private String defaultUrl = "https://nim.nosdn.127.net/MTAxMTAwMg==/bmltYV8xMDkyNTAxOTIxXzE1NDAwOTcyODAyODFfYmM1YzViN2MtNTQxNy00NmZiLTg5ZmYtMWEyNTFlNTU1NGNh?imageView&createTime=1540097280444";


    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        RoundImageView sessionIconV;
        TextView sessionNameV;
        View thisView;
        TextView sessionLastMsgV;
        TextView timeV;
        TextView unreadCountText;
        LinearLayout recentContactV;

        public ViewHolder(View view) {
            super(view);
            recentContactV = (LinearLayout) view.findViewById(R.id.recent_contact_item);
            sessionIconV = (RoundImageView) view.findViewById(R.id.session_icon);
            sessionNameV = (TextView) view.findViewById(R.id.session_name);
            sessionLastMsgV = (TextView) view.findViewById(R.id.last_message);
            timeV = (TextView) view.findViewById(R.id.time_text);
            unreadCountText = (TextView) view.findViewById(R.id.unread_number_text);
            thisView = view;
        }


        @Override
        public void onClick(View v) {

        }
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public RecentContactAdapter(List<RecentContactItem> recentContacts) {
        this.recentContacts = recentContacts;
    }

    public void setRecentContact(RecentContactItem recentContact1, int position) {
        recentContacts.set(position, recentContact1);
    }

    public RecentContactItem getRecentContact(int position){
        return recentContacts.get(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_contact_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {


        SessionTypeEnum sessionType = recentContacts.get(position).getRecentContact().getSessionType();


        switch (sessionType) {
            case P2P: {
                showP2PSessionItem(holder, position);
                break;
            }
            case Team: {
                showTeamSessionItem(holder, position);
                break;
            }
        }


        /**
         * Item点击事件
         */
        if (onItemClickListener != null) {
            holder.recentContactV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onClick(position);
                }
            });

            holder.recentContactV.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemClickListener.onLongClick(position);
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return recentContacts.size();
    }

    /**
     * 设置Adapter的点击事件
     */
    public interface OnItemClickListener {
        void onClick(int position);

        void onLongClick(int position);
    }

    /**
     * 点击事件接口配置函数
     *
     * @param onItemClickListener
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    /**
     * 点对点会话Item的展示
     *
     * @param holder
     * @param position
     */
    private void showP2PSessionItem(final ViewHolder holder, final int position) {
        String nickName = recentContacts.get(position).getRecentContact().getFromNick();
        String sessionID = recentContacts.get(position).getRecentContact().getContactId();

        //设置最后一条消息的缩略信息展示
        holder.sessionLastMsgV.setText(nickName + ":" + recentContacts.get(position).getRecentContact().getContent());

        //设置最近会话的未读数
        int unreadCount = recentContacts.get(position).getRecentContact().getUnreadCount();
        if (unreadCount > 0) {
            holder.unreadCountText.setText(unreadCount + "");
            holder.unreadCountText.setVisibility(View.VISIBLE);
        } else {
            holder.unreadCountText.setVisibility(View.GONE);
        }
        //设置时间展示
        holder.timeV.setText(TimeTranser.chargeSecondsToNowTime(recentContacts.get(position).getRecentContact().getTime()));

        NimUserInfo userInfo = NIMClient.getService(UserService.class).getUserInfo(sessionID);
        if (userInfo == null) {
            List<String> accounts = new ArrayList<>();
            accounts.add(sessionID);
            NIMClient.getService(UserService.class).fetchUserInfo(accounts).setCallback(new RequestCallback<List<NimUserInfo>>() {
                @Override
                public void onSuccess(List<NimUserInfo> nimUserInfos) {
                    String avatarUrl = nimUserInfos.get(0).getAvatar();
                    if (!(avatarUrl == null) && !(avatarUrl.equals(""))) {
                        //Glide加载网络图片
                        Glide.with(holder.thisView).load(avatarUrl).into(holder.sessionIconV);
                        holder.sessionNameV.setText(nimUserInfos.get(0).getName());
                        //
                    } else {
                        //Glide加载默认图片
                        Glide.with(holder.thisView).load(R.drawable.default_icon).into(holder.sessionIconV);
                        //TODO Bug 1108
                        if (nimUserInfos.get(0).getName() != null) {
                            holder.sessionNameV.setText(nimUserInfos.get(0).getName());
                        }
                    }
                    Log.d("拉取云端资料，设置会话头像", "onSuccess: ");
                }

                @Override
                public void onFailed(int i) {
                    Log.d("获取云端资料", "onFailed: ");
                }

                @Override
                public void onException(Throwable throwable) {
                    Log.d("获取云端资料", "onException: ");
                }
            });
        } else {
            if (!(userInfo.getAvatar() == null) && !(userInfo.getAvatar().equals(""))) {
                Glide.with(holder.thisView).load(userInfo.getAvatar()).into(holder.sessionIconV);
                holder.sessionNameV.setText(userInfo.getName());
            } else {
                //Glide加载默认图片
                Glide.with(holder.thisView).load(R.drawable.default_icon).into(holder.sessionIconV);
                if (userInfo.getName() != null) {
                    holder.sessionNameV.setText(userInfo.getName());
                }
            }
        }
    }

    /**
     * 群组会话Item展示
     *
     * @param holder
     * @param position
     */
    private void showTeamSessionItem(final ViewHolder holder, final int position) {
        String nickName = recentContacts.get(position).getRecentContact().getFromNick();
        String sessionID = recentContacts.get(position).getRecentContact().getContactId();

        //设置最后一条消息的缩略信息展示
        holder.sessionLastMsgV.setText(nickName + ":" + recentContacts.get(position).getRecentContact().getContent());
        //设置最近会话的未读数
        int unreadCount = recentContacts.get(position).getRecentContact().getUnreadCount();
        if (unreadCount > 0) {
            holder.unreadCountText.setText(unreadCount + "");
            holder.unreadCountText.setVisibility(View.VISIBLE);
        } else {
            holder.unreadCountText.setVisibility(View.GONE);
        }
        //设置时间展示
        holder.timeV.setText(TimeTranser.chargeSecondsToNowTime(recentContacts.get(position).getRecentContact().getTime()));
        /**
         * 查询群组资料
         */
        NIMClient.getService(TeamService.class).queryTeam(sessionID)
                .setCallback(new RequestCallback<Team>() {
                    @Override
                    public void onSuccess(Team team) {

                        holder.sessionNameV.setText(team.getName());
                        //获取群组资料并展示
                        if(team.getIcon() == null || team.getIcon().equals("")){
                            showTeamMemberIcon(team.getId(),holder,recentContacts.get(position));
                        }else {
                            Glide.with(holder.thisView).load(team.getIcon()).into(holder.sessionIconV);
//                            holder.sessionIconV.setImageBitmap(team.getIcon());
                        }
                    }

                    @Override
                    public void onFailed(int i) {
                        Log.e("最近会话列表获取群组资料失败", "onFailed: " + i);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        Log.e("最近会话列表获取群组资料异常", "onException: " + throwable.toString());
                    }
                });
    }

    /**
     * 展示群组头像
     * @param teamID
     * @param holder
     * @param recentContactItem
     * @return
     */
    private List<String> showTeamMemberIcon(final String teamID, final ViewHolder holder, final RecentContactItem recentContactItem) {

        NIMClient.getService(TeamService.class).queryMemberList(teamID).setCallback(new RequestCallbackWrapper<List<TeamMember>>() {
            @Override
            public void onResult(int code, final List<TeamMember> members, Throwable exception) {
                Log.d("获取群成员", "onResult: " + code);
                List<String> accounts = new ArrayList<>();

                for (int i = 0; i < members.size() && i < 9; i++) {
                    accounts.add(members.get(i).getAccount());
                }


                List<NimUserInfo> users = NIMClient.getService(UserService.class).getUserInfoList(accounts);
                teamMemberAvatars = new String[accounts.size()];
                if (accounts.size() > 6){
                    for (int i = 0;i < users.size();i++){
                        if(users.get(i).getAvatar() == null || users.get(i).getAvatar().equals("")){
                            teamMemberAvatars[i] = "https://nim.nosdn.127.net/MTAxMTAwMg==/bmltYV8xMzE4NzQ1NTg1XzE1NDIxMDE1NDU1ODFfODVmZTU1YWYtMTg3Ni00NzBkLTg2YzYtMWMxMmQ5ZjczMmMx?imageView&createTime=1542101545953?&thumbnail=20x0";
                        } else {
                            teamMemberAvatars[i] = users.get(i).getAvatar() + "?&thumbnail=20x0";
                        }
                    }
                }else {
                    for (int i = 0;i < users.size();i++){
                        if(users.get(i).getAvatar() == null || users.get(i).getAvatar().equals("")){
                            teamMemberAvatars[i] = "https://nim.nosdn.127.net/MTAxMTAwMg==/bmltYV8xMzE4NzQ1NTg1XzE1NDIxMDE1NDU1ODFfODVmZTU1YWYtMTg3Ni00NzBkLTg2YzYtMWMxMmQ5ZjczMmMx?imageView&createTime=1542101545953?&thumbnail=45x0";
                        } else {
                            teamMemberAvatars[i] = users.get(i).getAvatar() + "?&thumbnail=45x0";
                        }
                    }
                }

                showMixedIcon(teamID,holder,recentContactItem,teamMemberAvatars);
            }
        });
        return null;
    }


    /**
     * 九宫格群头像展示
     * @param holder
     * @param recentContactItem
     * @param teamAvatars
     * @param teamID
     */
    private void showMixedIcon(final String teamID, final ViewHolder holder , final RecentContactItem recentContactItem,String... teamAvatars){
        //微信风九宫格头像展示，参考： https://github.com/SheHuan/CombineBitmap
        WechatLayoutManager wechatLayoutManager = new WechatLayoutManager();
        CombineBitmap.init(context)
                .setLayoutManager(wechatLayoutManager)
                .setSize(45)
                .setGap(1)
                .setPlaceholder(R.drawable.default_icon)
                .setGapColor(Color.parseColor("#E8E8E8"))
                .setUrls(teamAvatars)
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onStart() {
                        Log.d("开始合成", "onStart: ");
                    }

                    @Override
                    public void onComplete(Bitmap bitmap) {
                        holder.sessionIconV.setImageBitmap(bitmap);
                        recentContactItem.setTeamIcon(bitmap);
                        File file =  saveBitmapFile(bitmap, getAppCacheDir(context.getApplicationContext())+"/nim");
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        AbortableFuture<String> uploadAvatarFuture = NIMClient.getService(NosService.class).upload(file,"image/png");
                        uploadAvatarFuture.setCallback(new RequestCallbackWrapper<String>() {
                            @Override
                            public void onResult(int code, String url, Throwable exception) {
                                if (code == ResponseCode.RES_SUCCESS && !TextUtils.isEmpty(url)) {
                                    LogUtil.i("RecentContactAdapter", "upload avatar success, url =" + url);
                                    NIMClient.getService(TeamService.class).updateTeam(teamID, TeamFieldEnum.ICON, url).setCallback(new RequestCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void param) {
                                            Log.d("RecentContaceAdapter", "onSuccess:  更新群组头像成功");
                                        }

                                        @Override
                                        public void onFailed(int code) {
                                            // 失败
                                        }

                                        @Override
                                        public void onException(Throwable exception) {
                                            // 错误
                                        }
                                    });
                                }
                            }
                        });

                    }
                }).build();
    }

    /**
     * 把batmap 转file
     * @param bitmap
     * @param filepath
     */
    public static File saveBitmapFile(Bitmap bitmap, String filepath){
        File file=new File(filepath);//将要保存图片的路径
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }


}
