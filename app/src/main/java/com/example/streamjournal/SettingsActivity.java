package com.example.streamjournal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final Context context = this;
        final LinearLayout settings = findViewById(R.id.settings_layout);

        // Display available effects fetched from the Rest Api
        final AsyncHttpClient client = new AsyncHttpClient();
        client.get("https://tungsten.alexlogan.co.uk/effect/b5583fa0-60ac-4ce1-8ba5-352d80757933/", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                try {
                    final JSONArray templates = new JSONObject(response.getString("effects")).getJSONArray("templates");
                    final JSONArray effects = new JSONObject(response.getString("effects")).getJSONArray("effects");

                    for (int i=0; i < effects.length(); i++) {
                        final JSONObject effect = effects.getJSONObject(i);

                        // Create option container
                        RelativeLayout option = new RelativeLayout(context);
                        RelativeLayout.LayoutParams optionParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        optionParams.setMargins(0, Math.round(10 * getResources().getDisplayMetrics().density), 0 ,0);
                        option.setLayoutParams(optionParams);

                        RelativeLayout.LayoutParams sectionHeaderParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);

                        if (i == 0) {
                            TextView header = new TextView(context);
                            sectionHeaderParams.setMargins(0, Math.round(50 * getResources().getDisplayMetrics().density), 0 ,0);
                            header.setLayoutParams(sectionHeaderParams);
                            header.setText(getString(R.string.basic));
                            header.setTextSize(24);
                            header.setTypeface(ResourcesCompat.getFont(context, R.font.raleway_medium));
                            settings.addView(header);
                        } else if (i == 2) {
                            TextView header = new TextView(context);
                            sectionHeaderParams.setMargins(0, Math.round(30 * getResources().getDisplayMetrics().density), 0 ,0);
                            header.setLayoutParams(sectionHeaderParams);
                            header.setText(getString(R.string.advanced));
                            header.setTextSize(24);
                            header.setTypeface(ResourcesCompat.getFont(context, R.font.raleway_medium));
                            settings.addView(header);
                        }

                        // Create option description
                        TextView optionName = new TextView(context);
                        RelativeLayout.LayoutParams optionNameParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        optionNameParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                        optionNameParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                        optionName.setLayoutParams(optionNameParams);
                        optionName.setText(effect.getString("name"));
                        optionName.setTextSize(16);
                        optionName.setTextColor(getResources().getColor(R.color.black));
                        optionName.setTypeface(ResourcesCompat.getFont(context, R.font.raleway_medium));

                        // Create option toggle switch
                        Switch optionStatus = new Switch(context);
                        RelativeLayout.LayoutParams optionStatusParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        optionStatusParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                        optionStatus.setChecked(effect.getBoolean("isActive"));
                        optionStatus.setLayoutParams(optionStatusParams);
                        optionStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                try {
                                    effect.put("isActive", isChecked);

                                    JSONObject updatedEffects = new JSONObject();
                                    updatedEffects.put("templates", templates);
                                    updatedEffects.put("effects", effects);
                                    RequestParams params = new RequestParams();
                                    params.put("effects", updatedEffects.toString());
                                    client.put("https://tungsten.alexlogan.co.uk/effect/b5583fa0-60ac-4ce1-8ba5-352d80757933/", params, new JsonHttpResponseHandler());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                        });
                        option.addView(optionName);
                        option.addView(optionStatus);
                        settings.addView(option);
                    }

                    client.get("https://tungsten.alexlogan.co.uk/overlays/", new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                            // Add section header
                            RelativeLayout.LayoutParams templateHeaderParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT);

                            TextView header = new TextView(context);
                            templateHeaderParams.setMargins(0, Math.round(30 * getResources().getDisplayMetrics().density), 0 ,0);
                            header.setLayoutParams(templateHeaderParams);
                            header.setText(getString(R.string.templates));
                            header.setTextSize(24);
                            header.setTypeface(ResourcesCompat.getFont(context, R.font.raleway_medium));
                            settings.addView(header);

                            // Add options to enable templates
                            for (int i=0; i < response.length(); i++) {
                                try {
                                    final JSONObject templateInfo = response.getJSONObject(i);

                                    // Create template container
                                    RelativeLayout template = new RelativeLayout(context);
                                    RelativeLayout.LayoutParams optionParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                                    optionParams.setMargins(0, Math.round(10 * getResources().getDisplayMetrics().density), 0 ,0);
                                    template.setLayoutParams(optionParams);

                                    // Create template checkbox
                                    CheckBox templateStatus = new CheckBox(context);
                                    RelativeLayout.LayoutParams templateStatusParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                                    templateStatusParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                                    String[] url = templateInfo.getString("url").split("/");
                                    String id = url[url.length- 1];
                                    if (templates.toString().contains(id)) {
                                        templateStatus.setChecked(true);
                                    }
                                    templateStatus.setLayoutParams(templateStatusParams);
                                    templateStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                                        @Override
                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                            try {
                                                String[] url = templateInfo.getString("url").split("/");
                                                String id = url[url.length- 1];

                                                if (isChecked) {
                                                    templates.put(id);
                                                } else {
                                                    for (int i=0; i < templates.length(); i++) {
                                                        if (templates.get(i).equals(id)) {
                                                            templates.remove(i);
                                                        }
                                                    }
                                                }

                                                JSONObject updatedEffects = new JSONObject();
                                                updatedEffects.put("templates", templates);
                                                updatedEffects.put("effects", effects);
                                                RequestParams params = new RequestParams();
                                                params.put("effects", updatedEffects.toString());
                                                client.put("https://tungsten.alexlogan.co.uk/effect/b5583fa0-60ac-4ce1-8ba5-352d80757933/", params, new JsonHttpResponseHandler());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                    });

                                    // Create template description
                                    TextView templateName = new TextView(context);
                                    RelativeLayout.LayoutParams templateNameParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                                    templateNameParams.setMargins(Math.round(50 * getResources().getDisplayMetrics().density), Math.round(10 * getResources().getDisplayMetrics().density), 0 ,0);
                                    templateNameParams.addRule(RelativeLayout.CENTER_VERTICAL);
                                    templateName.setLayoutParams(templateNameParams);
                                    templateName.setText(templateInfo.getString("name"));
                                    templateName.setTextSize(16);
                                    templateName.setTextColor(getResources().getColor(R.color.black));
                                    templateName.setTypeface(ResourcesCompat.getFont(context, R.font.raleway_medium));

                                    template.addView(templateStatus);
                                    template.addView(templateName);
                                    settings.addView(template);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


}
