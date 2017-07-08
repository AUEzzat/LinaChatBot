package com.sourcey.linachatbot;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity implements OnTaskCompleted {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    @Bind(R.id.input_user_or_email)
    EditText _username_or_email;
    @Bind(R.id.input_password)
    EditText _passwordText;
    @Bind(R.id.btn_login)
    Button _loginButton;
    @Bind(R.id.link_signup)
    TextView _signupLink;

    private String usernameOrEmail;
    private String username;
    private String email;
    private String password;
    private String retrievedUsername;
    private String retrievedEmail;
    private String retrievedPassword;
    private String Token;
    private String serverStatus;
    private ProgressDialog progressDialog;
    private Handler delayHandler = new android.os.Handler();
    private Runnable delayRunnable = new Runnable() {
        public void run() {
            serverStatus = "Failed to reach server";
            onLoginFailed();
            progressDialog.dismiss();
        }
    };
    private String toastText;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
//        DefaultHashMap<String, String> data = new DefaultHashMap<>("");
//        data.put("type", "contact");
//        data.put("contact","Amr Ezzat");
//        new StartIntent(getBaseContext(), data);
        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        User user = getUserState();
        if (user != null) {
            _username_or_email.setText(user.getuserNameOrEmail());
            _passwordText.setText(user.getPassword());
        }

    }


    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }
        // authentication logic.
        getToken getToken = new getToken(null, null, null, LoginActivity.this);
        getToken.setType(0);
        getToken.execute(username, email, password);

        _loginButton.setEnabled(false);

        progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();
        delayHandler.postDelayed(delayRunnable, 20000);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP && resultCode == RESULT_OK && data != null) {
            _username_or_email.setText(data.getStringExtra("username_or_email"));
            _passwordText.setText(data.getStringExtra("password"));
            login();
        }
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        _loginButton.setEnabled(false);
        Intent chatIntent = new Intent(getBaseContext(), MainActivity.class);
        chatIntent.putExtra("token", Token);
//        DatabaseHandler db = new DatabaseHandler(this);
//        if(db.getUser(username) == null)
//            db.addUser(new User(username, email, password, Token));
//        else
//            db.updateUserToken(new User(Token));
//        Log.d("logged in: ", username);
//        db.close();
        User user = new User(usernameOrEmail, password, Token);
        saveUserState(user);
        setResult(RESULT_OK, chatIntent);
        finish();
    }

    public User getUserState() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Gson gson = new Gson();
        String json = mPrefs.getString("user", "");
        if (!json.equals("")) {
            User user = gson.fromJson(json, User.class);
            return user;
        }
        return null;
    }

    public void saveUserState(User user) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        prefsEditor.putString("user", json);
        prefsEditor.commit();
    }

    public void onLoginFailed() {
        if (serverStatus != null && serverStatus.equals("bad input")) {
            toastText = "Bad Login Credentials";
            if (retrievedUsername != null && retrievedUsername.equals("This username is not valid.")) {
                _username_or_email.setError("Username doesn't exist");
                toastText = "Username doesn't exist";
            }
            if (retrievedEmail != null && (retrievedEmail.equals("This email is not valid.") || retrievedEmail.equals("Enter a valid email address."))) {
                _username_or_email.setError("Email doesn't exist");
                toastText = "Email doesn't exist";
            }
            if (retrievedPassword != null && retrievedPassword.equals("Incorrect credentials please try again")) {
                _passwordText.setError("Incorrect password");
                toastText = "Incorrect password";
            }
        } else if (serverStatus == null && toastText == null) {
            toastText = "Couldn't reach server";
        }
        else {
            toastText = serverStatus;
        }
        if (toastText == null) {
            toastText = "Something went wrong please try again";
        }
            new CustomToast(getBaseContext(), toastText, false);
            _loginButton.setEnabled(true);
        }

    public boolean validate() {
        boolean valid = true;

        usernameOrEmail = _username_or_email.getText().toString();
        password = _passwordText.getText().toString();

        if (!usernameOrEmail.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(usernameOrEmail).matches()) {
            email = usernameOrEmail;
        } else if (!usernameOrEmail.isEmpty() && Pattern.compile("^[a-z0-9_-]{3,15}$").matcher(usernameOrEmail).matches()) {
            username = usernameOrEmail;
        } else {
            _username_or_email.setError("Enter a valid username or a valid email");
            toastText = "Enter a valid username or a valid email";
            valid = false;
        }
        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("Password must be between 4 and 10 alphanumeric characters");
            toastText = "Password must be between 4 and 10 alphanumeric characters";
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    public void onTaskCompleted(DefaultHashMap<String, String> data) {
        Token = data.get("token");
        retrievedUsername = data.get("username");
        retrievedEmail = data.get("email");
        retrievedPassword = data.get("password");
        serverStatus = data.get("server status");
        // On complete call either onLoginSuccess or onLoginFailed
        if (serverStatus.equals("success") && !Token.equals("")) {
            onLoginSuccess();
        } else {
            onLoginFailed();
        }
        progressDialog.dismiss();
        delayHandler.removeCallbacks(delayRunnable);
    }
}
