package com.boss.bosshongbao.utils;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Created by Administrator on 2017/3/16.
 */

public class HongbaoSignatureQQ {
    public String sender, content, time, contentDescription = "", commentString;
    public boolean others;


    public boolean generateSignatureQQ(AccessibilityNodeInfo node, String excludeWords) {
        try {

            /* The hongbao container node. It should be a LinearLayout. By specifying that, we can avoid text messages. */
            AccessibilityNodeInfo hongbaoNode = node.getParent();
            if (!"android.widget.RelativeLayout".equals(hongbaoNode.getClassName())) return false;

            /* The text in the hongbao. Should mean something. */
            Log.e("QQ-------------2", hongbaoNode.getChild(0).getClassName().toString());
            String hongbaoContent = hongbaoNode.getChild(0).getText().toString();
            if (hongbaoContent == null || "查看红包".equals(hongbaoContent)) return false;

            /* Check the user's exclude words list. */
            String[] excludeWordsArray = excludeWords.split(" +");
            for (String word : excludeWordsArray) {
                if (word.length() > 0 && hongbaoContent.contains(word)) return false;
            }
            Log.e("QQ-------------3", 3 + "");

            /* The container node for a piece of message. It should be inside the screen.
                Or sometimes it will get opened twice while scrolling. */
            AccessibilityNodeInfo messageNode = hongbaoNode.getParent();

            Rect bounds = new Rect();
            messageNode.getBoundsInScreen(bounds);

            if (bounds.top < 0) return false;

            /* The sender and possible timestamp. Should mean something too. */
            String[] hongbaoInfo = getSenderContentDescriptionFromNode(messageNode);
            //if (this.getSignature(hongbaoInfo[0], hongbaoContent, hongbaoInfo[1]).equals(this.toString())) return false;
            Log.e("QQ-------------", 4 + "" + hongbaoInfo[0]);
            Log.e("QQ-------------", 5 + "" + hongbaoContent);
            /* So far we make sure it's a valid new coming hongbao. */
            this.sender = hongbaoInfo[0].replace(":","");
            this.time = hongbaoInfo[1];
            this.content = hongbaoContent;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String toString() {
        return this.getSignature(this.sender, this.content, this.time);
    }

    private String getSignature(String... strings) {
        String signature = "";
        for (String str : strings) {
            if (str == null) return null;
            signature += str + "|";
        }
        return signature.substring(0, signature.length() - 1);
    }

    public String getContentDescription() {
        return this.contentDescription;
    }

    public void setContentDescription(String description) {
        this.contentDescription = description;
    }

    private String[] getSenderContentDescriptionFromNode(AccessibilityNodeInfo node) {

        int count = node.getChildCount();
        String[] result = {"unknownSender", "unknownTime"};
        if (node.getChild(1).getClassName().equals("android.widget.TextView")) {
            result[0] = node.getChild(1).getText().toString();
        } else if (node.getChild(2).getClassName().equals("android.widget.TextView")) {
            result[0] = node.getChild(2).getText().toString();
        }
        return result;
    }

    public void cleanSignature() {
        this.content = "";
        this.time = "";
        this.sender = "";
    }

    private void oko() {


    }
}
