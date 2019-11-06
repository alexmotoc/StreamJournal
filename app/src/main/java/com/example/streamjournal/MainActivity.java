package com.example.streamjournal;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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
     * Button to record video
     */
    private Button mButtonVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mTextureView = findViewById(R.id.textureView);

        mButtonVideo = findViewById(R.id.video);
        mButtonVideo.setOnClickListener(this);

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
                mButtonVideo.setText(R.string.record);
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
        if (view.getId() == R.id.video) {
            if (!rtmpCamera2.isStreaming()) {
                if (rtmpCamera2.isRecording()
                        || rtmpCamera2.prepareAudio() && rtmpCamera2.prepareVideo()) {
                    mButtonVideo.setText(R.string.stop);
                    rtmpCamera2.startStream(RTMP_SERVER_URL);
                } else {
                    Toast.makeText(this, "Error preparing stream, This device cant do it",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                mButtonVideo.setText(R.string.record);
                rtmpCamera2.stopStream();
            }
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
            mButtonVideo.setText(getResources().getString(R.string.record));
        }
        rtmpCamera2.stopPreview();

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
}