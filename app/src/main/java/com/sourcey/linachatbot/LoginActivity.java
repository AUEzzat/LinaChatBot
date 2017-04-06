package com.sourcey.linachatbot;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity implements OnTaskCompleted {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    @Bind(R.id.input_username)
    EditText _username;
    @Bind(R.id.input_email)
    EditText _emailText;
    @Bind(R.id.input_password)
    EditText _passwordText;
    @Bind(R.id.btn_login)
    Button _loginButton;
    @Bind(R.id.link_signup)
    TextView _signupLink;

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
            serverStatus = "failed to reach server";
            onLoginFailed();
            progressDialog.dismiss();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);


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
            _username.setText(data.getStringExtra("username"));
            _emailText.setText(data.getStringExtra("email"));
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
        setResult(RESULT_OK, chatIntent);
        finish();
    }

    public void onLoginFailed() {
        String toastText;
        if (serverStatus == null) {
            toastText = "Enter a valid data";
        } else if (serverStatus != null && serverStatus.equals("bad input")) {
            toastText = "Bad Login Credentials";
            if (retrievedUsername != null && retrievedUsername.equals("This username is not valid.")) {
                _username.setError("Username doesn't exist");
                toastText = "Username doesn't exist";
            }
            if (retrievedEmail != null && retrievedEmail.equals("This email is not valid.")) {
                _emailText.setError("Email doesn't exist");
                toastText = "Email doesn't exist";
            }
            if (retrievedPassword != null && retrievedPassword.equals("Incorrect credentials please try again")) {
                _passwordText.setError("Incorrect password");
                toastText = "Incorrect password";
            }
        } else {
            toastText = "Couldn't reach server";
        }
        Toast.makeText(getBaseContext(), toastText, Toast.LENGTH_LONG).show();
        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        username = _username.getText().toString();
        email = _emailText.getText().toString();
        password = _passwordText.getText().toString();

        if (username.isEmpty() || !Pattern.compile("^[a-z0-9_-]{3,15}$").matcher(username).matches()) {
            _username.setError("enter a valid username");
            valid = false;
        } else {
            _username.setError(null);
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }
        if (_username.getError() == null || _emailText.getError() == null) {
            valid = true;
        }
        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
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
