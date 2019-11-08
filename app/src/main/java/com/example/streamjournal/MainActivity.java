package com.example.streamjournal;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.SystemClock;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.pedro.rtplibrary.rtmp.RtmpCamera2;
import com.pedro.rtplibrary.view.AutoFitTextureView;
import net.ossrs.rtmp.ConnectCheckerRtmp;

/**
 * More documentation see:
 * {@link com.pedro.rtplibrary.base.Camera2Base}
 * {@link com.pedro.rtplibrary.rtmp.RtmpCamera2}
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity
        implements ConnectCheckerRtmp, View.OnClickListener, TextureView.SurfaceTextureListener {

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
                break;
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        mTextureView.setAspectRatio(480, 640);
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