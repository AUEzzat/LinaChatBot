package com.sourcey.linachatbot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.internal.NavigationMenuView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.ArraySet;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.androidadvance.topsnackbar.TSnackbar;
import com.google.gson.Gson;
import com.sourcey.linachatbot.NetworkStateReceiver.NetworkStateReceiverListener;
import com.squareup.picasso.Picasso;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.intentservice.chatui.ChatView;
import co.intentservice.chatui.models.ChatMessage;
import pub.devrel.easypermissions.EasyPermissions;

import static com.sourcey.linachatbot.characterNumber.get;

public class MainActivity extends AppCompatActivity implements OnTaskCompleted,
        NetworkStateReceiverListener, OnFragmentClickListener, EasyPermissions.PermissionCallbacks {

    @Bind(R.id.main_view)
    DrawerLayout mDrawer;
    @Bind(R.id.nav_view)
    NavigationView navDrawer;
    @Bind(R.id.chat_view)
    ChatView chatView;

    private static final int REQUEST_AUTHENTICATION = 0;

    private getResponse getResponse;
    private String token;
    private TSnackbar snackbar;
    private MenuItem characterType;
    private String character;
    private boolean connected = false;
    private boolean retrievedMessages = false;
    private boolean retrievingMessages = false;
    private NetworkStateReceiver networkStateReceiver;
    private ArrayList<String> messagesID = new ArrayList<>();
    private Map<String, DefaultHashMap<String, String>> messagesMap = new HashMap<>();
    private int counterId = 10000;
    private String TimerID;
    private String HolderID;
    private boolean closing = false;
    private Set<String> waitingPerm = new ArraySet<>();
    private String waitingJSON;


    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        for(int i =0;i<list.size();i++){
            waitingPerm.add(list.get(i));
        }
        if(waitingPerm.equals(StartIntent.reqPerms)) {
            DefaultHashMap<String, String> waitingMap = new DefaultHashMap<>("");
            waitingMap.put("intentData", waitingJSON);
            try {
                new StartIntent(getBaseContext(), waitingMap, MainActivity.this, MainActivity.this);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        String messageText = "Permissions denied; Couldn't do intended actions.";
        messagesID.add(HolderID);
        DefaultHashMap<String, String> currentMessageMap = new DefaultHashMap<>("");
        currentMessageMap.put("type", "not_editable");
        currentMessageMap.put("message", messageText);
        messagesMap.put(HolderID, currentMessageMap);
        final ChatMessage message =
                new ChatMessage(messageText, System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatView.addMessage(message);
            }
        });
        sendMessageHelper(token, "history", messageText, TimerID, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onFragmentClick(int action, DefaultHashMap<String, String> details) {
        DefaultHashMap<String, String> data = new DefaultHashMap<>("");
        if (details != null) {
            data.putAll(details);
        }
        if (action == 0) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(null, data.get("message"));
            clipboard.setPrimaryClip(clip);
            new CustomToast(getBaseContext(), "Message copied to the clipboard", true);
        } else if (action == 1) {
            String imageUrl = data.get("image_url");
//            String videoUrl = data.get("video_url");
            String message = data.get("message").replace(imageUrl, "");
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            dialog.setTitle("Message content");
            View view = inflater.inflate(R.layout.show_message_content, null); // xml Layout file for imageView
            ImageView img = (ImageView) view.findViewById(R.id.content_image);
            TextView txt = (TextView) view.findViewById(R.id.content_text);
            if (!imageUrl.equals("")) {
                Picasso.with(MainActivity.this)
                        .load(imageUrl.replace("Http", "http"))
                        .placeholder(R.drawable.no_photo_placeholder)
                        .into(img);
                img.setBackgroundColor(getResources().getColor(R.color.primary));
            } else {
                img.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                txt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            if (message.trim().equals("")) {
                img.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            txt.setText(message);
            dialog.setView(view);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog.show();
        } else if (action == 2) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            // Create and show the dialog.
            ShowDialog messageDialog = new ShowDialog();
            Bundle dialogBundle = new Bundle();
            dialogBundle.putSerializable("data", details);
            dialogBundle.putInt("carry_id", 10);
            dialogBundle.putString("redB", "OK");
            dialogBundle.putString("greenB", "Cancel");
            messageDialog.setArguments(dialogBundle);
            messageDialog.show(ft, "dialog");
        } else if (action == 15) {
            if (data.get("message").equals("")) {
                new CustomToast(getBaseContext(), "Message can't be empty", true);
                return;
            }
            sendMessageHelper(token, "history", data.get("message"), data.get("id"), false);
        } else if (action == 25) {
            deleteChatHistory clearChatHistory = new deleteChatHistory();
            clearChatHistory.execute();
        } else if (action == 35 || action == 37) {
            String messageText = data.get("message");
            String messageID = data.get("id");
            final ChatMessage message = new ChatMessage(messageText, System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chatView.addMessage(message);
                }
            });
            sendMessageHelper(token, "history", messageText, messageID, false);

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (mDrawer != null && mDrawer.isDrawerOpen(GravityCompat.END)) {
                mDrawer.closeDrawers();
                return false;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        closing = true;
        if (networkStateReceiver != null) {
            try {
                unregisterReceiver(networkStateReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("token", token);
        outState.putString("character", character);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString("character", character);
        prefsEditor.commit();
    }

    private boolean sendMessageHelper(String token, String command, String message, String id, boolean dialog) {
        if (getResponse != null && getResponse.open) {
            if (!dialog) {
                messagesID.add(id);
                DefaultHashMap<String, String> currentMessageMap = messageMediaSeparator(message);
                currentMessageMap.put("type", "not_editable");
                messagesMap.put(id, currentMessageMap);
            }
            JSONObject messageJSON = new JSONObject();
            try {
                messageJSON.put("command", command);
                messageJSON.put("message", message);
                messageJSON.put("token", token);
                if (command.equals("history")) {
                    messageJSON.put("msg_id", id);
                } else {
                    messageJSON.put("character", get(character));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                new CustomToast(getBaseContext(), "failed to send message", true);
                return false;
            }
            getResponse.send(messageJSON.toString());
            return true;
        } else {
            setGetResponse(token);
            new CustomToast(getBaseContext(), "failed to send message", true);
            return false;
        }
    }

    private void sendButton(final String token) {
        chatView.setOnSentMessageListener(new ChatView.OnSentMessageListener() {

            @Override
            public boolean sendMessage(ChatMessage ChatMessage) {
                return sendMessageHelper(token, "send", ChatMessage.getMessage(), Integer.toString(counterId++), false);
            }
        });
    }


    @Override
    public void networkAvailable() {
        if (!retrievedMessages) {
            getOldMessages getOldMessages = new getOldMessages();
            getOldMessages.execute("jwt " + token, "10");
        }
        if (snackbar != null) {
            snackbar.dismiss();
        }
        connected = true;
        setGetResponse(token);
    }

    @Override
    public void networkUnavailable() {
        snackbar = TSnackbar.make(mDrawer, "Not Connected", TSnackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        }).setActionTextColor(Color.WHITE);
        connected = false;
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.parseColor("#FF8A80"));
        snackbar.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        setupDrawerContent(navDrawer);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        character = mPrefs.getString("character", "Casual");
        navDrawer.setCheckedItem(characterNumber.getID(get(character)));
        ((TextView) navDrawer.getHeaderView(0).findViewById(R.id.drawer_header)).setText(character);
        ((de.hdodenhof.circleimageview.CircleImageView) navDrawer.getHeaderView(0).findViewById(R.id.circle_view))
                .setImageResource(characterNumber.getImg(get(character)));
        mDrawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                ((NavigationMenuView) navDrawer.getChildAt(0)).smoothScrollToPosition(0);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
        setTitle(character + " Lina");

        //listener to mouse clicks on chat bubbles
        final ListView messages = (ListView) chatView.getChildAt(0);
        messages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DefaultHashMap<String, String> messageDetails = new DefaultHashMap<>("");
                messageDetails.putAll(messagesMap.get(messagesID.get(position)));
                messageDetails.put("id", messagesID.get(position));
                ArrayList<String> shownItems = new ArrayList<>();
                shownItems.add("Copy");
                shownItems.add("Display message");
                if (!messageDetails.get("type").equals("not_editable")) {
                    shownItems.add("Edit reply");
                }
//                new CustomToast(getBaseContext(), messagesID.get(position), true);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                // Create and show the dialog.
                ShowDialog messageDialog = new ShowDialog();
                Bundle dialogBundle = new Bundle();
                dialogBundle.putStringArrayList("list", shownItems);
                dialogBundle.putSerializable("data", messageDetails);
                messageDialog.setArguments(dialogBundle);
                messageDialog.show(ft, "dialog");
            }
        });


        Gson gson = new Gson();
        String json = mPrefs.getString("user", "");
        if (!json.equals("")) {
            User user = gson.fromJson(json, User.class);
            double timeDifference = (System.nanoTime() - user.getTokenTime()) / 1e9;
            if (timeDifference < 86400) {
                this.token = user.getToken();
                getOldMessages getOldMessages = new getOldMessages();
                getOldMessages.execute("jwt " + this.token, "10");
                setGetResponse(this.token);
                sendButton(this.token);
                return;
            }
        }
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, 0);
    }

    private void setGetResponse(String token) {
        Uri.Builder webSocketUriBuilder = new Uri.Builder();
        webSocketUriBuilder.scheme("wss")
                .authority("linachatbot.herokuapp.com")
                .appendPath("api")
                .appendPath("chat")
                .appendQueryParameter("token", token);
        URI webSocketUri = null;
        try {
            webSocketUri = new URI(webSocketUriBuilder.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        getResponse = new getResponse(webSocketUri, MainActivity.this, this);
        if (webSocketUriBuilder.toString().indexOf("wss") == 0) {
            try {
                SSLContext sslContext = SSLContext.getDefault();
                getResponse.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        getResponse.connect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_AUTHENTICATION && resultCode == RESULT_OK && data != null) {
            final String token = data.getStringExtra("token");
            this.token = "jwt " + token;
            getOldMessages getOldMessages = new getOldMessages();
            getOldMessages.execute(this.token, "10");
            setGetResponse(token);
            sendButton(token);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        characterType = menu.findItem(R.id.my_activity);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.my_activity) {
            if (mDrawer.isDrawerOpen(GravityCompat.END)) {
                mDrawer.closeDrawer(GravityCompat.END);
            } else {
                mDrawer.openDrawer(GravityCompat.END);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void clearPref() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.clear();
        prefsEditor.commit();
    }

    private void setupDrawerContent(final NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.action_logout) {
                            clearPref();
                            Intent mStartActivity = new Intent(getBaseContext(), MainActivity.class);
                            int mPendingIntentId = 123456;
                            PendingIntent mPendingIntent = PendingIntent.getActivity(getBaseContext(), mPendingIntentId,
                                    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager mgr = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                            System.exit(0);
                        } else if (menuItem.getItemId() == R.id.action_delete_chat_history) {
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            // Create and show the dialog.
                            ShowDialog messageDialog = new ShowDialog();
                            Bundle dialogBundle = new Bundle();
                            dialogBundle.putString("message", "Are you sure you want to clear all chat history?");
                            dialogBundle.putString("title", "Delete chat history?");
                            dialogBundle.putString("redB", "Delete");
                            dialogBundle.putString("greenB", "Cancel");
                            dialogBundle.putInt("carry_id", 20);
                            messageDialog.setArguments(dialogBundle);
                            messageDialog.show(ft, "dialog");
                            return true;
                        } else if (menuItem.getItemId() == R.id.action_help) {
                            return true;
                        }
                        setTitle(menuItem.getTitle() + " Lina");
                        characterType.setTitle(menuItem.getTitle());
                        ((NavigationMenuView) navigationView.getChildAt(0)).smoothScrollToPosition(0);
                        View drawerHeader = navigationView.getHeaderView(0);
                        ((TextView) drawerHeader.findViewById(R.id.drawer_header)).setText(menuItem.getTitle());
                        ((de.hdodenhof.circleimageview.CircleImageView) drawerHeader.findViewById(R.id.circle_view))
                                .setImageResource(characterNumber.getImg(get(menuItem.getTitle().toString())));
                        character = menuItem.getTitle().toString();
                        menuItem.setChecked(true);
//                        mDrawer.closeDrawers();
                        return true;
                    }
                });
    }

    private void setTimer(final int minute, final int second, final String id) {
        final String message = String.format(Locale.UK, "Timer of %s:%02d started at %s", minute, second,
                DateFormat.getTimeInstance().format(new Date()));
        final TSnackbar timerBar = TSnackbar.make(
                mDrawer,
                message,
                TSnackbar.LENGTH_INDEFINITE);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final CountDownTimer timer = new CountDownTimer(minute * 60000 + second * 1000, 1000) {

                    public void onTick(long millisUntilFinished) {
                        timerBar.setText(String.format(Locale.UK, "Remaining time %s:%02d", (int) millisUntilFinished / 60000,
                                millisUntilFinished % 60000 / 1000));
                    }

                    public void onFinish() {
                        timerBar.setText("Done!");
                        timerBar.dismiss();
                        String messageText = String.format("%s\nTimer stopped", message);
                        final ChatMessage message = new ChatMessage("Timer stopped", System.currentTimeMillis(), ChatMessage.Type.RECEIVED);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                chatView.addMessage(message);
                            }
                        });
                        sendMessageHelper(token, "history", messageText, TimerID, true);
                        DefaultHashMap<String, String> temp = messagesMap.get(TimerID);
                        temp.put("message", messageText);
                        messagesMap.put(TimerID, temp);
                        messagesID.add(TimerID);
                    }
                };
                timer.start();
                timerBar.setAction("Stop", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timer.onFinish();
                        timer.cancel();
                    }
                }).setActionTextColor(Color.WHITE);
                View timerBarView = timerBar.getView();
                timerBarView.setBackgroundColor(Color.parseColor("#ce0e0e"));
                timerBar.show();
            }
        });
    }

    @Override
    public void onTaskCompleted(final DefaultHashMap<String, String> data) {
        String type = data.get("type");
        if (type.equals("close")) {
            if (getResponse.getReadyState() == 3 && !closing) {
                getResponse.connect();
            }
            return;
        }
        Long formattedTime;
        try {
            formattedTime = Long.parseLong(data.get("formattedTime"));
        } catch (Exception e) {
            formattedTime = System.currentTimeMillis();
        }
        if (type.equals("message")) {
            String messageText = data.get("message");
            String id = data.get("id");
            String lineId = data.get("line_id");
            if(data.containsKey("extra_perm")) {
                waitingJSON = data.get("extra_perm");
                HolderID = id;
                return;
            }
            if (data.get("extra_timer").equals("start_timer")) {
                setTimer(Integer.parseInt(data.get("extra_minute")), Integer.parseInt(data.get("extra_second")), id);
                TimerID = id;
            }
            if (data.get("extra_notes").equals("delete_all_notes")) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                // Create and show the dialog.
                ShowDialog messageDialog = new ShowDialog();
                Bundle dialogBundle = new Bundle();
                dialogBundle.putString("message", "Are you sure you want to delete all notes?");
                dialogBundle.putString("title", "Delete All Notes");
                dialogBundle.putString("redB", "Yes");
                dialogBundle.putString("greenB", "No");
                dialogBundle.putInt("carry_id", 30);
                messageDialog.setArguments(dialogBundle);
                messageDialog.show(ft, "dialog");
                return;
            }
            if (data.get("extra_type").equals("intent")) {
                sendMessageHelper(token, "history", messageText, id, true);
            }

            messagesID.add(id);
            DefaultHashMap<String, String> currentMessageMap = messageMediaSeparator(messageText);
            if (lineId.equals("null")) {
                currentMessageMap.put("type", "not_editable");
            }
            currentMessageMap.put("pMessage", data.get("previousMessage"));
            messagesMap.put(id, currentMessageMap);
            final ChatMessage message = new ChatMessage(messageText, formattedTime, ChatMessage.Type.RECEIVED);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chatView.addMessage(message);
                }
            });
        } else if (type.equals("intent")) {

            runOnUiThread(new Runnable() {
                public void run() {
                    try {

                        new StartIntent(getBaseContext(), data, MainActivity.this, MainActivity.this);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private DefaultHashMap<String, String> messageMediaSeparator(String messageText) {
        DefaultHashMap<String, String> currentMessageMap = new DefaultHashMap<>("");
        String videoPattern = "https?:\\/\\/(?:[0-9A-Z-]+\\.)?" +
                "(?:youtu\\.be\\/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*";
        Pattern compiledVideoPattern = Pattern.compile(videoPattern, Pattern.CASE_INSENSITIVE);
        Matcher videoMatcher = compiledVideoPattern.matcher(messageText);
        String videoUrl = "";
        if (videoMatcher.find()) {
            videoUrl = videoMatcher.group();
            currentMessageMap.put("video_url", videoUrl);
        }
        String imagePattern = "https?:/(?:/[^/]+/?)+\\.(?:jpg|gif|png)";
        Pattern compiledImagePattern = Pattern.compile(imagePattern, Pattern.CASE_INSENSITIVE);
        Matcher imageMatcher = compiledImagePattern.matcher(messageText);
        String imageUrl = "";
        if (imageMatcher.find()) {
            imageUrl = imageMatcher.group();
            currentMessageMap.put("image_url", imageUrl);
        }
        currentMessageMap.put("message", messageText);
        return currentMessageMap;
    }

    public class deleteChatHistory extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            Uri.Builder deleteChatHistoryUrl = new Uri.Builder();
            HttpURLConnection urlConnection = null;

            deleteChatHistoryUrl.scheme("https")
                    .authority("linachatbot.herokuapp.com")
                    .appendPath("api")
                    .appendPath("chat")
                    .appendPath("messages")
                    .appendPath("delete");
            try {
                URL url = new URL(deleteChatHistoryUrl.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("DELETE");
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);
                urlConnection.setRequestProperty("Authorization", "jwt " + token);
                urlConnection.setUseCaches(false);
                urlConnection.connect();
                final int HttpResultCode = urlConnection.getResponseCode();
                if (HttpResultCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    return true;
                } else {
                    Log.i("failed: ", Integer.toString(HttpResultCode));
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean != null && aBoolean) {
                new CustomToast(getBaseContext(), "History is cleared", true);
                Intent mStartActivity = new Intent(getBaseContext(), MainActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(getBaseContext(), mPendingIntentId,
                        mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
            } else {
                new CustomToast(getBaseContext(), "Couldn't clear history", true);
            }
        }
    }

    public class getOldMessages extends AsyncTask<String, Void, ArrayList<ChatMessage>> {

        private final String LOG_TAG = getOldMessages.class.getSimpleName();

        private ArrayList<ChatMessage> getOldMessagesFromJson(String oldMessagesJsonStr) throws JSONException, IOException, ParseException {
            if (retrievingMessages || retrievedMessages) {
                return null;
            }
            retrievingMessages = true;
            Log.i(LOG_TAG, oldMessagesJsonStr);
            JSONObject oldMessagesJsonObj = new JSONObject(oldMessagesJsonStr);
            JSONArray oldMessagesJsonArray = oldMessagesJsonObj.getJSONArray("results");
            ArrayList<ChatMessage> oldMessages = new ArrayList<>(oldMessagesJsonArray.length());
            for (int i = 0; i < oldMessagesJsonArray.length(); i++) {
                JSONObject oldMessage = oldMessagesJsonArray.getJSONObject(i);
                String id = oldMessage.getString("id");
                String lineId;
                try {
                    lineId = oldMessage.getString("lineId");
                } catch (JSONException e) {
                    lineId = "null";
                }
                String messageText = oldMessage.getString("message");
                messagesID.add(0, id);
                DefaultHashMap<String, String> currentMessageMap = messageMediaSeparator(messageText);
                if (lineId.equals("null")) {
                    currentMessageMap.put("type", "not_editable");
                }

                if (messageText.equals("")) {
                    messageText = "¯\\_(ツ)_/¯";
                }
                String humanUser = oldMessage.getString("owner");
                Long messageTime = Long.parseLong(oldMessage.getString("formated_timestamp"));
                ChatMessage.Type messageType = ChatMessage.Type.RECEIVED;
                if (humanUser.equals("user")) {
                    messageType = ChatMessage.Type.SENT;
                } else if (i + 1 < oldMessagesJsonArray.length()) {
                    currentMessageMap.put("pMessage", oldMessagesJsonArray.getJSONObject(i + 1).getString("message"));
                }
                messagesMap.put(id, currentMessageMap);
                oldMessages.add(new ChatMessage(messageText, messageTime, messageType));
            }
            Collections.reverse(oldMessages);
            return oldMessages;
        }

        protected ArrayList<ChatMessage> doInBackground(String... params) {
            String token = params[0];
            String oldMessagesRetrieveLimit = params[1];
            StringBuilder sb = new StringBuilder();
            String oldMessagesJsonStr = "[]";
            Uri.Builder chatHistoryUrl = new Uri.Builder();
            HttpURLConnection urlConnection = null;
            try {
                chatHistoryUrl.scheme("https")
                        .authority("linachatbot.herokuapp.com")
                        .appendPath("api")
                        .appendPath("chat")
                        .appendPath("messages")
                        .appendQueryParameter("limit", oldMessagesRetrieveLimit);
                URL url = new URL(chatHistoryUrl.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setRequestMethod("GET");
                urlConnection.setUseCaches(false);
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);
                urlConnection.setRequestProperty("Authorization", token);
                urlConnection.connect();

                int HttpResult = urlConnection.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    oldMessagesJsonStr = sb.toString();
                } else {
                    Log.i(LOG_TAG, urlConnection.getResponseMessage());
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            try {
                return getOldMessagesFromJson(oldMessagesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            } catch (ParseException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<ChatMessage> oldMessages) {
            retrievingMessages = false;
            if (oldMessages != null && !retrievedMessages) {
                retrievedMessages = true;
                chatView.addMessages(oldMessages);
                if (snackbar != null) {
                    snackbar.dismiss();
                }
            } else if (connected && !retrievedMessages) {
                snackbar = TSnackbar.make(mDrawer, "Failed to retrieve old messages", TSnackbar.LENGTH_INDEFINITE);
                snackbar.setAction("Retry", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getOldMessages getOldMessages = new getOldMessages();
                        getOldMessages.execute("jwt " + token, "10");
                    }
                }).setActionTextColor(Color.WHITE);
                View snackbarView = snackbar.getView();
                snackbarView.setBackgroundColor(Color.parseColor("#FF8A80"));
                snackbar.show();
            } else if (!retrievedMessages) {
                new CustomToast(getBaseContext(), "failed to retrieve old messages", true);
            }
        }
    }
}
