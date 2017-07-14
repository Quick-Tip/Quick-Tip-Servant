package com.quicktip.quick_tip_servant;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    RequestQueue queue;
    private ListView recentTipsView;
    private View progressView;
    private View footerView;
    private View content;
    private SwipeRefreshLayout swipeContainer;
    boolean first;
    private ArrayAdapter<HashMap<String, String>> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        first = true;
        View mainBar = findViewById(R.id.main_bar);
        content = mainBar.findViewById(R.id.content);
        progressView = content.findViewById(R.id.progress);
        footerView = ((LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listview_button, null, false);
        recentTipsView = (ListView) content.findViewById(R.id.recent_tip_list);

        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView nicknameText = (TextView) headerView.findViewById(R.id.nickname);
        TextView usernameText = (TextView) headerView.findViewById(R.id.username);
        String nickname = this.getSharedPreferences("login_data", Context.MODE_PRIVATE).getString("nickname", "");
        String username = this.getSharedPreferences("login_data", Context.MODE_PRIVATE).getString("username", "");
        nicknameText.setText(nickname);
        usernameText.setText(username);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        Button bindGoTipButton = (Button) findViewById(R.id.bind_gotip_button);
        bindGoTipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, BindActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
            }
        });
        Button unbindGoTipButton = (Button) findViewById(R.id.unbind_gotip_button);
        unbindGoTipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.setClass(MainActivity.this, UnbindActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
            }
        });

        queue = Volley.newRequestQueue(this);
        showProgress(true);

        Date date = new Date();
        final Calendar start = new GregorianCalendar();
        final Calendar end = new GregorianCalendar();
        start.setTime(date);
        end.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DATE)+1);
        start.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH)-1, start.get(Calendar.DATE));
        requestRencentTip(format.format(start.getTime()), format.format(end.getTime()));

        swipeContainer = (SwipeRefreshLayout) content.findViewById(R.id.refreshable_view);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestRencentTip(format.format(start.getTime()), format.format(end.getTime()));
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences sp = getSharedPreferences("login_data", MODE_PRIVATE);
        if (!sp.getString("service", "").equals("started") && settings.getBoolean("notiSetting", true))
            startService(new Intent(getBaseContext(), WebsocketService.class));
        if (!settings.getBoolean("notiSetting", true))
            stopService(new Intent(getBaseContext(), WebsocketService.class));

        getSupportActionBar().setElevation(0);
    }

    public void requestRencentTip(String start, String end) {
        String url = getString(R.string.server) + "/reward?waiter=&start=" + start + "&end=" + end;

        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 0) {
                                JSONObject data = response.getJSONObject("data");
                                JSONArray rewardList = data.getJSONArray("rewardList");
                                if (first) {
                                    ArrayList<HashMap<String, String>> arrayList = getItem(rewardList);
                                    adapter = new ListViewAdapter(MainActivity.this, R.layout.tip_list, arrayList);
                                    recentTipsView.setAdapter(adapter);
                                    recentTipsView.addFooterView(footerView);
                                    first = false;
                                } else {
                                    ArrayList<HashMap<String, String>> arrayList = getItem(rewardList);
                                    adapter = new ListViewAdapter(MainActivity.this, R.layout.tip_list, arrayList);
                                    recentTipsView.setAdapter(adapter);
                                    adapter.notifyDataSetChanged();
                                }
                                Button jumpToAllTipsButton = (Button) recentTipsView.findViewById(R.id.jump_to_all_tips_button);
                                jumpToAllTipsButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent i = new Intent(MainActivity.this, AllTipsActivity.class);
                                        startActivity(i);
                                        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                                    }
                                });
                            } else {
                                showError(response.getString("msg"));
                            }
                        } catch (JSONException e) {
                            showError("Something went wrong...");
                        }
                        showProgress(false);
                        swipeContainer.setRefreshing(false);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showError("Fail to connect server.");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = MainActivity.this.getSharedPreferences("login_data", Context.MODE_PRIVATE)
                        .getString("token", "");
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("access-token", token);
                return headers;
            }
        };
        queue.add(req);
    }

    public class ListViewAdapter extends ArrayAdapter<HashMap<String, String>> {

        private AppCompatActivity activity;
        private List<HashMap<String, String>> tipList;

        public ListViewAdapter(AppCompatActivity context, int resource, List<HashMap<String, String>> objects) {
            super(context, resource, objects);
            this.activity = context;
            this.tipList = objects;
        }

        @Override
        public HashMap<String, String> getItem(int position) {
            return tipList.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(MainActivity.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.tip_list, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.score.setOnRatingBarChangeListener(onRatingChangedListener(holder, position));

            holder.score.setTag(position);
            try {
                try {
                    holder.score.setRating(Float.parseFloat(getItem(position).get("score")));
                } catch (NumberFormatException e) {
                    showError("Something went wrong...");
                }
                holder.user.setText(getItem(position).get("user"));
                holder.content.setText(getItem(position).get("content"));
                holder.date.setText(getItem(position).get("date"));
                holder.shop.setText(getItem(position).get("shop"));
                holder.money.setText(getItem(position).get("money"));
            } catch(NullPointerException e) {
                showError("Something went wrong...");
            }
            return convertView;
        }


        private RatingBar.OnRatingBarChangeListener onRatingChangedListener(final ViewHolder holder, final int position) {
            return null;
        }

        private class ViewHolder {
            private RatingBar score;
            private TextView content;
            private TextView user;
            private TextView date;
            private TextView shop;
            private TextView money;

            public ViewHolder(View view) {
                score = (RatingBar) view.findViewById(R.id.tip_score);
                content = (TextView) view.findViewById(R.id.tip_content);
                user = (TextView) view.findViewById(R.id.tip_user);
                date = (TextView) view.findViewById(R.id.tip_date);
                shop = (TextView) view.findViewById(R.id.tip_shop);
                money = (TextView) view.findViewById(R.id.tip_money);
            }
        }
    }

    public ArrayList<HashMap<String, String>> getItem(JSONArray rewardList) {
        ArrayList<HashMap<String, String>> item = new ArrayList<HashMap<String, String>>();
        int len = rewardList.length() > 10 ? 10 : rewardList.length();
        try {
            for (int i = 0; i < len; i++) {
                JSONObject reward = rewardList.getJSONObject(i);
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("user", reward.getString("setterNickname"));
                map.put("money", reward.getString("money"));
                map.put("score", reward.getString("star"));
                map.put("content", "    " + reward.getString("comment"));
                map.put("shop", reward.getString("shopNickname"));
                map.put("date", reward.getString("dayTime"));
                item.add(map);
            }
        } catch (JSONException e) {
            showError("Something went wrong...");
        }
        return item;
    }

    public void showError(String msg) {
        showProgress(false);
        swipeContainer.setRefreshing(false);
        new AlertDialog.Builder(this)
                .setTitle("Error" )
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_account) {
            Intent i = new Intent(MainActivity.this, AccountActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_wallet) {
            Intent i = new Intent(MainActivity.this, WalletActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_tips) {
            Intent i = new Intent(MainActivity.this, AllTipsActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_setting) {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_logout) {
            SharedPreferences sp = MainActivity.this.getSharedPreferences("login_data", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            startService(new Intent(getBaseContext(), WebsocketService.class));
            editor.putString("login", "false");
            editor.putString("username", "");
            editor.putString("nickname", "");
            editor.apply();

            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        } else if (id == R.id.nav_problem) {

        } else if (id == R.id.nav_about) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

            recentTipsView.setVisibility(show ? View.GONE : View.VISIBLE);
            recentTipsView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    recentTipsView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            recentTipsView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
