package com.sourcey.linachatbot;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by amrezzat on 3/10/2017.
 */

public class getToken extends AsyncTask<String, Void, DefaultHashMap<String, String>> {
    private Activity activity;
    private Context context;
    private String progressMsg;
    private OnTaskCompleted listener;
    private String type = null;

    public void setType(int type) {
        switch (type) {
            case 0:
                this.type = "login";
                break;
            case 1:
                this.type = "register";
        }
    }

    public getToken(Activity activity, Context context, String progressMsg, OnTaskCompleted listener) {
        this.activity = activity;
        this.context = context;
        this.progressMsg = progressMsg;
        this.listener = listener;
    }

    private final String LOG_TAG = getToken.class.getSimpleName();

    protected DefaultHashMap<String, String> doInBackground(String... params) {
        Boolean canRetrieveData = false;
        Boolean canReachServer = false;
        String tokenJSONStr = null;
        String username = params[0];
        String email = params[1];
        String password = params[2];
        StringBuilder sb = new StringBuilder();
        DefaultHashMap<String, String> data = new DefaultHashMap<>("");
        HttpURLConnection urlConnection = null;
        try {
            Uri.Builder chatBotUrl = new Uri.Builder();
            chatBotUrl.scheme("https")
                    .authority("linachatbot.herokuapp.com")
                    .appendPath("api")
                    .appendPath("accounts")
                    .appendPath(type);
            URL url = new URL(chatBotUrl.toString() + "/");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.connect();
            //Create JSONObject here
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("username", username);
            jsonParam.put("email", email);
            if (type.equals("register")) {
                jsonParam.put("email2", email);
            }
            jsonParam.put("password", password);
            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(jsonParam.toString());
            out.close();

            int HttpResult = urlConnection.getResponseCode();
            final String HttpResultStr = urlConnection.getResponseMessage();
            canRetrieveData = Arrays.asList(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_ACCEPTED,
                    HttpURLConnection.HTTP_CREATED).contains(HttpResult);
            canReachServer = Arrays.asList(HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_UNAUTHORIZED,
                    HttpURLConnection.HTTP_FORBIDDEN, HttpURLConnection.HTTP_NOT_FOUND).contains(HttpResult);
            if (canRetrieveData) {
                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                tokenJSONStr = sb.toString();
            } else if (canReachServer) {
                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                tokenJSONStr = sb.toString();
            } else {
                Log.i(LOG_TAG, HttpResultStr);
                data.put("server status", "failed to reach server");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
        try {
            if (canRetrieveData) {
                data.put("server status", "success");
                if (type.equals("login")) {
                    data.put("token", new JSONObject(tokenJSONStr).getString("token"));
                } else {
                    data.put("username", new JSONObject(tokenJSONStr).getString("username"));
                    data.put("email", new JSONObject(tokenJSONStr).getString("email"));
                    data.put("password", password);
                }
            }
            if (canReachServer) {
                data.put("server status", "bad input");
                if (type.equals("login")) {
                    //TODO: leave empty or add empty string to token
                } else {
                    JSONObject jObject = new JSONObject(tokenJSONStr);
                    Iterator<String> keys = jObject.keys();
                    while(keys.hasNext()) {
                        String nextKey = keys.next();
                        if(nextKey.equals("username")) {
                            data.put("username", new JSONObject(tokenJSONStr).getJSONArray("username").getString(0));
                        }
                        else if(nextKey.equals("email")) {
                            data.put("email", new JSONObject(tokenJSONStr).getJSONArray("email").getString(0));
                        }
                    }
                    if(!data.containsKey("username")) {
                        data.put("username", username);
                    }
                    if(!data.containsKey("email")) {
                        data.put("email", email);
                    }
                    data.put("password", password);
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return data;
    }

    @Override
    protected void onPostExecute(DefaultHashMap<String, String> data) {
        if (data.size() != 0) {
            listener.onTaskCompleted(data);
        }
    }
}
