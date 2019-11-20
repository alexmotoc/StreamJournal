package com.example.streamjournal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ProfileActivity";
    private static final int RC_SIGN_IN = 1;

    private GoogleSignInClient mGoogleSignInClient;

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
            case R.id.youtube_login:
                googleSignIn();
                break;
        }
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void updateGoogleSignInUI(GoogleSignInAccount account) {
        // Hide button used for logging in
        mYouTube.setVisibility(View.GONE);

        LinearLayout mConnectAccounts = findViewById(R.id.connectAccounts);

        LinearLayout mConnectedAccount = (LinearLayout)getLayoutInflater().inflate(
                R.layout.logged_in_layout, mConnectAccounts, false);

        TextView mPlatformName = mConnectedAccount.findViewById(R.id.platformName);
        mPlatformName.setText(R.string.youtube);
        mPlatformName.setTextColor(getResources().getColor(R.color.youtube));

        Button mLogout = mConnectedAccount.findViewById(R.id.logout);
        mLogout.setBackground(getResources().getDrawable(R.drawable.youtube_button));

        TextView mAccountInfo = mConnectedAccount.findViewById(R.id.accountInfo);
        mAccountInfo.setText(getString(R.string.signed_in_google,
                account.getDisplayName(), account.getEmail()));

        mConnectAccounts.addView(mConnectedAccount);
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
