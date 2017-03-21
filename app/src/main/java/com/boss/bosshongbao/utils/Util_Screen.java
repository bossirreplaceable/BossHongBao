package com.boss.bosshongbao.utils;


import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

/**
 * 另外，根据flags不同的值，实现不同级别的电源管理。
 * 关于int flags 各种锁的类型对CPU 、屏幕、键盘的影响：
 * PARTIAL_WAKE_LOCK :保持CPU 运转，屏幕和键盘灯有可能是关闭的。
 * SCREEN_DIM_WAKE_LOCK ：保持CPU 运转，允许保持屏幕显示但有可能是灰的，允许关闭键盘灯
 * SCREEN_BRIGHT_WAKE_LOCK ：保持CPU 运转，允许保持屏幕高亮显示，允许关闭键盘灯
 * FULL_WAKE_LOCK ：保持CPU 运转，保持屏幕高亮显示，键盘灯也保持亮度.
 */

/**
 * 手机解/锁屏  工具类
 * @author ThinkBear
 */

public class Util_Screen {


    private KeyguardManager keyguardManager;
    private PowerManager powerManager;

    private PowerManager.WakeLock wakeLoc = null;
    private KeyguardManager.KeyguardLock keyguardLock = null;

    private Context context = null;

    private static Util_Screen screen = null;

    private static boolean isUnlocking = false;

    public static boolean isUnlocking() {
        return isUnlocking;
    }

    public static Util_Screen getInitialize(Context context) {
        if (screen == null) {
            screen = new Util_Screen(context);
        }
        Log.e("Unlock+++++++++++","2");
        return screen;
    }

    private Util_Screen(Context context) {
        this.context = context;
        this.keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    /**
     * 当前是否为亮屏状态
     *
     * @return
     */
    public boolean isScreenLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return this.powerManager.isInteractive();
        } else {
            return this.powerManager.isScreenOn();
        }
    }

    /**
     * 当前是否为锁屏状态
     *
     * @return
     */
    public boolean isScreenLock() {
        return this.keyguardManager.inKeyguardRestrictedInputMode();
    }

    public void doUnlockScreen() {
        try {
            Log.e("Unlock+++++++++++","3");
            if(!this.isScreenLight()){
                this.wakeLoc = this.powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
                this.wakeLoc.acquire();
                Util_Screen.isUnlocking = true;
                Log.e("Unlock+++++++++++","4");
            }
            if (this.isScreenLock()) {
                Log.e("Unlock+++++++++++","5");
                this.keyguardLock = keyguardManager.newKeyguardLock("My Lock");
                this.keyguardLock.disableKeyguard();
                Util_Screen.isUnlocking = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doLockScreen() {
        Util_Screen.isUnlocking = false;
        try {
            if (this.keyguardLock != null) {
                this.keyguardLock.reenableKeyguard();
                this.keyguardLock = null;
            }
            if (this.wakeLoc != null) {
                this.wakeLoc.release();
                this.wakeLoc = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}