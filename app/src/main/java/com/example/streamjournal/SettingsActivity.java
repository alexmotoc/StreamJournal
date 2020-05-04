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

        getEffects(client, context, settings);
    }

    public void getEffects(final AsyncHttpClient client, final Context context, final LinearLayout settings) {
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
                        optionParams.setMargins(0, pxToDp(10), 0 ,0);
                        option.setLayoutParams(optionParams);

                        RelativeLayout.LayoutParams sectionHeaderParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);

                        if (i == 0) {
                            TextView header = createTextView(context, new int[] {}, new int[] {0, 50, 0, 0}, getString(R.string.basic), 24, -1);
                            settings.addView(header);
                        } else if (i == 2) {
                            TextView header = createTextView(context, new int[] {}, new int[] {0, 30, 0, 0}, getString(R.string.basic), 24, -1);
                            settings.addView(header);
                        }

                        // Create option description
                        TextView optionName = createTextView(context, new int[] {RelativeLayout.ALIGN_PARENT_START, RelativeLayout.CENTER_IN_PARENT},
                                new int[] {0, 0, 0, 0}, effect.getString("name"), 16, getResources().getColor(R.color.black));

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

                                    updateEffects(client, templates, effects);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                        });

                        option.addView(optionName);
                        option.addView(optionStatus);
                        settings.addView(option);
                    }

                    getTemplates(client, context, settings, templates, effects);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getTemplates(final AsyncHttpClient client, final Context context, final LinearLayout settings,
                             final JSONArray templates, final JSONArray effects) {
        client.get("https://tungsten.alexlogan.co.uk/overlays/", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                // Add section header
                TextView header = createTextView(context, new int[] {}, new int[] {0, 30, 0, 0}, getString(R.string.templates), 24, -1);
                settings.addView(header);

                // Add options to enable templates
                for (int i=0; i < response.length(); i++) {
                    try {
                        final JSONObject templateInfo = response.getJSONObject(i);

                        // Create template container
                        RelativeLayout template = new RelativeLayout(context);
                        RelativeLayout.LayoutParams optionParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        optionParams.setMargins(0, pxToDp(10), 0 ,0);
                        template.setLayoutParams(optionParams);

                        // Create template checkbox
                        CheckBox templateStatus = new CheckBox(context);
                        RelativeLayout.LayoutParams templateStatusParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        templateStatusParams.addRule(RelativeLayout.ALIGN_PARENT_START);

                        // Parse url to obtain template id
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
                                        // Remove template from active list if it is unchecked
                                        for (int i=0; i < templates.length(); i++) {
                                            if (templates.get(i).equals(id)) {
                                                templates.remove(i);
                                            }
                                        }
                                    }

                                    updateEffects(client, templates, effects);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                        });

                        // Create template description
                        TextView templateName = createTextView(context, new int[] {RelativeLayout.CENTER_VERTICAL}, new int[] {50, 10, 0, 0},
                                templateInfo.getString("name"), 16, getResources().getColor(R.color.black));

                        template.addView(templateStatus);
                        template.addView(templateName);
                        settings.addView(template);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     *
     * @param px Number of pixels
     * @return Device pixels (dp) corresponding to a pixel value
     */
    public int pxToDp(int px) {
        return Math.round(px * getResources().getDisplayMetrics().density);
    }

    /**
     *
     * @param context Activity context
     * @param rules Array of rules (integers) to be applied to the layout parameters
     * @param margins Array of margins (size 4) ordered as follows: left, top, right, bottom
     * @param text The text to be rendered in the view
     * @param size The size of the text
     * @param color The color of the text
     * @return A new TextView element with the specified properties
     */
    public TextView createTextView(Context context, int[] rules, int[] margins, String text, int size, int color) {
        TextView textView = new TextView(context);

        RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        textViewParams.setMargins(pxToDp(margins[0]), pxToDp(margins[1]), pxToDp(margins[2]) ,pxToDp(margins[3]));

        for (int rule: rules) {
            textViewParams.addRule(rule);
        }

        textView.setLayoutParams(textViewParams);
        textView.setText(text);
        textView.setTextSize(size);

        if (color != -1) {
            textView.setTextColor(color);
        }

        textView.setTypeface(ResourcesCompat.getFont(context, R.font.raleway_medium));

        return textView;
    }

    /**
     *
     * @param client Client used to perform PUT request
     * @param templates Array of template ids
     * @param effects Array of effect objects containing the effect's name and status
     * @throws JSONException Throws exception if JSON put operation fails
     */
    public void updateEffects(AsyncHttpClient client, JSONArray templates, JSONArray effects) throws JSONException {
        JSONObject updatedEffects = new JSONObject();
        updatedEffects.put("templates", templates);
        updatedEffects.put("effects", effects);
        RequestParams params = new RequestParams();
        params.put("effects", updatedEffects.toString());
        client.put("https://tungsten.alexlogan.co.uk/effect/b5583fa0-60ac-4ce1-8ba5-352d80757933/", params, new JsonHttpResponseHandler());
    }
}
