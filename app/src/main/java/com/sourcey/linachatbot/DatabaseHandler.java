package com.sourcey.linachatbot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by amrezzat on 4/28/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper {
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 2;

    // Database Name
    private static final String DATABASE_NAME = "usersManager";

    // Contacts table name
    private static final String TABLE_USERS = "users";

    // Contacts Table Columns names
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_TOKEN = "token";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_USERNAME + " TEXT PRIMARY KEY," +
                KEY_EMAIL + " TEXT," + KEY_PASSWORD + " TEXT,"
                + KEY_TOKEN + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

        // Create tables again
        onCreate(db);
    }

    // Adding new user
    public void addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, user.getUserName()); // User Name
        values.put(KEY_EMAIL, user.getEmail()); // User Email
        values.put(KEY_PASSWORD, user.getPassword()); // User Password
        values.put(KEY_TOKEN, user.getToken()); // User Token
        // Inserting Row
        db.insert(TABLE_USERS, null, values);
        db.close(); // Closing database connection
    }

    // Getting single contact
    public User getUser(String userName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_USERNAME,
                        KEY_EMAIL, KEY_PASSWORD, KEY_TOKEN}, KEY_USERNAME + "=?",
                new String[]{userName}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        if(cursor.isBeforeFirst())
            return null;

        User user = new User(cursor.getString(0),
                cursor.getString(1), cursor.getString(2), cursor.getString(3));
        cursor.close();
        // return contact
        return user;
    }

    // Getting All Users
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<User>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_USERS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setUserName(cursor.getString(0));
                user.setEmail(cursor.getString(1));
                user.setPassword(cursor.getString(2));
                user.setToken(cursor.getString(3));
                // Adding user to list
                userList.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return contact list
        return userList;
    }

    // Getting users Count
    public int getUsersCount() {
        String countQuery = "SELECT  * FROM " + TABLE_USERS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        // return count
        return cursor.getCount();
    }

    // Updating single user
    public int updateUserToken(User user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TOKEN, user.getToken());

        // updating row
        return db.update(TABLE_USERS, values, KEY_USERNAME + " = ?",
                new String[]{String.valueOf(user.getUserName())});
    }

    // Deleting single user
    public void deleteUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USERS, KEY_USERNAME + " = ?",
                new String[]{user.getUserName()});
        db.close();
    }
    //Return last  created user
    public User getLastUser(){
        User user = getAllUsers().get(getAllUsers().size() - 1);
        return user;
    }
}
