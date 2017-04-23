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
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by amrezzat on 4/6/2017.
 */

public class StartIntent {
    private final String LOG_TAG = StartIntent.class.getSimpleName();

    StartIntent(Context context, DefaultHashMap<String, String> data) {
        Intent intent = null;
        switch (data.get("type")) {
            case "alarm set":
                intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, data.get("name"))
                        .putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(data.get("hour")))
                        .putExtra(AlarmClock.EXTRA_MINUTES, Integer.parseInt(data.get("minute")));
                break;
            case "call":
                intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + data.get("number")));
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "Call Permission Not Granted");
                    return;
                }
                break;
            case "contact":
                ContentResolver cr = context.getContentResolver();
                Cursor contactLookup = cr.query(ContactsContract.Contacts.CONTENT_URI,
                        null, null, null, null);
                if (contactLookup != null && contactLookup.getCount() > 0) {
                    String id;
                    while (contactLookup.moveToNext()) {
                        String required_name = "abd zidan";
                        required_name = required_name.replace(" ", "(.*)").toLowerCase();
                        String name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
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
                                    while (phoneLookup.moveToNext()) {
                                        System.out.println(phoneLookup.getString(phoneLookup.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA)));
                                    }
                                    phoneLookup.close();
                                }
                            }
                            contactLookup.close();
                            break;
                        }
                    }
                }
                break;
            case "email":
                intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                        .putExtra(Intent.EXTRA_EMAIL, new String[]{data.get("email")})
                        .putExtra(Intent.EXTRA_SUBJECT, data.get("subject"))
                        .putExtra(Intent.EXTRA_TEXT, data.get("message"));
                break;
            case "calender set":
                //TODO
                Calendar beginTime = Calendar.getInstance();
                beginTime.set(2012, 0, 19, 7, 30); //set(year, month, day, hours, minutes)
                Calendar endTime = Calendar.getInstance();
                endTime.set(2012, 0, 19, 8, 30);
                intent = new Intent(Intent.ACTION_INSERT)
                        .setData(Events.CONTENT_URI)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis());
                break;
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
