package com.quicktip.quick_tip_servant;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
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
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.quicktip.quick_tip_servant.R;

import org.json.JSONException;
import org.json.JSONObject;

import static android.Manifest.permission.READ_CONTACTS;

public class AccountActivity extends AppCompatActivity {
    private boolean isRequesting = false;
    RequestQueue queue;

    // UI references.
    private EditText mUsernameView;
    private EditText mEmployerView;
    private EditText mNicknameView;
    private EditText mOldPasswordView;
    private EditText mNewPasswordView;
    private TextView errorText;
    private View mProgressView;
    private View mUpdateFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mUsernameView = (EditText) findViewById(R.id.username);
        mEmployerView = (EditText) findViewById(R.id.employer);
        mNicknameView = (EditText) findViewById(R.id.nickname);
        mOldPasswordView = (EditText) findViewById(R.id.old_password);
        mNewPasswordView = (EditText) findViewById(R.id.new_password);
        errorText = (TextView) findViewById(R.id.error_text);
        mProgressView = findViewById(R.id.account_progress);
        mUpdateFormView = findViewById(R.id.update_form);
        Button mUpdateAccountButton = (Button) findViewById(R.id.email_sign_in_button);
        mUpdateAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AccountActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                attemptUpdateAccount();
            }
        });
        queue = Volley.newRequestQueue(this);

        showProgress(true);
        getAccountInfo();
    }

    private void attemptUpdateAccount() {
        if (isRequesting) {
            return;
        }

        mNicknameView.setError(null);
        mOldPasswordView.setError(null);
        mNewPasswordView.setError(null);

        String username = mUsernameView.getText().toString();
        String nickname = mNicknameView.getText().toString();
        String oldPassword = mOldPasswordView.getText().toString();
        String newPassword = mNewPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (!TextUtils.isEmpty(oldPassword)) {
            if (!isPasswordValid(newPassword)) {
                mNewPasswordView.setError("New password cannot be empty");
                focusView = mNewPasswordView;
                cancel = true;
            } else if (newPassword.length() < 6) {
                mNewPasswordView.setError("Passoword should has at least 6 chars.");
                focusView = mNewPasswordView;
                cancel = true;
            }
        }

        if (cancel) {
             focusView.requestFocus();
        } else {
            isRequesting = true;
            showProgress(true);
            sendRequest(username, nickname, oldPassword, newPassword);
        }
    }

    public void sendRequest(String username, final String nickname, String oldPassword, String newPassword) {
        String url = getString(R.string.server)+"/user";
        Map<String, String> auth = new HashMap<String, String>();
        auth.put("nickname", nickname);
        auth.put("username", username);

        JsonObjectRequest updateRequest = new JsonObjectRequest
                (Request.Method.PUT, url, new JSONObject(auth), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 0) {
                                showProgress(false);
                                isRequesting = false;

                                JSONObject data = response.getJSONObject("data");
                                JSONObject userInfo = data.getJSONObject("userInfo");
                                SharedPreferences sp = AccountActivity.this.getSharedPreferences("login_data", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putString("nickname", userInfo.getString("nickname"));
                                editor.putString("token", data.getString("token"));
                                editor.apply();

                                new AlertDialog.Builder(AccountActivity.this)
                                        .setTitle("Alert")
                                        .setMessage("Your changes have been saved.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            } else {
                                showUpdateError(response.getString("msg"));
                            }
                        } catch (JSONException e) {
                            showUpdateError("Update failed");
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showUpdateError("Fail to connect the server");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = AccountActivity.this.getSharedPreferences("login_data", Context.MODE_PRIVATE)
                        .getString("token", "");
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("access-token", token);
                return headers;
            }
        };
        queue.add(updateRequest);
    }

    public void getAccountInfo() {
        String url = getString(R.string.server)+"/user";

        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            JSONObject data = response.getJSONObject("data");
                            SharedPreferences sp = AccountActivity.this.getSharedPreferences("login_data", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            if (code == 0) {
                                JSONObject userInfo = data.getJSONObject("userInfo");
                                editor.putString("username", userInfo.getString("username"));
                                editor.putString("nickname", userInfo.getString("nickname"));
                                editor.putString("employer", userInfo.getString("employerName"));
                                editor.apply();

                                mNicknameView.setText(userInfo.getString("nickname"));
                                mUsernameView.setText(userInfo.getString("username"));
                                mEmployerView.setText(userInfo.getString("employerName"));

                            } else {
                                new AlertDialog.Builder(AccountActivity.this)
                                        .setTitle("Error" )
                                        .setMessage(response.getString("msg"))
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        })
                                        .show();
                            }
                            editor.putString("token", data.getString("token"));
                            editor.apply();
                            showProgress(false);
                        } catch (JSONException e) {
                            showProgress(false);
                            new AlertDialog.Builder(AccountActivity.this)
                                    .setTitle("Error" )
                                    .setMessage("Something went wrong.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                                    .show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        new AlertDialog.Builder(AccountActivity.this)
                                .setTitle("Error" )
                                .setMessage("Fail to connect the server")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = AccountActivity.this.getSharedPreferences("login_data", Context.MODE_PRIVATE)
                        .getString("token", "");
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("access-token", token);
                return headers;
            }
        };
        queue.add(req);
    }


    public void showUpdateError(String msg) {
        isRequesting = false;
        showProgress(false);
        errorText.setText(msg);
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 6;
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

            mUpdateFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mUpdateFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mUpdateFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mUpdateFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
        }
        return super.onOptionsItemSelected(item);
    }
}
