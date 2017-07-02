package com.sourcey.linachatbot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by amrezzat on 6/28/2017.
 */

public class NotesDatabaseHandler extends SQLiteOpenHelper {
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "usersManager";

    // Contacts table name
    private static final String TABLE_NOTES = "notes";

    // Contacts Table Columns names
    private static final String KEY_TITLE = "title";
    private static final String KEY_TEXT = "text";

    public NotesDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_NOTES + "("
                + KEY_TITLE + " TEXT PRIMARY KEY," +
                KEY_TEXT + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);

        // Create tables again
        onCreate(db);
    }

    // Adding new note
    public void addNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, note.getTitle()); // Note Title
        values.put(KEY_TEXT, note.getText()); // Note Text
        // Inserting Row
        db.insert(TABLE_NOTES, null, values);
        db.close(); // Closing database connection
    }

    // Getting single note
    public Note getNote(String noteTitle) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOTES, new String[]{KEY_TITLE, KEY_TEXT}, KEY_TITLE + "=?",
                new String[]{noteTitle}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        if (cursor.isBeforeFirst())
            return null;

        Note note = new Note(cursor.getString(0), cursor.getString(1));
        cursor.close();
        // return contact
        return note;
    }

    // Getting All Notes
    public List<Note> getAllNotes() {
        List<Note> noteList = new ArrayList<Note>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_NOTES;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setTitle(cursor.getString(0));
                note.setText(cursor.getString(1));

                // Adding note to list
                noteList.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return note list
        return noteList;
    }

    // Getting notes Count
    public int getNotesCount() {
        String countQuery = "SELECT  * FROM " + TABLE_NOTES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        // return count
        return cursor.getCount();
    }

    // Updating single note
    public int updateNoteText(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TEXT, note.getText());

        // updating row
        return db.update(TABLE_NOTES, values, KEY_TITLE + " = ?",
                new String[]{String.valueOf(note.getTitle())});
    }

    // Deleting single note
    public void deleteNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, KEY_TITLE + " = ?", new String[]{note.getTitle()});
        db.close();
    }

    //Return last created note
    public Note getLastNote(){
        Note note = getAllNotes().get(getAllNotes().size() - 1);
        return note;
    }
}
