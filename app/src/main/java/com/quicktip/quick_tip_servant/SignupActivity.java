package com.quicktip.quick_tip_servant;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.READ_CONTACTS;

public class SignupActivity extends AppCompatActivity {
    private boolean isRequesting = false;
    RequestQueue queue;

    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mPasswordAgainView;
    private View mProgressView;
    private View mSignupFormView;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordAgainView = (EditText) findViewById(R.id.passwordAgain);
        errorText = (TextView) findViewById(R.id.error_text);
        mSignupFormView = findViewById(R.id.signup_form);
        mProgressView = findViewById(R.id.signup_progress);
        Button mEmailSignInButton = (Button) findViewById(R.id.sign_up_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignup();
            }
        });
        Button jumpToSignInButton = (Button) findViewById(R.id.jump_to_sign_in_button);
        jumpToSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });
        queue = Volley.newRequestQueue(this);
    }

    private void attemptSignup() {
        if (isRequesting) {
            return;
        }

        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mPasswordAgainView.setError(null);
        mUsernameView.clearFocus();
        mPasswordView.clearFocus();
        mPasswordAgainView.clearFocus();

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(SignupActivity.this.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String passwordAgain = mPasswordAgainView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(username)) {
            mPasswordAgainView.setError(getString(R.string.error_field_required));
            focusView = mPasswordAgainView;
            cancel = true;
        } else if (TextUtils.isEmpty(password)) {
            mPasswordAgainView.setError(getString(R.string.error_field_required));
            focusView = mPasswordAgainView;
            cancel = true;
        } else if (TextUtils.isEmpty(passwordAgain)) {
            mPasswordAgainView.setError(getString(R.string.error_field_required));
            focusView = mPasswordAgainView;
            cancel = true;
        }

        if (!password.equals(passwordAgain)) {
            mPasswordAgainView.setError("Password does not match!");
            focusView = mPasswordAgainView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            isRequesting = true;
            sendRequest(username, password, passwordAgain);
        }
    }

    public void sendRequest(final String username, final String password, final String passwordAgain) {
        String url = getString(R.string.server)+"/user/register";
        Map<String, String> auth = new HashMap<String, String>();
        auth.put("username", username);
        auth.put("password", password);
        auth.put("verify", passwordAgain);
        auth.put("user_type", "1");

        JsonObjectRequest signupRequest = new JsonObjectRequest
                (Request.Method.POST, url, new JSONObject(auth), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 0) {
                                JSONObject data = response.getJSONObject("data");
                                JSONObject userInfo = data.getJSONObject("userInfo");
                                SharedPreferences sp = SignupActivity.this.getSharedPreferences("login_data", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putString("login", "true");
                                editor.putString("username", userInfo.getString("username"));
                                editor.putString("nickname", userInfo.getString("nickname"));
                                editor.putString("token", data.getString("token"));
                                editor.apply();

                                Intent i = new Intent(SignupActivity.this, MainActivity.class);
                                startActivity(i);
                                finish();
                            } else {
                                showSignupError(response.getString("msg"));
                            }
                        } catch (JSONException e) {
                            showSignupError("Signup failed due to unknown reason...");
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showSignupError("Fail to connect server.");
                    }
                });
        queue.add(signupRequest);
    }

    public void showSignupError(String msg) {
        showProgress(false);
        isRequesting = false;
        errorText.setText(msg);
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() >= 6;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mSignupFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mSignupFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSignupFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mSignupFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}

