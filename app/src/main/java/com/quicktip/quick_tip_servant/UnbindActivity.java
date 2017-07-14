package com.quicktip.quick_tip_servant;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.quicktip.quick_tip_servant.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UnbindActivity extends AppCompatActivity {
    RequestQueue queue;
    private View mProgressView;
    ListView deviceListView;
    private SwipeRefreshLayout swipeContainer;

    public class MyAdapter extends BaseAdapter implements ListAdapter {
        private ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        private Context context;

        public MyAdapter(ArrayList<HashMap<String, String>> list, Context context) {
            this.list = list;
            this.context = context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int pos) {
            return list.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.device_list, null);
            }

            //Handle TextView and display string from your list
            TextView nameView = (TextView)view.findViewById(R.id.device_name);
            TextView descriView = (TextView)view.findViewById(R.id.device_description);
            nameView.setText(list.get(position).get("name"));
            descriView.setText(list.get(position).get("description"));

            //Handle buttons and add onClickListeners
            Button addBtn = (Button)view.findViewById(R.id.unbind_device);
            addBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    showProgress(true);
                    unBindRequest(list.get(position).get("shop_id"), list.get(position).get("desktop_id"));
                }
            });
            return view;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unbind);

        mProgressView = findViewById(R.id.progress);
        deviceListView = (ListView) findViewById(R.id.device_list);
        queue = Volley.newRequestQueue(this);

        showProgress(true);
        requestBindInfo();

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.refreshable_view);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestBindInfo();
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    public void requestBindInfo() {
        String url = getString(R.string.server)+"/nfc";

        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            int code = response.getInt("code");
                            JSONObject data = response.getJSONObject("data");
                            SharedPreferences sp = UnbindActivity.this.getSharedPreferences("login_data", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            if (code == 0) {
                                JSONObject userInfo = data.getJSONObject("userInfo");
                                editor.putString("username", userInfo.getString("username"));
                                editor.putString("nickname", userInfo.getString("nickname"));
                                editor.putString("employer", userInfo.getString("employerName"));
                                editor.apply();
                                JSONArray deviceList = data.getJSONArray("nfc");
                                MyAdapter myAdapter = new MyAdapter(UnbindActivity.this.getItem(deviceList), UnbindActivity.this);
                                deviceListView.setAdapter(myAdapter);
                                myAdapter.notifyDataSetChanged();
                                swipeContainer.setRefreshing(false);
                            } else {
                                new AlertDialog.Builder(UnbindActivity.this)
                                        .setTitle("Error" )
                                        .setMessage(response.getString("msg"))
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                            editor.putString("token", data.getString("token"));
                            editor.apply();
                            showProgress(false);
                        } catch (JSONException e) {
                            showProgress(false);
                            new AlertDialog.Builder(UnbindActivity.this)
                                    .setTitle("Error" )
                                    .setMessage("Something went wrong...")
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
                        showProgress(false);
                        new AlertDialog.Builder(UnbindActivity.this)
                                .setTitle("Error" )
                                .setMessage("Fail to connect server.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = UnbindActivity.this.getSharedPreferences("login_data", Context.MODE_PRIVATE)
                        .getString("token", "");
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("access-token", token);
                return headers;
            }
        };
        queue.add(req);
    }

    public void unBindRequest(String shop_id, String desktop_id) {
        String url = getString(R.string.server)+"/nfc";
        Map<String, String> params = new HashMap<String, String>();
        params.put("shop_id", shop_id);
        params.put("desktop_id", desktop_id);
        params.put("bind", "0");

        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.PUT, url, new JSONObject(params), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            JSONObject data = response.getJSONObject("data");
                            SharedPreferences sp = UnbindActivity.this.getSharedPreferences("login_data", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            if (code == 0) {
                                JSONObject userInfo = data.getJSONObject("userInfo");
                                editor.putString("username", userInfo.getString("username"));
                                editor.putString("nickname", userInfo.getString("nickname"));
                                editor.putString("employer", userInfo.getString("employerName"));
                                editor.apply();
                                new AlertDialog.Builder(UnbindActivity.this)
                                        .setTitle("Message" )
                                        .setMessage("Successfully unbind the device.")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                showProgress(true);
                                                requestBindInfo();
                                            }
                                        })
                                        .show();
                            } else {
                                new AlertDialog.Builder(UnbindActivity.this)
                                        .setTitle("Error" )
                                        .setMessage(response.getString("msg"))
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                            editor.putString("token", data.getString("token"));
                            editor.apply();
                            showProgress(false);
                        } catch (JSONException e) {
                            showProgress(false);
                            new AlertDialog.Builder(UnbindActivity.this)
                                    .setTitle("Error" )
                                    .setMessage("Something went wrong...")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showProgress(false);
                        new AlertDialog.Builder(UnbindActivity.this)
                                .setTitle("Error" )
                                .setMessage("Fail to connect server.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = UnbindActivity.this.getSharedPreferences("login_data", Context.MODE_PRIVATE)
                        .getString("token", "");
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("access-token", token);
                return headers;
            }
        };
        queue.add(req);
    }

    public ArrayList<HashMap<String, String>> getItem(JSONArray deviceList) {
        ArrayList<HashMap<String, String>> item = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i < deviceList.length(); i++) {

            try {
                JSONObject device = deviceList.getJSONObject(i);
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("shop_id", device.getString("shop_id"));
                map.put("desktop_id", device.getString("desktop_id"));
                map.put("name", device.getString("desktop_id"));
                map.put("description",  device.getString("data"));
                item.add(map);
            } catch (JSONException e) {
                new AlertDialog.Builder(UnbindActivity.this)
                        .setTitle("Error" )
                        .setMessage("Something went wrong.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }
        return item;
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            deviceListView.setVisibility(show ? View.GONE : View.VISIBLE);
            deviceListView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    deviceListView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            deviceListView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
