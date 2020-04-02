package com.example.streamjournal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ProfileActivity";
    private static final String TWITCH_CLIENT_ID = "0qmxdyuchkdcpkfktmq2t47z06eng1";
    private static final int RC_SIGN_IN = 1;

    private GoogleSignInClient mGoogleSignInClient;
    private SharedPreferences tokens;

    private Button mTwitch;
    private Button mYouTube;

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if (account == null) {
            // User has not signed in
        } else {
            // User has already signed in
            updateGoogleSignInUI(account);
        }

        tokens = this.getPreferences(Context.MODE_PRIVATE);
        String token = tokens.getString("twitch", "");

        final AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Authorization", "OAuth " + token);
        client.get("https://id.twitch.tv/oauth2/validate", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // If the response is JSONObject instead of expected JSONArray
                if (response.has("login")) {
                    updateTwitchSignInUI();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mTwitch = findViewById(R.id.twitch_login);
        mTwitch.setOnClickListener(this);

        mYouTube = findViewById(R.id.youtube_login);
        mYouTube.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.twitch_login:
                twitchSignIn();
                break;
            case R.id.twitch_logout:
                twitchSignOut();
                break;
            case R.id.youtube_login:
                googleSignIn();
                break;
            case R.id.youtube_logout:
                googleSignOut();
                break;
        }
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void twitchSignIn() {
        String redirectUrl = "http://localhost";
        String responseType = "token";
        String scope = "user:edit+channel_read";
        String state = "c3ab8aa609ea11e793ae92361f002671";

        String authenticationUrl = String.format("https://id.twitch.tv/oauth2/authorize" +
                "?client_id=%s" +
                "&redirect_uri=%s" +
                "&response_type=%s" +
                "&scope=%s" +
                "&state=%s", TWITCH_CLIENT_ID, redirectUrl, responseType, scope, state);

        final WebView web = findViewById(R.id.twitch_web);
        if (web.getVisibility() == View.GONE) {
            web.setVisibility(View.VISIBLE);
        } else {
            web.setVisibility(View.GONE);
        }
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!url.startsWith("http://localhost"))  {
                    view.loadUrl(url);
                } else {
                    // Replace url structure so that parsing can work
                    Uri uri = Uri.parse(url.replace("localhost/#", "twitch.tv?"));
                    final String accessToken = uri.getQueryParameter("access_token");
                    SharedPreferences.Editor editor = tokens.edit();
                    editor.putString("twitch", accessToken);
                    editor.apply();
                    web.setVisibility(View.GONE);
                    updateTwitchSignInUI();

                    // Send token to get saved to the REST API
                    final AsyncHttpClient client = new AsyncHttpClient();
                    client.get("https://tungsten.alexlogan.co.uk/user/b0960c68-af68-4e5b-8447-1150878998c1/", new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            try {
                                final JSONObject tokens = new JSONObject(response.getString("tokens"));
                                final JSONObject twitch = tokens.getJSONObject("twitch");

                                client.addHeader("Authorization", "OAuth " + accessToken);
                                client.addHeader("Client-ID", TWITCH_CLIENT_ID);
                                client.addHeader("Accept", "application/vnd.twitchtv.v5+json");
                                client.get("https://api.twitch.tv/kraken/channel", new JsonHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                        try {
                                            String streamKey = response.getString("stream_key");
                                            twitch.put("streamKey", streamKey);
                                            twitch.put("authentication", accessToken);

                                            RequestParams params = new RequestParams();
                                            params.put("tokens", tokens.toString());
                                            client.removeAllHeaders();
                                            client.put("https://tungsten.alexlogan.co.uk/user/b0960c68-af68-4e5b-8447-1150878998c1/", params, new JsonHttpResponseHandler());
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                return true;
            }
        });
        String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.109 Safari/537.36";
        web.getSettings().setUserAgentString(USER_AGENT);
        web.getSettings().setJavaScriptEnabled(true);
        CookieManager.getInstance().removeAllCookies(null);
        web.loadUrl(authenticationUrl);
    }

    private void twitchSignOut() {
        // Send post request to API to revoke OAuth token
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("client_id", TWITCH_CLIENT_ID);
        params.put("token", tokens.getString("twitch", ""));

        client.post("https://id.twitch.tv/oauth2/revoke", params, new JsonHttpResponseHandler());

        updateSignOutUI(mTwitch);
    }

    private void updateSignOutUI(Button platform) {
        platform.setVisibility(View.VISIBLE);
        LinearLayout mConnectedAccount = findViewById(R.id.connected_account);
        mConnectedAccount.setVisibility(View.GONE);
    }

    private void updateTwitchSignInUI() {
        // Hide button used for logging in
        mTwitch.setVisibility(View.GONE);

        LinearLayout mConnectAccounts = findViewById(R.id.connect_accounts);

        LinearLayout mConnectedAccount = (LinearLayout)getLayoutInflater().inflate(
                R.layout.logged_in_layout, mConnectAccounts, false);

        TextView mPlatformName = mConnectedAccount.findViewById(R.id.platform_name);
        mPlatformName.setText(R.string.twitch);
        mPlatformName.setTextColor(getResources().getColor(R.color.twitch));

        Button mLogout = mConnectedAccount.findViewById(R.id.logout);
        mLogout.setId(R.id.twitch_logout);
        mLogout.setBackground(getResources().getDrawable(R.drawable.twitch_button));
        mLogout.setOnClickListener(this);

        final TextView mAccountInfo = mConnectedAccount.findViewById(R.id.account_info);

        // Send POST request to find out user information
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Authorization", "Bearer " + tokens.getString("twitch", ""));
        client.get("https://api.twitch.tv/helix/users", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONObject data = response.getJSONArray("data").getJSONObject(0);
                    String display_name = data.getString("display_name");
                    String login = data.getString("login");
                    mAccountInfo.setText(getString(R.string.signed_in,
                            display_name, login));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        if (mConnectAccounts.findViewById(R.id.connected_account) == null) {
            mConnectAccounts.addView(mConnectedAccount, mConnectAccounts.getChildCount() - 2);
        } else {
            mConnectAccounts.findViewById(R.id.connected_account).setVisibility(View.VISIBLE);
        }
    }

    private void googleSignOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateSignOutUI(mYouTube);
                    }
                });
    }

    private void updateGoogleSignInUI(GoogleSignInAccount account) {
        // Hide button used for logging in
        mYouTube.setVisibility(View.GONE);

        LinearLayout mConnectAccounts = findViewById(R.id.connect_accounts);

        LinearLayout mConnectedAccount = (LinearLayout)getLayoutInflater().inflate(
                R.layout.logged_in_layout, mConnectAccounts, false);

        TextView mPlatformName = mConnectedAccount.findViewById(R.id.platform_name);
        mPlatformName.setText(R.string.youtube);
        mPlatformName.setTextColor(getResources().getColor(R.color.youtube));

        Button mLogout = mConnectedAccount.findViewById(R.id.logout);
        mLogout.setId(R.id.youtube_logout);
        mLogout.setBackground(getResources().getDrawable(R.drawable.youtube_button));
        mLogout.setOnClickListener(this);

        TextView mAccountInfo = mConnectedAccount.findViewById(R.id.account_info);
        mAccountInfo.setText(getString(R.string.signed_in,
                account.getDisplayName(), account.getEmail()));

        if (mConnectAccounts.findViewById(R.id.connected_account) == null) {
            mConnectAccounts.addView(mConnectedAccount);
        } else {
            mConnectAccounts.findViewById(R.id.connected_account).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateGoogleSignInUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }
}
