package com.sourcey.linachatbot;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.util.ArraySet;
import android.util.Log;

import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by amrezzat on 4/6/2017.
 */

public class StartIntent {
    private final String LOG_TAG = StartIntent.class.getSimpleName();

    public static Set<String> reqPerms;

    StartIntent(Context context, DefaultHashMap<String, String> data, OnTaskCompleted listener, Activity activity) throws JSONException {
        Intent intent;
        JSONArray intentJSONArray = new JSONArray(data.get("intentData"));
        String noteTitle;
        String noteText;
        NotesDatabaseHandler noteData;
        Note note;
        int noteCount;
        reqPerms = new ArraySet<>();
        String message = "";
        for (int i = 0; i < intentJSONArray.length(); i++) {
            intent = null;
            JSONArray currentJSONArray = intentJSONArray.getJSONArray(i);
            DefaultHashMap<String, String> currentIntentData = new DefaultHashMap<>("");
            currentIntentData.put("type", currentJSONArray.getString(0));
            for (int j = 1; j < currentJSONArray.length(); j++) {
                String s = currentJSONArray.getString(j);
                currentIntentData.put(s.substring(0, s.indexOf('(')),
                        s.substring(s.indexOf('(') + 2, s.indexOf(')') - 1));
            }
            Log.v("ss",currentIntentData.toString());
            Log.v("sss", currentJSONArray.toString());
            switch (currentIntentData.get("type")) {
                case "start_timer":
                    message += '\n' + String.format(Locale.UK, "Timer of %s:%02d started at %s.", currentIntentData.get("minute"),
                            Integer.parseInt(currentIntentData.get("second")), DateFormat.getTimeInstance().format(new Date()));
                    data.put("extra_timer", "start_timer");
                    data.put("extra_minute", currentIntentData.get("minute"));
                    data.put("extra_second", currentIntentData.get("second"));
                    break;

                case "set_alarm":

                    int minute;
                    try {
                        minute = Integer.parseInt(currentIntentData.get("minute"));
                    } catch (Exception e) {
                        minute = 0;
                        e.printStackTrace();
                    }
                    intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                            .putExtra(AlarmClock.EXTRA_MESSAGE, currentIntentData.get("title"))
                            .putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(currentIntentData.get("hour")))
                            .putExtra(AlarmClock.EXTRA_MINUTES, minute);
                    message += '\n' + String.format("%s alarm set at %s:%s.", currentIntentData.get("title"), currentIntentData.get("hour"),
                            String.format(Locale.UK, "%02d", minute));
                    break;

                case "next_alarm":
                    String nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
                    if (!nextAlarm.equals("")) {
                        message += String.format("next alarm at %s", nextAlarm);
                    } else {
                        message += "no alarm was found";
                    }
                    break;

                case "call_number":
                    if (EasyPermissions.hasPermissions(context, Manifest.permission.CALL_PHONE)) {
                        intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + currentIntentData.get("number")));
                        message += String.format("called %s.", currentIntentData.get("number"));
                    } else {
                        Log.d(LOG_TAG, "Call Permission Denied");
                        reqPerms.add(Manifest.permission.CALL_PHONE);
                    }
                    break;


                case "call_contact":
                    if (!EasyPermissions.hasPermissions(context, Manifest.permission.CALL_PHONE)) {
                        Log.d(LOG_TAG, "Call Contact Permission Denied");
                    }
                case "message_contact":
                    if (!EasyPermissions.hasPermissions(context, Manifest.permission.SEND_SMS) && currentIntentData.get("type").equals("message_contact")) {
                        reqPerms.add(Manifest.permission.CALL_PHONE);
                        Log.d(LOG_TAG, "Message Contact Permission Denied");
                    }
                case "view_contact":
                    if (!EasyPermissions.hasPermissions(context, Manifest.permission.READ_CONTACTS)) {
                        reqPerms.add(Manifest.permission.READ_CONTACTS);
                        Log.d(LOG_TAG, "Access Contacts Permission Denied");
                        break;
                    } else if (!EasyPermissions.hasPermissions(context, Manifest.permission.SEND_SMS) && currentIntentData.get("type").equals("message_contact")) {
                        break;
                    } else if (!EasyPermissions.hasPermissions(context, Manifest.permission.CALL_PHONE) && currentIntentData.get("type").equals("call_contact")) {
                        break;
                    }
                    String phoneNumber = "";
                    String contactName = "";
                    ContentResolver cr = context.getContentResolver();
                    Cursor contactLookup = cr.query(ContactsContract.Contacts.CONTENT_URI,
                            null, null, null, null);
                    if (contactLookup != null && contactLookup.getCount() > 0) {
                        String id;
                        while (contactLookup.moveToNext()) {
                            String required_name = currentIntentData.get("contact_name");
                            required_name = required_name.replace(" ", "(.*)").toLowerCase();
                            String name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                            String[] nameList = name.split(" ");
                            int rnLength = required_name.split(" ").length;
                            name = "";
                            for (int k = 0; k < rnLength; k++) {
                                name = name + nameList[k];
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
                        message += String.format("No contact by the name %s was found.", currentIntentData.get("contact_name"));
                        break;
                    } else if (phoneNumber.equals("")) {
                        message += String.format("No phone number was found for %s.", contactName);
                        break;
                    }

                    if (currentIntentData.get("type").equals("call_contact")) {

                        intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                        message += String.format("called %s(%s)", currentIntentData.get("contact_name"), phoneNumber);

                    } else if (currentIntentData.get("type").equals("message_contact")) {

                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumber));
                        intent.putExtra("sms_body", currentIntentData.get("message"));
                        message += String.format("%s is sent to %s(%s).", currentIntentData.get("message"), contactName, phoneNumber);
                    } else {
                        message += String.format("%s info:\nPhone number: %s.", contactName, phoneNumber);
                    }

                    break;

                case "message_number":
                    if (EasyPermissions.hasPermissions(context, Manifest.permission.SEND_SMS)) {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + currentIntentData.get("number")));
                        intent.putExtra("sms_body", currentIntentData.get("message"));
                        message += String.format("%s is sent to %s.", currentIntentData.get("message"), currentIntentData.get("number"));
                    } else {
                        reqPerms.add(Manifest.permission.SEND_SMS);
                        Log.d(LOG_TAG, "Message Number Permission Denied");
                    }
                    break;


                case "send_email":
                    intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                            .putExtra(Intent.EXTRA_EMAIL, new String[]{currentIntentData.get("email")})
                            .putExtra(Intent.EXTRA_SUBJECT, currentIntentData.get("subject"))
                            .putExtra(Intent.EXTRA_TEXT, currentIntentData.get("body"));
                    String body = currentIntentData.get("body");
                    String subject = currentIntentData.get("subject");
                    if (body.equals("")) {
                        body = "(empty)";
                    }
                    if (!subject.equals("")) {
                        subject = subject + ",";
                    }
                    message += String.format("%s\n%s\n\nis sent to %s.", subject, body, currentIntentData.get("email"));
                    break;

                case "set_event":
                    if (EasyPermissions.hasPermissions(context, Manifest.permission.WRITE_CALENDAR)) {
                        String[] startStr = currentIntentData.get("start_time").split(":");
                        ArrayList<Integer> startInt = new ArrayList<>();
                        Calendar beginTime = Calendar.getInstance();
                        try {
                            for (String s : startStr) startInt.add(Integer.valueOf(s));
                            switch (startInt.size()) {
                                case 5:
                                    beginTime.set(startInt.get(0), startInt.get(1), startInt.get(2), startInt.get(3), startInt.get(4));
                                    break;
                                case 3:
                                    beginTime.set(startInt.get(0), startInt.get(1), startInt.get(2));
                                    break;
                                case 2:
                                    beginTime.set(Calendar.HOUR_OF_DAY, startInt.get(0));
                                    beginTime.set(Calendar.MINUTE, startInt.get(1));
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //set(year, month, day, hours, minutes)
                        String[] endStr = currentIntentData.get("end_time").split(":");
                        ArrayList<Integer> endInt = new ArrayList<>();
                        Calendar endTime = Calendar.getInstance();
                        try {
                            for (String s : endStr) endInt.add(Integer.valueOf(s));
                            switch (endInt.size()) {
                                case 5:
                                    endTime.set(endInt.get(0), endInt.get(1), endInt.get(2), endInt.get(3), endInt.get(4));
                                    break;
                                case 3:
                                    endTime.set(endInt.get(0), endInt.get(1), endInt.get(2));
                                    break;
                                case 2:
                                    endTime.set(Calendar.HOUR_OF_DAY, endInt.get(0));
                                    endTime.set(Calendar.MINUTE, endInt.get(1));
                                    break;
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        intent = new Intent(Intent.ACTION_INSERT)
                                .setData(Events.CONTENT_URI)
                                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
                                .putExtra(Events.TITLE, currentIntentData.get("title"))
                                .putExtra(Events.DESCRIPTION, currentIntentData.get("description"))
                                .putExtra(Events.EVENT_LOCATION, currentIntentData.get("location"));
                        String title = currentIntentData.get("title");
                        String location = currentIntentData.get("location");
                        if (!currentIntentData.get("description").equals("")) {
                            title = title + ":";
                        }
                        if (!location.equals("")) {
                            location = "at " + location + ".";
                        }
                        message += String.format("%s\n%s\nfrom %s\n to %s\n%s", title, currentIntentData.get("description"),
                                beginTime.getTime(), endTime.getTime(), location);
                    } else {
                        reqPerms.add(Manifest.permission.WRITE_CALENDAR);
                        Log.d(LOG_TAG, "Calender Permission Denied");
                    }
                    break;

                case "show_date_time":
                    String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                    message += String.format("Current Date and Time is: %s.", currentDateTimeString);
                    break;

                case "show_date":
                    String currentDateString = DateFormat.getDateInstance().format(new Date());
                    message += String.format("Current Date is: %s.", currentDateString);
                    break;

                case "show_time":
                    String currentTimeString = DateFormat.getTimeInstance().format(new Date());
                    message += String.format("Current Time is: %s.", currentTimeString);
                    break;

                case "save_note":
                    noteTitle = currentIntentData.get("title");
                    noteData = new NotesDatabaseHandler(context);
                    note = noteData.getNote(noteTitle);
                    if (note != null) {
                        message += "note already exists\nuse edit note to edit text\nor choose another title.";
                    } else {
                        noteText = currentIntentData.get("text");
                        note = new Note(noteTitle, noteText);
                        noteData.addNote(note);
                        if (!noteText.equals("")) {
                            noteTitle += ":\n";
                        }
                        message += String.format("your note\n%s%s saved.", noteTitle, noteText);
                    }
                    break;

                case "show_note":
                    noteTitle = currentIntentData.get("title");
                    noteData = new NotesDatabaseHandler(context);
                    note = noteData.getNote(noteTitle);
                    if (note == null) {
                        message += String.format("%no note by the name %s was found.", noteTitle);
                    } else {
                        noteText = note.getText();
                        noteTitle = "note " + noteTitle;
                        if (!noteText.equals("")) {
                            noteTitle += ":\n";
                        }
                        message += String.format("showing %s%s.", noteTitle, noteText);
                    }
                    break;
                case "edit_note":
                    noteTitle = currentIntentData.get("title");
                    noteData = new NotesDatabaseHandler(context);
                    note = noteData.getNote(noteTitle);
                    if (note == null) {
                        message += String.format("%s note doesn't exist.", noteTitle);
                    } else {
                        noteText = currentIntentData.get("text");
                        note.setText(noteText);
                        noteData.updateNoteText(note);
                        noteTitle = "note " + noteTitle + " text updated to";
                        if (!noteText.equals("")) {
                            noteTitle += ":\n";
                        }
                        message += String.format("note %s%s\n updated.", noteTitle, noteText);
                    }
                    break;

                case "remove_note":
                    noteTitle = currentIntentData.get("title");
                    noteData = new NotesDatabaseHandler(context);
                    note = noteData.getNote(noteTitle);
                    if (note == null) {
                        message += String.format("no note by the name %s was found.", noteTitle);
                    } else {
                        noteData.deleteNote(note);
                        message += String.format("your %s note is deleted.", noteTitle);
                    }
                    break;

                case "show_last_note":
                    noteData = new NotesDatabaseHandler(context);
                    noteCount = noteData.getNotesCount();
                    if (noteCount > 0) {
                        note = noteData.getLastNote();
                        noteTitle = note.getTitle();
                        noteText = note.getText();
                        noteTitle = "note " + noteTitle;
                        if (!noteText.equals("")) {
                            noteTitle += ":\n";
                        }
                        message += String.format("last note is %s%s", noteTitle, noteText);
                    } else {
                        message += "you have no notes to show";
                    }
                    break;

                case "show_all_notes":

                    noteData = new NotesDatabaseHandler(context);
                    noteCount = noteData.getNotesCount();
                    message += "your notes are:\n";
                    if (noteCount > 0) {
                        List<Note> notes = noteData.getAllNotes();
                        int counter = 0;
                        for (Note currentNote : notes) {
                            noteTitle = currentNote.getTitle();
                            noteTitle = String.format("note %s %s", ++counter, noteTitle);
                            noteText = currentNote.getText();
                            if (!noteText.equals("")) {
                                noteTitle += ":\n";
                            }
                            message += String.format("%s%s\n", noteTitle, noteText);
                        }
                    } else {
                        message += "you have no notes to show";
                    }
                    break;

                case "delete_all_notes":
                    data.put("extra_notes", "delete_all_notes");
                    break;

                case "play_trailer":
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(data.get("trailer_link")));
                    message += "playing trailer";
                    break;

                case "display_message":
                    message += data.get("message");
                    break;
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
            message = WordUtils.capitalizeFully(message + "\n\n", '\n');
        }
        if (reqPerms.size() > 0) {
            String[] perms = new String[reqPerms.size()];
            perms = new ArrayList<>(reqPerms).toArray(perms);
            EasyPermissions.requestPermissions(activity, "Grant Permission to allow application functionality", 0, perms);
        }
        if (!message.equals("")) {
            data.put("type", "message");
            data.put("message", message.trim());
            data.put("extra_type", "intent");
            if (reqPerms.size() > 0) {
                data.put("extra_perm", intentJSONArray.toString());
            }
            listener.onTaskCompleted(data);
        }
    }

}
