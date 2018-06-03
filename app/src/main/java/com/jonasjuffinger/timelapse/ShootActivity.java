package com.jonasjuffinger.timelapse;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.ma1co.pmcademo.app.BaseActivity;

import com.sony.scalar.hardware.CameraEx;

import java.io.IOException;
import java.util.List;

public class ShootActivity  extends BaseActivity implements SurfaceHolder.Callback, CameraEx.ShutterListener
{
    private Settings settings;

    private int shotCount;

    private TextView tvCount, tvBattery; //, tvInfo;
    private LinearLayout llEnd;

    private SurfaceView reviewSurfaceView;
    private SurfaceHolder cameraSurfaceHolder;
    private CameraEx cameraEx;
    private Camera camera;
    private CameraEx.AutoPictureReviewControl autoReviewControl;
    private int pictureReviewTime;

    private boolean stopPicturePreview;
    private boolean takingPicture;

    private long shootTime;

    private Display display;

    Intent batteryStatus;

    private Handler shootRunnableHandler = new Handler();
    private final Runnable shootRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if(stopPicturePreview) {
                stopPicturePreview = false;
                camera.stopPreview();
                reviewSurfaceView.setVisibility(View.GONE);
                //display.off();
            }

            if(shotCount < settings.shotCount) {
                long remainingTime = shootTime + settings.interval * 1000 - System.currentTimeMillis();

                log("  Remaining Time: " + Long.toString(remainingTime));

                if (remainingTime <= 150) { // 300ms is vaguely the time this postDelayed is to slow
                    shoot();
                    if(!settings.displayOff) {
                        log(Boolean.toString(settings.displayOff));
                        display.on();
                    }
                } else {
                    shootRunnableHandler.postDelayed(this, remainingTime-150);
                }
            }
            else {
                display.on();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvCount.setText("Thanks for using this app!");
                        llEnd.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoot);

        Intent intent = getIntent();
        settings = Settings.getFromIntent(intent);

        shotCount = 0;
        takingPicture = false;

        tvCount = (TextView) findViewById(R.id.tvCount);
        tvBattery = (TextView) findViewById(R.id.tvBattery);
        llEnd = (LinearLayout) findViewById(R.id.llEnd);

        reviewSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        reviewSurfaceView.setZOrderOnTop(false);
        cameraSurfaceHolder = reviewSurfaceView.getHolder();
        cameraSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = registerReceiver(null, ifilter);
    }


    @Override
    protected void onResume() {
        log("onResume");

        super.onResume();
        cameraEx = CameraEx.open(0, null);
        cameraEx.setShutterListener(this);
        cameraSurfaceHolder.addCallback(this);
        autoReviewControl = new CameraEx.AutoPictureReviewControl();
        //autoReviewControl.setPictureReviewInfoHist(true);
        cameraEx.setAutoPictureReviewControl(autoReviewControl);

        final Camera.Parameters params = cameraEx.createEmptyParameters();
        final CameraEx.ParametersModifier modifier = cameraEx.createParametersModifier(params);
        modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);
        // setSilentShutterMode doesn't exist on all cameras
        try {
            modifier.setSilentShutterMode(settings.silentShutter);
        }
        catch(NoSuchMethodError ignored)
        {}
        cameraEx.getNormalCamera().setParameters(params);

        pictureReviewTime = autoReviewControl.getPictureReviewTime();
        log(Integer.toString(pictureReviewTime));

        shootRunnableHandler.postDelayed(shootRunnable, settings.interval * 1000);

        display = new Display(getDisplayManager());

        if(settings.displayOff) {
            display.turnAutoOff(5000);
        }

        setAutoPowerOffMode(false);

        //tvBattery.setText(Integer.toString(getBatteryPercentage()));
    }

    @Override
    protected boolean onMenuKeyUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        display.on();
        display.turnAutoOff(Display.NO_AUTO_OFF);

        super.onPause();

        log("on pause");

        shootRunnableHandler.removeCallbacks(shootRunnable);

        if(cameraSurfaceHolder == null)
            log("cameraSurfaceHolder == null");
        else {
            cameraSurfaceHolder.removeCallback(this);
        }

        autoReviewControl = null;

        if(camera == null)
            log("camera == null");
        else {
            camera.stopPreview();
            camera = null;
        }

        if(cameraEx == null)
            log("cameraEx == null");
        else {
            cameraEx.setAutoPictureReviewControl(null);
            cameraEx.release();
            cameraEx = null;
        }

        setAutoPowerOffMode(true);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = cameraEx.getNormalCamera();
            camera.setPreviewDisplay(holder);
        }
        catch (IOException e) {}
    }


    private void shoot() {
        if(takingPicture)
            return;

        shootTime = System.currentTimeMillis();

        cameraEx.burstableTakePicture();

        shotCount++;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvCount.setText(Integer.toString(shotCount));
                tvBattery.setText(Integer.toString(getBatteryPercentage()));
            }
        });
    }


    @Override
    public void onShutter(int i, CameraEx cameraEx) {
        this.cameraEx.cancelTakePicture();

        camera.startPreview();

        if(shotCount < settings.shotCount) {

            // remaining time to the next shot
            long remainingTime = shootTime + settings.interval * 1000 - System.currentTimeMillis();

            log("Remaining Time: " + Long.toString(remainingTime));

            // if the remaining time is negative immediately take the next picture
            if (remainingTime < 0) {
                stopPicturePreview = false;
                shootRunnableHandler.post(shootRunnable);
            }
            // show the preview picture for some time
            else {
                long previewPictureShowTime = Math.min(remainingTime, pictureReviewTime * 1000);
                log("  Stop preview in: " + Long.toString(previewPictureShowTime));
                reviewSurfaceView.setVisibility(View.VISIBLE);
                stopPicturePreview = true;
                shootRunnableHandler.postDelayed(shootRunnable, previewPictureShowTime);
            }
        }
        else {
            stopPicturePreview = true;
            shootRunnableHandler.postDelayed(shootRunnable, pictureReviewTime * 1000);
        }
    }

    private int getBatteryPercentage()
    {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return (int) (level / (float)scale * 100);
    }

    @Override
    protected void onAnyKeyDown() {
        display.on();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    protected void setColorDepth(boolean highQuality)
    {
        super.setColorDepth(false);
    }


    private void log(String s) {
        Logger.info(s);
    }

    private void dumpList(List list, String name) {
        log(name);
        log(": ");
        if (list != null)
        {
            for (Object o : list)
            {
                log(o.toString());
                log(" ");
            }
        }
        else
            log("null");
        log("\n");
    }
}
