package com.quicktip.quick_tip_servant;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

import java.util.HashMap;
import java.util.Map;

public class WalletActivity extends AppCompatActivity {
    private boolean isRequesting = false;
    RequestQueue queue;

    private View mWalletView;
    private View mProgressView;
    private View mProgressLiteView;
    private TextView moneyText;
    private EditText withdrawView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        mWalletView = (View) findViewById(R.id.wallet_view);
        mProgressView = (View) findViewById(R.id.progress);
        mProgressLiteView = (View) findViewById(R.id.progress_lite);
        moneyText = (TextView) findViewById(R.id.wallet_money);
        withdrawView = (EditText) findViewById(R.id.withdraw_money);

        queue = Volley.newRequestQueue(this);

        showProgress(true);
        requestMoney();

        Button withdrawButton = (Button) findViewById(R.id.withdraw);
        withdrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    float data =  Float.parseFloat(withdrawView.getText().toString());
                    isRequesting = true;
                    showProgressLite(true);
                    sendWithdrawRequest(data);
                } catch (NumberFormatException e) {
                    new AlertDialog.Builder(WalletActivity.this)
                            .setTitle("Error" )
                            .setMessage("Please input valid amount！")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });
    }

    public void sendWithdrawRequest(float ammount) {
        String url = getString(R.string.server)+"/account";
        Map<String, Float> param = new HashMap<String, Float>();
        param.put("money", -ammount);

        JsonObjectRequest withdrawRequest = new JsonObjectRequest
                (Request.Method.PUT, url, new JSONObject(param), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            JSONObject data = response.getJSONObject("data");
                            if (code == 0) {
                                JSONObject userInfo = data.getJSONObject("userInfo");
                                withdrawView.setText(null);
                                new AlertDialog.Builder(WalletActivity.this)
                                        .setTitle("Alert" )
                                        .setMessage("Withdraw successfully！")
                                        .setPositiveButton("OK", null)
                                        .show();
                                String balance = userInfo.getString("balance");
                                moneyText.setText(balance);

                                showProgressLite(false);
                                isRequesting = false;
                            } else {
                                showError(response.getString("msg"));
                            }
                            SharedPreferences sp = WalletActivity.this.getSharedPreferences("login_data", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("token", data.getString("token"));
                            editor.apply();
                        } catch (JSONException e) {
                            showError("Something went wrong...");
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showError("Fail to connect server.");
                    }
                }) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        String token = WalletActivity.this.getSharedPreferences("login_data", Context.MODE_PRIVATE)
                                .getString("token", "");
                        HashMap<String, String> headers = new HashMap<String, String>();
                        headers.put("access-token", token);
                        return headers;
                    }
                };
        queue.add(withdrawRequest);
    }

    public void requestMoney() {
        String url = getString(R.string.server)+"/user";

        JsonObjectRequest moneyRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            JSONObject data = response.getJSONObject("data");
                            if (code == 0) {
                                showProgress(false);
                                JSONObject userInfo = data.getJSONObject("userInfo");
                                String moneyData = userInfo.getString("balance");
                                moneyText.setText(moneyData);
                            } else {
                                showError(response.getString("msg"));
                            }
                            SharedPreferences sp = WalletActivity.this.getSharedPreferences("login_data", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("token", data.getString("token"));
                            editor.apply();
                        } catch (JSONException e) {
                            showError("Something went wrong...");
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showError("Fail to connect server.");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = WalletActivity.this.getSharedPreferences("login_data", Context.MODE_PRIVATE)
                        .getString("token", "");
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("access-token", token);
                return headers;
            }
        };
        queue.add(moneyRequest);
    }

    public void showError(String msg) {
        isRequesting = false;
        showProgressLite(false);
        new AlertDialog.Builder(WalletActivity.this)
                .setTitle("Error")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mWalletView.setVisibility(show ? View.GONE : View.VISIBLE);
            mWalletView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mWalletView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mWalletView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showProgressLite(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressLiteView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressLiteView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressLiteView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressLiteView.setVisibility(show ? View.VISIBLE : View.GONE);
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
