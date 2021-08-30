package com.jonasjuffinger.timelapse;

import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.content.Context;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.ma1co.openmemories.framework.ImageInfo;
import com.github.ma1co.openmemories.framework.MediaManager;
import com.github.ma1co.pmcademo.app.BaseActivity;

import com.sony.scalar.hardware.CameraEx;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.round;

public class ShootActivity extends BaseActivity implements SurfaceHolder.Callback, CameraEx.ShutterListener {
    private Settings settings;

    private int shotCount;

    private TextView tvCount, tvBattery, tvRemaining, tvVideoLen, tvAperature, tvShutterSpeed, tvIso, tvLastIso;
    private LinearLayout llEnd;

    private SurfaceView reviewSurfaceView;
    private SurfaceHolder cameraSurfaceHolder;
    private CameraEx cameraEx;
    private Camera camera;
    private CameraEx.AutoPictureReviewControl autoReviewControl;
    private int pictureReviewTime;

    private boolean burstShooting;
    private boolean stopPicturePreview;
    private boolean takingPicture;

    private long shootTime;
    private long shootStartTime;

    private Display display;

    static private final boolean SHOW_END_SCREEN = true;

    int getcnt() {
        if (settings.brs) {
            return 3;
        }
        return 1;
    }

    private Handler shootRunnableHandler = new Handler();
    private final Runnable shootRunnable = new Runnable() {
        @Override
        public void run() {
            if (stopPicturePreview) {
                stopPicturePreview = false;
                camera.stopPreview();
                reviewSurfaceView.setVisibility(View.GONE);
                if (settings.displayOff)
                    display.off();
            }

            if (burstShooting) {
                shoot();
            } else if (shotCount < settings.shotCount * getcnt()) {
                long remainingTime = round(shootTime + settings.interval * 1000 - System.currentTimeMillis());
                if (brck.get() > 0) {
                    remainingTime = -1;
                }

                log("  Remaining Time: " + Long.toString(remainingTime));

                if (remainingTime <= 150) { // 300ms is vaguely the time this postDelayed is to slow
                    brck.getAndDecrement();
                    shoot();
                    display.on();
                } else {
                    shootRunnableHandler.postDelayed(this, remainingTime - 150);
                }
            }
            else {
                display.on();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (SHOW_END_SCREEN) {
                            tvCount.setText("Thanks for using this app!");
                            tvBattery.setVisibility(View.INVISIBLE);
                            tvRemaining.setVisibility(View.INVISIBLE);
                            llEnd.setVisibility(View.VISIBLE);
                        } else {
                            onBackPressed();
                        }
                    }
                });
            }
        }
    };

    private Handler manualShutterCallbackCallRunnableHandler = new Handler();
    private final Runnable manualShutterCallbackCallRunnable = new Runnable() {
        @Override
        public void run() {
            onShutter(0, cameraEx);
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
        burstShooting = settings.interval == 0;

        tvCount = (TextView) findViewById(R.id.tvCount);
        tvBattery = (TextView) findViewById(R.id.tvBattery);
        tvRemaining = (TextView) findViewById(R.id.tvRemaining);
        tvVideoLen = (TextView) findViewById(R.id.tvVideoLen);
        tvAperature = (TextView) findViewById(R.id.tvAperature);
        tvShutterSpeed = (TextView) findViewById(R.id.tvShutterSpeed);
        tvIso = (TextView) findViewById(R.id.tvIso);
        tvLastIso = (TextView) findViewById(R.id.tvLastIso);
        llEnd = (LinearLayout) findViewById(R.id.llEnd);

        reviewSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        reviewSurfaceView.setZOrderOnTop(false);
        cameraSurfaceHolder = reviewSurfaceView.getHolder();
        cameraSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private CameraEx.ParametersModifier modifier;


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

        final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();

        try {
            if (settings.mf)
                params.setFocusMode(CameraEx.ParametersModifier.FOCUS_MODE_MANUAL);
            else
                params.setFocusMode("auto");
        } catch (Exception ignored) {
        }


        modifier = cameraEx.createParametersModifier(params);

        if (burstShooting) {
            try {
                modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BURST);
                List driveSpeeds = modifier.getSupportedBurstDriveSpeeds();
                modifier.setBurstDriveSpeed(driveSpeeds.get(driveSpeeds.size() - 1).toString());
                modifier.setBurstDriveButtonReleaseBehave(CameraEx.ParametersModifier.BURST_DRIVE_BUTTON_RELEASE_BEHAVE_CONTINUE);
            } catch (Exception ignored) {
            }
        } else {
            modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_SINGLE);
        }

        log("ExposureCompensation: " + params.getExposureCompensation());
        log("Aperature: " + modifier.getAperture());
        log("ShutterSpeed: " + modifier.getShutterSpeed().first + " / " + modifier.getShutterSpeed().second);
        log("ISO: " + modifier.getISOSensitivity());

        // setSilentShutterMode doesn't exist on all cameras
        try {
            modifier.setSilentShutterMode(settings.silentShutter);
        } catch (NoSuchMethodError ignored) {
        }

        try {
            //add also AEL if set
            if (settings.ael) {
                modifier.setAutoExposureLock(CameraEx.ParametersModifier.AE_LOCK_ON);
            }
        } catch (Exception e) {
            //do nothing
        }

        if (settings.brs) {
            try {
                modifier.setDriveMode(CameraEx.ParametersModifier.DRIVE_MODE_BRACKET);
                modifier.setBracketMode(CameraEx.ParametersModifier.BRACKET_MODE_EXPOSURE);
                modifier.setExposureBracketMode(CameraEx.ParametersModifier.EXPOSURE_BRACKET_MODE_SINGLE);
                modifier.setExposureBracketPeriod(30);
                modifier.setNumOfBracketPicture(3);
            } catch (Exception e) {
                //do nothing
            }
        }

        cameraEx.getNormalCamera().setParameters(params);

        pictureReviewTime = 2; //autoReviewControl.getPictureReviewTime();
        //log(Integer.toString(pictureReviewTime));


        shotCount = 0;
        shootRunnableHandler.postDelayed(shootRunnable, (long) settings.delay * 1000 * 60);
        shootStartTime = System.currentTimeMillis() + settings.delay * 1000 * 60;

        if (burstShooting) {
            manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 500);
        }

        display = new Display(getDisplayManager());

        if (settings.displayOff) {
            display.turnAutoOff(5000);
        }

        setAutoPowerOffMode(false);

        tvCount.setText(Integer.toString(shotCount) + "/" + Integer.toString(settings.shotCount * getcnt()));
        tvRemaining.setText(getRemainingTime());
        tvBattery.setText(getBatteryPercentage());
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

        if (cameraSurfaceHolder == null)
            log("cameraSurfaceHolder == null");
        else {
            cameraSurfaceHolder.removeCallback(this);
        }

        autoReviewControl = null;

        if (camera == null)
            log("camera == null");
        else {
            camera.stopPreview();
            camera = null;
        }

        if (cameraEx == null)
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
        } catch (IOException e) {
        }
    }

    private void shoot() {
        if (takingPicture)
            return;

        shootTime = System.currentTimeMillis();

        cameraEx.burstableTakePicture();

        shotCount++;

        updateDisplay();
    }

    private void updateDisplay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final Camera.Parameters params = cameraEx.getNormalCamera().getParameters();
                modifier = cameraEx.createParametersModifier(params);

                tvCount.setText(String.format("%s/%s", Integer.toString(shotCount), Integer.toString(settings.shotCount * getcnt())));
                tvRemaining.setText(getRemainingTime());
                tvBattery.setText(getBatteryPercentage());

                String strValue;
                if (settings.fps != 0)
                    strValue = String.valueOf(round(10 * (double) shotCount / settings.fps) / 10);
                else
                    strValue = String.valueOf(round(10 * (double) shotCount / 24) / 10);
                tvVideoLen.setText(String.format("%s Seconds", strValue));

                tvAperature.setText(String.valueOf((double) modifier.getAperture() / 100));

                final Pair ss = modifier.getShutterSpeed();
                log("First: " + ss.first);
                log("Second: " + ss.second);
                if (Integer.decode(ss.second + "") == 1)
                    tvShutterSpeed.setText(ss.first + "\"");
                else if (Integer.decode(ss.first + "") > 1 && Integer.decode(ss.second + "") > 1)
                    tvShutterSpeed.setText(String.valueOf((double) round(10 * (double) Integer.decode(ss.first + "") / Integer.decode(ss.second + "")) / 10));
                else
                    tvShutterSpeed.setText(ss.first.toString() + "/" + ss.second.toString());

                tvIso.setText(getISO());

                try {
                    MediaManager mediaManager = MediaManager.create(getApplicationContext());

                    Cursor imagesCursor = mediaManager.queryNewestImages();
                    imagesCursor.moveToNext();
                    ImageInfo lastImageInfo = mediaManager.getImageInfo(imagesCursor);

                    tvLastIso.setText("" + lastImageInfo.getIso());
                } catch(Exception e) {
                    Logger.error(e.getMessage());
                }

            }
        });
    }

    String getISO() {
        int iso = modifier.getISOSensitivity();

        if (iso == 0)
            return "AUTO";

        return String.valueOf(iso);
    }


    private AtomicInteger brck = new AtomicInteger(0);


    // When burst shooting this method is not called automatically
    // Therefore we called it every second manually
    @Override
    public void onShutter(int i, CameraEx cameraEx) {

        if (brck.get() < 0) {
            brck = new AtomicInteger(0);
            if (getcnt() > 1) {
                brck = new AtomicInteger(2);
            }
        }

        if (burstShooting) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvCount.setText(getRemainingTime());
                    tvRemaining.setText("");
                    tvBattery.setText(getBatteryPercentage());
                }
            });

            // just keep shooting until we have all shots
            if (System.currentTimeMillis() >= shootStartTime + settings.shotCount * 1000) {
                this.cameraEx.cancelTakePicture();
                stopPicturePreview = true;
                display.on();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (SHOW_END_SCREEN) {
                            tvCount.setText("Thanks for using this app!");
                            tvBattery.setVisibility(View.INVISIBLE);
                            tvRemaining.setVisibility(View.INVISIBLE);
                            llEnd.setVisibility(View.VISIBLE);
                        } else {
                            onBackPressed();
                        }
                    }
                });
            } else {
                manualShutterCallbackCallRunnableHandler.postDelayed(manualShutterCallbackCallRunnable, 500);
            }
        } else {
            this.cameraEx.cancelTakePicture();

            //camera.startPreview();

            if (shotCount < settings.shotCount * getcnt()) {

                // remaining time to the next shot
                double remainingTime = shootTime + settings.interval * 1000 - System.currentTimeMillis();
                if (brck.get() > 0) {
                    remainingTime = -1;
                }

                log("Remaining Time: " + remainingTime);

                // if the remaining time is negative immediately take the next picture
                if (remainingTime < 0) {
                    stopPicturePreview = false;
                    shootRunnableHandler.post(shootRunnable);
                }
                // show the preview picture for some time
                else {
                    long previewPictureShowTime = round(Math.min(remainingTime, pictureReviewTime * 1000));
                    log("  Stop preview in: " + previewPictureShowTime);
                    reviewSurfaceView.setVisibility(View.VISIBLE);
                    stopPicturePreview = true;
                    shootRunnableHandler.postDelayed(shootRunnable, previewPictureShowTime);
                }
            } else {
                stopPicturePreview = true;
                shootRunnableHandler.postDelayed(shootRunnable, pictureReviewTime * 1000);
            }
        }
    }

    private String getBatteryPercentage() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        String s = "";
        if (isCharging)
            s = "c ";

        return s + (int) (level / (float) scale * 100) + "%";
    }

    private String getRemainingTime() {
        if (burstShooting)
            return "" + round((settings.shotCount * 1000 - System.currentTimeMillis() + shootStartTime) / 1000) + "s";
        else
            return "" + round((settings.shotCount * getcnt() - shotCount) * settings.interval / 60) + "min";
    }

    @Override
    protected void onAnyKeyDown() {
        display.on();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    protected void setColorDepth(boolean highQuality) {
        super.setColorDepth(false);
    }


    private void log(String s) {
        Logger.info(s);
    }

    private void dumpList(List list, String name) {
        log(name);
        log(": ");
        if (list != null) {
            for (Object o : list) {
                log(o.toString());
                log(" ");
            }
        } else
            log("null");
        log("\n");
    }


    @Override
    protected boolean onUpperDialChanged(int value) {
        log("onUpperDialChanged: " + value);

        if (value == -1)
            cameraEx.decrementAperture();
        else if (value == 1)
            cameraEx.incrementAperture();

        updateDisplay();

        return true;
    }

    @Override
    protected boolean onLowerDialChanged(int value) {
        log("onLowerDialChanged: " + value);

        if (value == -1)
            cameraEx.decrementShutterSpeed();
        else if (value == 1)
            cameraEx.incrementShutterSpeed();

        updateDisplay();

        return true;
    }

    @Override
    protected boolean onC1KeyDown() {
        cameraEx.incrementAperture();
        updateDisplay();
        return true;
    }

    @Override
    protected boolean onC2KeyDown() {
        cameraEx.decrementAperture();
        updateDisplay();
        return true;
    }

    @Override
    protected boolean onLeftKeyDown() {
        cameraEx.decrementShutterSpeed();
        updateDisplay();
        return true;
    }

    @Override
    protected boolean onRightKeyDown() {
        cameraEx.incrementShutterSpeed();
        updateDisplay();
        return true;
    }

    @Override
    protected boolean onUpKeyDown() {
        modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());
        int iso = modifier.getISOSensitivity();

        if (iso == 0) {
            modifier.setISOSensitivity(100);
        } else {
            try {
                modifier.setISOSensitivity(2 * iso);
            } catch (Exception e) {
                Logger.error(e.getMessage());
            }
        }

        updateDisplay();
        return true;
    }

    @Override
    protected boolean onDownKeyDown() {
        modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());
        int iso = modifier.getISOSensitivity();
        
        if (iso == 100) {
            modifier.setISOSensitivity(0);
        } else {
            try {
                modifier.setISOSensitivity(iso / 2);
            } catch (Exception e) {
                Logger.error(e.getMessage());
            }
        }

        updateDisplay();
        return true;
    }
/*
    @Override
    protected boolean onAelKeyDown() {
        modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());
        modifier.setAutoExposureLock(CameraEx.ParametersModifier.AE_LOCK_ON);
        updateDisplay();
        return true;
    }
*/
}
