package com.netease.nical.nimuikitdemo.RecentContact;

import android.graphics.Bitmap;

import com.netease.nimlib.sdk.msg.model.RecentContact;

public class RecentContactItem {

    private RecentContact recentContact;
    private Boolean needFresh;
    private Bitmap teamIcon;

    public void setNeedFresh(Boolean needFresh) {
        this.needFresh = needFresh;
    }

    public Boolean getNeedFresh() {
        return needFresh;
    }

    public void setRecentContact(RecentContact recentContact) {
        this.recentContact = recentContact;
    }

    public RecentContact getRecentContact() {
        return recentContact;
    }

    public void setTeamIcon(Bitmap teamIcon) {
        this.teamIcon = teamIcon;
    }

    public Bitmap getTeamIcon() {
        return teamIcon;
    }

    public RecentContactItem(){

    }

    public RecentContactItem(RecentContact recentContact,Boolean b){
        this.recentContact = recentContact;
        this.needFresh = b;
    }
}
