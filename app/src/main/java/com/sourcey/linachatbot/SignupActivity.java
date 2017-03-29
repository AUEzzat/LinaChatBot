package com.sourcey.linachatbot;

import android.app.Activity;
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

public class SignupActivity extends AppCompatActivity implements OnTaskCompleted {
    private static final String TAG = "SignupActivity";

    @Bind(R.id.input_username)
    EditText _username;
    @Bind(R.id.input_email)
    EditText _emailText;
    @Bind(R.id.input_password)
    EditText _passwordText;
    @Bind(R.id.input_reEnterPassword)
    EditText _reEnterPasswordText;
    @Bind(R.id.btn_signup)
    Button _signupButton;
    @Bind(R.id.link_login)
    TextView _loginLink;

    private String username;
    private String email;
    private String retrievedUsername;
    private String retrievedEmail;
    private String password;
    private String serverStatus;
    ProgressDialog progressDialog;
    private Handler delayHandler = new android.os.Handler();
    private Runnable delayRunnable = new Runnable() {
        public void run() {
            serverStatus = "failed to reach server";
            onSignupFailed();
            progressDialog.dismiss();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
    }

    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        // signup logic.
        getToken getToken = new getToken(new Activity(), getBaseContext(), "...", SignupActivity.this);
        getToken.setType(1);
        getToken.execute(username, email, password);

        _signupButton.setEnabled(false);

        progressDialog = new ProgressDialog(SignupActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        delayHandler.postDelayed(delayRunnable, 10000);
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    public void onSignupSuccess() {
        _signupButton.setEnabled(true);
        Intent signInIntent = new Intent(getBaseContext(), LoginActivity.class);
        signInIntent.putExtra("username", username);
        signInIntent.putExtra("email", email);
        signInIntent.putExtra("password", password);
        setResult(RESULT_OK, signInIntent);
        finish();
    }

    public void onSignupFailed() {
        String toastText;
        if (serverStatus == null) {
            toastText = "Enter a valid data";
        } else if (serverStatus != null && serverStatus.equals("bad input")) {
            toastText = "User already exists";
            if (retrievedUsername != null && retrievedUsername.equals("A user with that username already exists.")) {
                _username.setError("Username already exists choose another username or login");
                toastText = "Username already exists";
            }
            if (retrievedEmail != null && retrievedEmail.equals("This user has already registered")) {
                _emailText.setError("Email already registered please login");
                toastText = "Email already exists";
            }
        } else {
            toastText = "Couldn't reach server";
        }
        Toast.makeText(getBaseContext(), toastText, Toast.LENGTH_LONG).show();
        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        username = _username.getText().toString();
        email = _emailText.getText().toString();
        password = _passwordText.getText().toString();
        String reEnterPassword = _reEnterPasswordText.getText().toString();

        if (username.isEmpty() || !Pattern.compile("^[a-z0-9_-]{3,15}$").matcher(username).matches()) {
            _username.setError("enter a valid username,\n characters, numerals and [-,_] are allowed only");
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

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(password))) {
            _reEnterPasswordText.setError("Password Do not match");
            valid = false;
        } else {
            _reEnterPasswordText.setError(null);
        }

        return valid;
    }

    public void onTaskCompleted(DefaultHashMap<String, String> data) {

        retrievedUsername = data.get("username");
        retrievedEmail = data.get("email");
        serverStatus = data.get("server status");

        // On complete call either onLoginSuccess or onLoginFailed
        if (serverStatus.equals("success") && username.equals(retrievedUsername) && email.equals(retrievedEmail)) {
            onSignupSuccess();
        } else {
            onSignupFailed();
        }
        progressDialog.dismiss();
        delayHandler.removeCallbacks(delayRunnable);
    }
}