package com.sourcey.linachatbot;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by amrezzat on 4/6/2017.
 */

public class StartIntent {
    private final String LOG_TAG = StartIntent.class.getSimpleName();

    private OnTaskCompleted listener;

    StartIntent(Context context, DefaultHashMap<String, String> data, OnTaskCompleted listener) {
        Intent intent = null;
        String message = null;
        switch (data.get("name")) {

            case "start_timer":
                message = String.format(Locale.UK, "Timer of %s:%02d started at %s", data.get("minute"), Integer.parseInt(data.get("second")),
                        data.get("formattedTime"));
                break;

            case "set_alarm":
                int minute;
                try {
                    minute = Integer.parseInt(data.get("minute"));
                } catch (Exception e) {
                    minute = 0;
                    e.printStackTrace();
                }
                intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, data.get("title"))
                        .putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(data.get("hour")))
                        .putExtra(AlarmClock.EXTRA_MINUTES, minute);
                message = String.format("%s alarm set at %s:%s", data.get("title"), data.get("hour"), String.format(Locale.UK, "%02d", minute));
                break;

            case "view_next_alarm":
                String nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
                if(!nextAlarm.equals("")) {
                    message = String.format("next alarm at %s", nextAlarm);
                }
                else {
                    message = "no alarm was found";
                }
                break;

            case "call_number":
                intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + data.get("number")));
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "Call Permission Not Granted");
                    message = "Call Permission Not Granted";
                    return;
                }
                break;

            case "view_contact":
            case "call_contact":
            case "message_contact":
                String phoneNumber = "";
                String contactName = "";
                ContentResolver cr = context.getContentResolver();
                Cursor contactLookup = cr.query(ContactsContract.Contacts.CONTENT_URI,
                        null, null, null, null);
                if (contactLookup != null && contactLookup.getCount() > 0) {
                    String id;
                    while (contactLookup.moveToNext()) {
                        String required_name = data.get("contact_name");
                        required_name = required_name.replace(" ", "(.*)").toLowerCase();
                        String name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        String[] nameList = name.split(" ");
                        int rnLength = required_name.split(" ").length;
                        name = "";
                        for (int i = 0; i < rnLength; i++) {
                            name = name + nameList[i];
                        }
                        if (name.toLowerCase().matches(required_name)) {
                            id = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Contacts._ID));
                            intent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(id));
                            intent.setData(uri);
                            if (Integer.parseInt(contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                                Cursor phoneLookup = cr.query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        null,
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                        new String[]{id}, null);
                                if (phoneLookup != null) {
                                    phoneLookup.moveToNext();
                                    phoneNumber = phoneLookup.getString(phoneLookup.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));
                                    phoneLookup.close();
                                }
                            }
                            contactName = name;
                            contactLookup.close();
                            break;
                        }
                    }
                }
                if (contactName.equals("")) {
                    message = String.format("No contact by the name %s was found.", capitalize(data.get("contact_name")));
                    break;
                } else if (phoneNumber.equals("")) {
                    message = String.format("No phone number was found for %s.", capitalize(contactName));
                    break;
                }
                if (data.get("name").equals("call_contact")) {
                    intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        message = "Call Permission Not Granted";
                        Log.d(LOG_TAG, "Call Permission Not Granted");
                        return;
                    }
                }
                else if (data.get("name").equals("message_contact")) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumber));
                    intent.putExtra("sms_body", data.get("message"));
                    message = String.format("%s is sent to %s(%s)", data.get("message"), capitalize(contactName), phoneNumber);
                }
//                    else {
//                        message = String.format("%s info:%nPhone number: %s", contactName, phoneNumber);
//                    }

                break;

            case "message_number":
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + data.get("number")));
                intent.putExtra("sms_body", data.get("message"));
                message = String.format("%s is sent to %s", data.get("message"), data.get("number"));

            case "send_email":
                intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                        .putExtra(Intent.EXTRA_EMAIL, new String[]{data.get("email")})
                        .putExtra(Intent.EXTRA_SUBJECT, data.get("subject"))
                        .putExtra(Intent.EXTRA_TEXT, data.get("text"));
                String text = data.get("text");
                String subject = data.get("subject");
                if (text.equals("")) {
                    text = "(empty)";
                }
                if (!subject.equals("")) {
                    subject = subject + ",";
                }
                message = String.format("%s%n%s%n%nis sent to %s", subject, text, data.get("email"));
                break;

            case "set_event":
                String[] startStr = data.get("start_time").split(":");
                ArrayList<Integer> startInt = new ArrayList<>();
                Calendar beginTime = Calendar.getInstance();
                try {
                    for (String s : startStr) startInt.add(Integer.valueOf(s));
                    beginTime.set(startInt.get(0), startInt.get(1), startInt.get(2), startInt.get(3), startInt.get(4));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //set(year, month, day, hours, minutes)
                String[] endStr = data.get("end_time").split(":");
                ArrayList<Integer> endInt = new ArrayList<>();
                Calendar endTime = Calendar.getInstance();
                try {
                    for (String s : endStr) endInt.add(Integer.valueOf(s));
                    endTime.set(endInt.get(0), endInt.get(1), endInt.get(2), endInt.get(3), endInt.get(4));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                intent = new Intent(Intent.ACTION_INSERT)
                        .setData(Events.CONTENT_URI)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
                        .putExtra(Events.TITLE, data.get("title"))
                        .putExtra(Events.DESCRIPTION, data.get("description"))
                        .putExtra(Events.EVENT_LOCATION, data.get("location"));
                String title = data.get("title");
                String location = data.get("location");
                if (!data.get("description").equals("")) {
                    title = title + ":";
                }
                if (!location.equals("")) {
                    location = "at " + location;
                }
                message = String.format("%s%n%s%nfrom %s%nto %s%n%s", title, data.get("description"),
                        beginTime.getTime(), endTime.getTime(), location);
                break;

            case "show_date_time":
                String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                message = String.format("Current Date and Time is: %s", currentDateTimeString);
                break;
            case "show_date":
                String currentDateString = DateFormat.getDateInstance().format(new Date());
                message = String.format("Current Date is: %s", currentDateString);
                break;
            case "show_time":
                String currentTimeString = DateFormat.getTimeInstance().format(new Date());
                message = String.format("Current Time is: %s", currentTimeString);
                break;

        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }

        if (message != null) {
            message = capitalize(message.trim());
            data.put("type", "message");
            data.put("message", message);
            listener.onTaskCompleted(data);
        }
    }

    private String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }
}
