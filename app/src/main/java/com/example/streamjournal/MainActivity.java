package com.example.streamjournal;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtplibrary.rtmp.RtmpCamera2;
import com.pedro.rtplibrary.view.AutoFitTextureView;
import net.ossrs.rtmp.ConnectCheckerRtmp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * More documentation see:
 * {@link com.pedro.rtplibrary.base.Camera2Base}
 * {@link com.pedro.rtplibrary.rtmp.RtmpCamera2}
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity
        implements ConnectCheckerRtmp, View.OnClickListener,
        TextureView.SurfaceTextureListener, TextureView.OnTouchListener {

    private static final String RTMP_SERVER_URL = "rtmp://tungsten.alexlogan.co.uk:8569/ingest/cs407";

    /**
     * A reference to the camera device.
     */
    private RtmpCamera2 rtmpCamera2;

    /**
     * An {@link TextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * Button to record video.
     */
    private Button mButtonVideo;

    /**
     *
     * Button to access settings menu.
     */
    private Button mButtonSettings;

    /**
     *
     * Button to flip camera.
     */
    private Button mButtonFlipCamera;

    /**
     *
     * Button to access user profile.
     */
    private Button mButtonProfile;

    /**
     *
     * Used to display how long the stream has been online for.
     */
    private TextView mTimer;

    private Handler timerHandler = new Handler();
    private long startTime = 0L;
    long upTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mTextureView = findViewById(R.id.textureView);

        mButtonVideo = findViewById(R.id.video);
        mButtonVideo.setOnClickListener(this);

        mButtonSettings = findViewById(R.id.settings);
        mButtonSettings.setOnClickListener(this);

        mButtonFlipCamera = findViewById(R.id.flipCamera);
        mButtonFlipCamera.setOnClickListener(this);

        mButtonProfile = findViewById(R.id.profile);
        mButtonProfile.setOnClickListener(this);

        mTimer = findViewById(R.id.timer);

        rtmpCamera2 = new RtmpCamera2(mTextureView, this);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setOnTouchListener(this);
    }

    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT)
                        .show();
                startTime = SystemClock.uptimeMillis();
                timerHandler.postDelayed(updateTimerThread, 0);
                mTimer.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onConnectionFailedRtmp(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Connection failed. " + reason,
                        Toast.LENGTH_SHORT).show();
                rtmpCamera2.stopStream();
            }
        });
    }

    @Override
    public void onNewBitrateRtmp(long bitrate) {
    }

    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                timerHandler.removeCallbacks(updateTimerThread);
                mTimer.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video:
                if (!rtmpCamera2.isStreaming()) {
                    if (rtmpCamera2.isRecording()
                            || rtmpCamera2.prepareAudio() && rtmpCamera2.prepareVideo()) {
                        mButtonVideo.setBackgroundResource(R.drawable.stop_recording_button);
                        rtmpCamera2.startStream(RTMP_SERVER_URL);
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mButtonVideo.setBackgroundResource(R.drawable.recording_button);
                    rtmpCamera2.stopStream();
                }
                break;
            case R.id.flipCamera:
                try {
                    rtmpCamera2.switchCamera();
                } catch (CameraOpenException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            case R.id.profile:
                Intent profileIntent = new Intent(this, ProfileActivity.class);
                startActivity(profileIntent);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            // Send normalised coordinates of the tap to the REST Api
            final AsyncHttpClient client = new AsyncHttpClient();
            client.get("https://tungsten.alexlogan.co.uk/effect/b5583fa0-60ac-4ce1-8ba5-352d80757933/", new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        JSONObject effects = new JSONObject(response.getString("effects"));
                        JSONObject targetArea = new JSONObject();
                        targetArea.put("x", event.getX() / v.getWidth());
                        targetArea.put("y", event.getY() / v.getHeight());
                        effects.put("target", targetArea);

                        RequestParams params = new RequestParams();
                        params.put("effects", effects.toString());
                        client.put("https://tungsten.alexlogan.co.uk/effect/b5583fa0-60ac-4ce1-8ba5-352d80757933/", params, new JsonHttpResponseHandler());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return true;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        mTextureView.setAspectRatio(1080, 1920);
        rtmpCamera2.startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        rtmpCamera2.startPreview();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (rtmpCamera2.isStreaming()) {
            rtmpCamera2.stopStream();
            mButtonVideo.setBackgroundResource(R.drawable.recording_button);
        }
        rtmpCamera2.stopPreview();

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            upTime = SystemClock.uptimeMillis() - startTime;

            int secs = (int) (upTime / 1000);
            int mins = secs / 60;
            int hours = mins / 60;
            secs = secs % 60;

            if (hours == 0) {
                mTimer.setText(getString(R.string.time_no_hours, mins, secs));
            } else {
                mTimer.setText(getString(R.string.time_with_hours, hours, mins, secs));
            }

            timerHandler.postDelayed(this, 0);
        }

    };
}