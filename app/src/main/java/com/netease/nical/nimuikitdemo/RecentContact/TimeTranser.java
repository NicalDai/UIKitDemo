package com.netease.nical.nimuikitdemo.RecentContact;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeTranser {

    public static String chargeSecondsToNowTime(Long seconds) {

        long timeGap = System.currentTimeMillis() - seconds - System.currentTimeMillis()%(24*3600*1000);


        if(timeGap < 24*3600*1000 && timeGap > 0){
            return "昨天";
        }

        if(timeGap >= 24*3600*1000 && timeGap <= 48*3600*1000){
            return "前天";
        }

        if(timeGap > 72*3600*1000){
            SimpleDateFormat format2 = new SimpleDateFormat("MM月dd日");
            return format2.format(new Date(seconds));
        }

        SimpleDateFormat format2 = new SimpleDateFormat("HH:mm");
        return format2.format(new Date(seconds));
    }
}
