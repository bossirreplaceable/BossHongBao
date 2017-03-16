package com.boss.bosshongbao.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.graphics.Path;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.DisplayMetrics;


import com.boss.bosshongbao.utils.HongbaoSignature;
import com.boss.bosshongbao.utils.PowerUtil;

import java.util.List;

public class HongbaoService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String WECHAT_DETAILS_EN = "Details";
    private static final String WECHAT_DETAILS_CH = "红包详情";
    private static final String WECHAT_BETTER_LUCK_EN = "Better luck next time!";
    private static final String WECHAT_BETTER_LUCK_CH = "手慢了";
    private static final String WECHAT_EXPIRES_CH = "已超过24小时";
    private static final String WECHAT_VIEW_SELF_CH = "查看红包";
    private static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    private static final String WECHAT_NOTIFICATION_TIP = "[微信红包]";
    private static final String QQ_NOTIFICATION_TIP = "[QQ红包]";
    private static final String QQ_UNPICK_HONGBAO = "点击拆开";
    private static final String QQ_PASSWORD_HONGBAO = "口令红包";
    private static final String QQ_PASSWORD_ENTER = "点击输入口令";
    private static final String WECHAT_LUCKMONEY_DONE = "你领取了";
    private static final String QQ_PASSWORD_SEND = "发送";
    private static final String QQ_HONGBAO_ACTIVITY = "SplashActivity";
    private static final String WECHAT_LUCKMONEY_RECEIVE_ACTIVITY = "LuckyMoneyReceiveUI";
    private static final String WECHAT_LUCKMONEY_DETAIL_ACTIVITY = "LuckyMoneyDetailUI";
    private static final String WECHAT_LUCKMONEY_GENERAL_ACTIVITY = "LauncherUI";
    private static final String WECHAT_LUCKMONEY_CHATTING_ACTIVITY = "ChattingUI";
    private static final String QQ_PACKAGENAME = "com.tencent.mobileqq";
    private static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    private String currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;
    private boolean isNotification = false, isChatContent = false, isSend = false;
    private boolean isOpen = false, isUnpick = false, isGreetings = false;
    private AccessibilityNodeInfo rootNodeInfo, mReceiveNode, mUnpickNode, sendNode;
    //--------------------------------------------------------------------
    private boolean isNotificationQQ = false, isOpenQQ = false;
    private AccessibilityNodeInfo rootNodeInfoQQ;
    private HongbaoSignature signature = new HongbaoSignature();
    private PowerUtil powerUtil;
    private SharedPreferences sharedPreferences;
    private int bottomHongbao = 0;

    /**
     * AccessibilityEvent
     *
     * @param event 事件
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (sharedPreferences == null) return;
        setCurrentActivityName(event);
        Log.e("Name-----------", currentActivityName);
        if (sharedPreferences.getBoolean("pref_watch_notification", false)) {
            if (!isNotification && sharedPreferences.getBoolean("pref_wechat", false)) {
                watchNotifications(event);
            }
            if (!isNotificationQQ && sharedPreferences.getBoolean("pref_qq", false)) {
                watchNotificationsQQ(event);
            }
        }
        rootNodeInfo = getRootInActiveWindow();
        if (!isNotification && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            AccessibilityNodeInfo chatNode = getTheLastNode1(rootNodeInfo, WECHAT_VIEW_SELF_CH, WECHAT_VIEW_OTHERS_CH);
            if (chatNode != null) {
                chatNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                isChatContent = true;
            }
        }
        if (event.getClassName().equals("android.widget.Button") && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)) {

            AccessibilityNodeInfo sendNode1 = event.getSource();
            if (sendNode1 != null) {
                sendNode = sendNode1;
            }
        }
        if (isNotificationQQ && event.getPackageName().equals(QQ_PACKAGENAME)) {
            if (sharedPreferences.getBoolean("pref_watch_chat", false)) {
                watchQQ(event);
            }
        }
        if ((isNotification || isChatContent) && event.getPackageName().equals(WECHAT_PACKAGENAME)) {
            if (sharedPreferences.getBoolean("pref_watch_chat", false)) {
                watchChat(event);
            }

        }
        if (isGreetings && signature.commentString != null
                && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)) {
            sendComment();
            isGreetings = false;
            signature.commentString = null;
        }
        if (isSend && sendNode != null) {
            sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            isSend = false;
        }

    }

    private void watchChat(AccessibilityEvent event) {
        this.rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) return;
        mReceiveNode = null;
        mUnpickNode = null;
        checkNodeInfo(event.getEventType());


        /* 如果已经接收到红包并且还没有戳开 */
        if (isOpen && mReceiveNode != null) {
            mReceiveNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            isOpen = false;
        }
        /* 如果戳开但还未领取 */
        if (isUnpick && mUnpickNode != null) {
            int delayFlag = sharedPreferences.getInt("pref_open_delay", 0) * 1000;
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            try {
                                openPacket();
                            } catch (Exception e) {
                                isNotification = false;
                                isUnpick = false;
                            }
                        }
                    },
                    delayFlag);
        }
    }

    private void checkNodeInfo(int eventType) {
        if (this.rootNodeInfo == null) return;


        Log.e("Notification---------", "" + 1);
        /* 聊天会话窗口，遍历节点匹配“领取红包”和"查看红包" */
        if (currentActivityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY)
                || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)) {
            AccessibilityNodeInfo node1 = (sharedPreferences.getBoolean("pref_watch_self", false)) ?
                    this.getTheLastNode(rootNodeInfo, WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH) : this.getTheLastNode(rootNodeInfo, WECHAT_VIEW_OTHERS_CH);
            Log.e("Notification---------", "" + 2);
            if (node1 != null) {
                Log.e("Notification---------", "" + 3);
                String excludeWords = sharedPreferences.getString("pref_watch_exclude_words", "");
                if (this.signature.generateSignature(node1, excludeWords)) {
                    isOpen = true;
                    mReceiveNode = node1;
                    Log.e("Notification---------", "" + 4);
                }
                return;
            }
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        if (currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY)) {
            AccessibilityNodeInfo node2 = findOpenButton(this.rootNodeInfo);
            if (node2 != null && "android.widget.Button".equals(node2.getClassName())) {
                mUnpickNode = node2;
                isUnpick = true;
                Log.e("Notification---------", "" + 5);
                return;

            }
        }

       /* 红包抢完，从红包详情页返回到聊天界面 */
        if (currentActivityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY)) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            isNotification = false;
            isChatContent = false;
            isGreetings = true;
            signature.commentString = generateCommentString();
            Log.e("ET-----content---", signature.commentString);
        }
    }

    private void openPacket() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float dpi = metrics.density;
        if (android.os.Build.VERSION.SDK_INT <= 23) {
            mUnpickNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            if (android.os.Build.VERSION.SDK_INT > 23) {

                Path path = new Path();
                if (640 == dpi) {
                    path.moveTo(720, 1575);
                } else {
                    path.moveTo(540, 1060);
                }
                GestureDescription.Builder builder = new GestureDescription.Builder();
                GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 450, 50)).build();
                dispatchGesture(gestureDescription, new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        Log.e("test", "onCompleted");

                        super.onCompleted(gestureDescription);
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        Log.e("test", "onCancelled");

                        super.onCancelled(gestureDescription);
                    }
                }, null);

            }
        }
    }

    private void setCurrentActivityName(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        try {
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            getPackageManager().getActivityInfo(componentName, 0);
            currentActivityName = componentName.flattenToShortString();


        } catch (PackageManager.NameNotFoundException e) {
            currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;
        }
    }


    private void watchNotifications(AccessibilityEvent event) {
        // Not a notification
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return;
        }
        // Not a hongbao
        String tip = event.getText().toString();
        if (!tip.contains(WECHAT_NOTIFICATION_TIP)) {
            return;
        }

        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Notification notification = (Notification) parcelable;
            try {
                /* 清除signature,避免进入会话后误判 */
                signature.cleanSignature();
                isNotification = true;
                notification.contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }

    }

    private void watchNotificationsQQ(AccessibilityEvent event) {
        // Not a notification
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return;
        }
        // Not a hongbao
        String tip = event.getText().toString();
        if (!tip.contains(QQ_NOTIFICATION_TIP)) {
            return;
        }
        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Notification notification = (Notification) parcelable;
            try {
                /* 清除signature,避免进入会话后误判 */
                signature.cleanSignature();
                isNotificationQQ = true;
                notification.contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onInterrupt() {

    }

    private AccessibilityNodeInfo findOpenButton(AccessibilityNodeInfo node) {
        if (node == null)
            return null;

        //非layout元素
        if (node.getChildCount() == 0) {
            if ("android.widget.Button".equals(node.getClassName()))
                return node;
            else
                return null;
        }

        //layout元素，遍历找button
        AccessibilityNodeInfo button;
        for (int i = 0; i < node.getChildCount(); i++) {
            button = findOpenButton(node.getChild(i));
            if (button != null) {
                return button;
            }
        }
        return null;
    }

    private void sendComment() {
        try {
            AccessibilityNodeInfo etNode = findEditText(this.rootNodeInfo);
            Log.e("ET---------", "" + 8);
            if (etNode != null) {
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, signature.commentString);
                etNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                isSend = true;
            }

        } catch (Exception e) {
            isGreetings = false;

        }
    }

    private AccessibilityNodeInfo findEditText(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }

        //非layout元素
        if (node.getChildCount() == 0) {
            if ("android.widget.EditText".equals(node.getClassName())) {
                return node;
            } else {
                return null;
            }
        }
        //layout元素，遍历找button
        AccessibilityNodeInfo button;
        for (int i = 0; i < node.getChildCount(); i++) {
            button = findEditText(node.getChild(i));

            if (button != null) {
                Log.e("5-------------boss", button.getClassName().toString());
                return button;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo getTheLastNode(AccessibilityNodeInfo rootNode, String... texts) {
        int bottom = 0;
        AccessibilityNodeInfo lastNode = null, tempNode;
        List<AccessibilityNodeInfo> nodes;
        for (String text : texts) {
            if (text == null) continue;
            nodes = rootNode.findAccessibilityNodeInfosByText(text);
            Log.e("Nodes-------------", nodes.size() + "");
            if (nodes != null && !nodes.isEmpty()) {
                tempNode = nodes.get(nodes.size() - 1);
                if (tempNode == null) return null;
                Rect bounds = new Rect();
                tempNode.getBoundsInScreen(bounds);
                if (bounds.bottom > 0) {
                    bottomHongbao = bounds.bottom;
                }
                Log.e("Bottom---------1", bounds.bottom + "");
                if (bounds.bottom > bottom) {
                    bottom = bounds.bottom;
                    lastNode = tempNode;
                    signature.others = text.equals(WECHAT_VIEW_OTHERS_CH);

                }
            }
        }
        return lastNode;
    }

    private void watchQQ(AccessibilityEvent event) {
        this.rootNodeInfoQQ = getRootInActiveWindow();
        if (rootNodeInfoQQ == null) return;

        if (isOpenQQ) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            isOpenQQ = false;
            isNotificationQQ = false;
            return;
        }
        if (sharedPreferences.getBoolean("pref_qq_greetings", false)) {
            AccessibilityNodeInfo node1 = this.getTheLastNode(rootNodeInfoQQ, QQ_PASSWORD_HONGBAO);
            if (node1 != null) {
                node1.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                //Enter the password into input box.
                List<AccessibilityNodeInfo> node2 = this.rootNodeInfoQQ.findAccessibilityNodeInfosByText(QQ_PASSWORD_ENTER);
                if (node2 != null && node2.size() > 0) {
                    node2.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    //get the button of send,click
                    AccessibilityNodeInfo nowNode = getRootInActiveWindow();
                    AccessibilityNodeInfo sendNode = findOpenButton(nowNode);
                    if (sendNode != null) {
                        sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        isOpenQQ = true;
                    }
                }
            }
        }
        //grab ordinary red envelops
        AccessibilityNodeInfo node1 = this.getTheLastNode(rootNodeInfoQQ, QQ_UNPICK_HONGBAO);
        if (node1 != null) {
            String excludeWords = sharedPreferences.getString("pref_watch_exclude_words", "");
//            if (this.signature.generateSignature(node1, excludeWords)) {
            node1.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            isOpenQQ = true;
//            } else {
//                isNotificationQQ = false;
//            }
            return;

        }
    }

    private AccessibilityNodeInfo getTheLastNode1(AccessibilityNodeInfo rootNode, String... texts) {
        AccessibilityNodeInfo lastNode = null, tempNode;
        List<AccessibilityNodeInfo> nodes;
        if (bottomHongbao <= 0) return null;

        for (String text : texts) {
            if (text == null) continue;
            nodes = rootNode.findAccessibilityNodeInfosByText(text);
            if (nodes != null && !nodes.isEmpty()) {
                tempNode = nodes.get(nodes.size() - 1);
                if (tempNode == null) return null;
                Rect bounds = new Rect();
                tempNode.getBoundsInScreen(bounds);

                if (bounds.bottom == bottomHongbao) {
                    lastNode = tempNode;
                    Log.e("Success-----------", "yes");
                }
            }
        }
        return lastNode;
    }


    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        this.watchFlagsFromPreference();
    }

    private void watchFlagsFromPreference() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.powerUtil = new PowerUtil(this);
        Boolean watchOnLockFlag = sharedPreferences.getBoolean("pref_watch_on_lock", false);
        this.powerUtil.handleWakeLock(watchOnLockFlag);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_watch_on_lock")) {
            Boolean changedValue = sharedPreferences.getBoolean(key, false);
            this.powerUtil.handleWakeLock(changedValue);
        }
    }

    @Override
    public void onDestroy() {
        this.powerUtil.handleWakeLock(false);
        super.onDestroy();
    }

    private String generateCommentString() {
        if (!signature.others) return null;

//        Boolean needComment = sharedPreferences.getBoolean("pref_comment_switch", false);
//        if (!needComment) return null;

//        String[] wordsArray = sharedPreferences.getString("pref_comment_words", "").split(" +");
//        if (wordsArray.length == 0) return null;

//        Boolean atSender = sharedPreferences.getBoolean("pref_comment_at", false);
//        if (atSender) {
        return "@" + signature.sender;
        //+ " " + wordsArray[(int) (Math.random() * wordsArray.length)];
//        } else {
//            return wordsArray[(int) (Math.random() * wordsArray.length)];
//        }
    }
}


//    /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
//    boolean hasNodes = this.hasOneOfThoseNodes(
//            WECHAT_BETTER_LUCK_CH, WECHAT_DETAILS_CH,
//            WECHAT_BETTER_LUCK_EN, WECHAT_DETAILS_EN, WECHAT_EXPIRES_CH);
//-----------------------------------------------------------------------------
//    private boolean watchList(AccessibilityEvent event) {
//        if (mListMutex) return false;
//        mListMutex = true;
//        AccessibilityNodeInfo eventSource = event.getSource();
//        // Not a message
//        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventSource == null)
//            return false;
//
//
//        List<AccessibilityNodeInfo> nodes = eventSource.findAccessibilityNodeInfosByText(WECHAT_NOTIFICATION_TIP);
//        //增加条件判断currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
//        //避免当订阅号中出现标题为“[微信红包]拜年红包”（其实并非红包）的信息时误判
//        if (!nodes.isEmpty() && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)) {
//            AccessibilityNodeInfo nodeToClick = nodes.get(0);
//            if (nodeToClick == null) return false;
//            CharSequence contentDescription = nodeToClick.getContentDescription();
//            if (contentDescription != null && !signature.getContentDescription().equals(contentDescription)) {
//                nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                signature.setContentDescription(contentDescription.toString());
//                return true;
//            }
//        }
//        return false;
//    }