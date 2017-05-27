package com.luh.giec.giecota.util;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Administrator on 2017/5/26.
 */


    public class SystemUtils {
        /**
         * 获取手机型号
         *
         * @return
         */
        public static String getPhoneModel() {
            return android.os.Build.MODEL;
        }

        /**
         * 获取手机Android API等级（22、23 ...）
         *
         * @return
         */
        public static int getBuildLevel() {
            return android.os.Build.VERSION.SDK_INT;
        }

        /**
         * 获取手机Android 版本（4.4、5.0、5.1 ...）
         *
         * @return
         */
        public static String getBuildVersion() {
            return android.os.Build.VERSION.RELEASE;
        }

        /**
         * 获取手机品牌
         *
         * @return
         */
        public static String getPhoneBrand() {
            return android.os.Build.BRAND;
        }

        /**
         * 获取设备的唯一标识，deviceId
         *
         * @param context
         * @return
         */
        public static String getDeviceId(Context context) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = tm.getDeviceId();
            if (deviceId == null) {
                return "";
            } else {
                return deviceId;
            }
        }

        public static String getDate(){
            SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            return formatter.format(curDate);
        }

        public static int countDays(String date){
            SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            try {
                Date lastDate = formatter.parse(date);
                GregorianCalendar cal1 = new GregorianCalendar();
                GregorianCalendar cal2 = new GregorianCalendar();
                cal1.setTime(curDate);
                cal2.setTime(lastDate);
                return (int) ((cal1.getTimeInMillis()-cal2.getTimeInMillis())/(1000*3600*24));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

