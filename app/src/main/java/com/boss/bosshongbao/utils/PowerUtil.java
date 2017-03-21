package com.boss.bosshongbao.utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;


public class PowerUtil {
    private PowerManager.WakeLock wakeLock;
    private KeyguardManager.KeyguardLock keyguardLock;

    public PowerUtil(Context context) {
        Log.e("傻X------------------", "+_+");
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "HongbaoWakelock");
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock = km.newKeyguardLock("HongbaoKeyguardLock");
    }

    private void acquire() {
        Log.e("傻X------------------", "" + 3);
        wakeLock.acquire(1800000);
        keyguardLock.disableKeyguard();
    }

    private void release() {
        if (wakeLock.isHeld()) {
            Log.e("傻X------------------", "" + 2);
            wakeLock.release();
            keyguardLock.reenableKeyguard();
        }
    }

    public void handleWakeLock(boolean isWake) {
        if (isWake) {
            Log.e("傻X------------------", "" + 0);
            this.acquire();
        } else {
            Log.e("傻X------------------", "" + 1);
            this.release();
        }
    }
}
