package com.quicktip.quick_tip_servant;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
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

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllTipsActivity extends AppCompatActivity {
    private RequestQueue queue;
    private View progressView;
    ListView allTipsView;
    View datePickerView;
    private TextView tipsMonthView;
    RatingBar averageView;
    TextView scoreView;
    TextView totalView;
    AlertDialog datePickerDialog;
    Calendar today;
    float average;
    float total;
    private ArrayAdapter<HashMap<String, String>> adapter;

    private class ListViewAdapter extends ArrayAdapter<HashMap<String, String>> {

        private AppCompatActivity activity;
        private List<HashMap<String, String>> tipList;

        private ListViewAdapter(AppCompatActivity context, int resource, List<HashMap<String, String>> objects) {
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
            } catch (NullPointerException e) {
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

            private ViewHolder(View view) {
                score = (RatingBar) view.findViewById(R.id.tip_score);
                content = (TextView) view.findViewById(R.id.tip_content);
                user = (TextView) view.findViewById(R.id.tip_user);
                date = (TextView) view.findViewById(R.id.tip_date);
                shop = (TextView) view.findViewById(R.id.tip_shop);
                money = (TextView) view.findViewById(R.id.tip_money);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_tips);

        final Date date = new Date();
        today = new GregorianCalendar();
        today.setTime(date);
        final Calendar start = new GregorianCalendar();
        final Calendar end = new GregorianCalendar();
        start.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), 1);
        end.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, 1);
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        final HashMap<Integer, String> month = new HashMap<Integer, String>(12);
        month.put(0, "Jan"); month.put(1, "Feb"); month.put(2, "Mar"); month.put(3, "Apr");
        month.put(4, "May"); month.put(5, "June"); month.put(6, "July"); month.put(7, "Aug");
        month.put(8, "Spet"); month.put(9, "Oct"); month.put(10, "Nov"); month.put(11, "Dec");

        totalView = (TextView) findViewById(R.id.total);
        scoreView = (TextView) findViewById(R.id.score);
        progressView = findViewById(R.id.progress);
        allTipsView = (ListView) findViewById(R.id.tip_list);
        tipsMonthView = (TextView) findViewById(R.id.tips_month);
        tipsMonthView.setText(month.get(today.get(Calendar.MONTH)) + ", " + today.get(Calendar.YEAR));
        averageView = (RatingBar) findViewById(R.id.average_score);
        datePickerView = View.inflate(this, R.layout.date_picker, null);
        datePickerDialog  = new AlertDialog.Builder(this).create();
        DatePicker datePicker = (DatePicker) datePickerView.findViewById(R.id.date_picker);
        datePicker.init(today.get(Calendar.YEAR),today.get(Calendar.MONTH),today.get(Calendar.DAY_OF_MONTH),null);
        LinearLayout ll = (LinearLayout)datePicker.getChildAt(0);
        LinearLayout ll2 = (LinearLayout)ll.getChildAt(0);
        ll2.getChildAt(1).setVisibility(View.INVISIBLE);

        datePickerView.findViewById(R.id.date_time_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePicker datePickerTmp = (DatePicker) datePickerView.findViewById(R.id.date_picker);
                today = new GregorianCalendar(datePickerTmp.getYear(),
                        datePickerTmp.getMonth(),
                        datePickerTmp.getDayOfMonth());

                tipsMonthView.setText(month.get(today.get(Calendar.MONTH)) + ", " + today.get(Calendar.YEAR));
                start.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), 1);
                end.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, 1);
                requestAllTip(format.format(start.getTime()), format.format(end.getTime()));
                datePickerDialog.dismiss();
            }
        });
        datePickerDialog.setTitle("Choose Month");
        datePickerDialog.setView(datePickerView);

        queue = Volley.newRequestQueue(this);

        showProgress(true);
        requestAllTip(format.format(start.getTime()), format.format(end.getTime()));
        Button monthPickerButton = (Button) findViewById(R.id.date_picker_button);
        monthPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog.show();
            }
        });
    }

    public ArrayList<HashMap<String, String>> getItem(JSONArray rewardList) {
        ArrayList<HashMap<String, String>> item = new ArrayList<HashMap<String, String>>();
        total = 0;
        try {
            for (int i = 0; i < rewardList.length(); i++) {
                JSONObject reward = rewardList.getJSONObject(i);
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("user", reward.getString("setterNickname"));
                map.put("money", reward.getString("money"));
                map.put("score", reward.getString("star"));
                map.put("content", "    " + reward.getString("comment"));
                map.put("shop", reward.getString("shopNickname"));
                map.put("date", reward.getString("dayTime"));
                item.add(map);
                average += Float.parseFloat(reward.getString("star"));
                total += Float.parseFloat(reward.getString("money"));
            }
        } catch (JSONException e) {
            showError("Something went wrong.");
        }
        if (rewardList.length() > 0)
            average /= rewardList.length();
        return item;
    }

    public void requestAllTip(String start, String end) {
        String url = getString(R.string.server) + "/reward?waiter=&start=" + start + "&end=" + end;
        average = 0;

        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int code = response.getInt("code");
                            if (code == 0) {
                                JSONObject data = response.getJSONObject("data");
                                JSONArray rewardList = data.getJSONArray("rewardList");

                                ArrayList<HashMap<String, String>> arrayList = getItem(rewardList);
                                adapter = new AllTipsActivity.ListViewAdapter(AllTipsActivity.this, R.layout.tip_list, arrayList);
                                allTipsView.setAdapter(adapter);
                                averageView.setRating(average);
                                scoreView.setText((""+average).substring(0,3));
                                totalView.setText("$ "+total);
                            } else {
                                showError(response.getString("msg"));
                            }
                        } catch (JSONException e) {
                            showError("Something went wrong");
                        }
                        showProgress(false);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showError("Fail to connect the server");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = AllTipsActivity.this.getSharedPreferences("login_data", Context.MODE_PRIVATE)
                        .getString("token", "");
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("access-token", token);
                return headers;
            }
        };
        queue.add(req);
    }

    public void showError(String msg) {
        showProgress(false);
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            allTipsView.setVisibility(show ? View.GONE : View.VISIBLE);
            allTipsView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    allTipsView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            allTipsView.setVisibility(show ? View.GONE : View.VISIBLE);
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