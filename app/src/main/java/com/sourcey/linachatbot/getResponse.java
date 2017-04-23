package com.sourcey.linachatbot;

import android.app.Activity;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

/**
 * Created by amrezzat on 3/18/2017.
 */

public class getResponse extends WebSocketClient {

    private final String LOG_TAG = getResponse.class.getSimpleName();
    private OnTaskCompleted listener;
    private Activity activity;
    Boolean open = false;


    public getResponse(URI serverUri, Draft draft, OnTaskCompleted listener, Activity activity) {
        super(serverUri, draft);
        this.listener = listener;
        this.activity = activity;
    }

    public getResponse(URI serverURI, OnTaskCompleted listener, Activity activity) {
        super(serverURI);
        this.listener = listener;
        this.activity = activity;
    }

    @Override
    public void onOpen(ServerHandshake handShakeData) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new CustomToast(activity, "Connected", true);
            }
        });
        Log.i(LOG_TAG, "opened connection");
        open = true;

    }

    @Override
    public void onMessage(String message) {
        Log.i(LOG_TAG, "received: " + message);
        JSONObject replyJSON;
        try {
            replyJSON = new JSONObject(message);
            String replyMsg = replyJSON.getString("msg");
            String owner = replyJSON.getString("owner");
            String messageTime = replyJSON.getString("formated_timestamp");
            DefaultHashMap<String, String> data = new DefaultHashMap<>("");
            data.put("type", "getResponse");
            data.put("formattedTime",messageTime);
            data.put("message", replyMsg);
            if(owner.equals("bot")) {
                listener.onTaskCompleted(data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new CustomToast(activity, "Disconnected", true);
            }
        });
        Log.i(LOG_TAG, "Connection closed by " + (remote ? "remote peer" : "us")  + " with code " + code);
        open = false;
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        // if the error is fatal then onClose will be called additionally
    }
}
