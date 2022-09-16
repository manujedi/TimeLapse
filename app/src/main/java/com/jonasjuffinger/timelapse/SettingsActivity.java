package com.jonasjuffinger.timelapse;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.EditText;

import com.github.ma1co.pmcademo.app.BaseActivity;
import com.sony.scalar.hardware.CameraEx;

import org.w3c.dom.Text;

import java.util.List;

public class SettingsActivity extends BaseActivity {
    private SettingsActivity that = this;

    private Settings settings;

    private TabHost tabHost;

    private Button bnStart, bnClose, btnMinMinus, btnMinPlus, btnSecMinus, btnSecPlus;
    private Button btnISOminus, btnISOplus, btnApminus, btnApplus, btnExpminus, btnExpplus, btnGetCur;
    private TextView tvIntervalValueMin, tvIntervalValueSec, tvISO, tvAP, tvExp;

    private EditText txtShots;


    private TextView tvDurationValue, tvDurationUnit;

    private AdvancedSeekBar sbDelay;
    private TextView tvDelayValue, tvDelayUnit;


    private CheckBox cbSilentShutter;
    private CheckBox cbAEL;
    private CheckBox cbMF;
    private CheckBox cbDOFF;

    private CameraEx cameraEx;
    private CameraEx.ParametersModifier modifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler))
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());

        Logger.info("Hello World");

        settings = new Settings();
        settings.load(this);

        bnStart = (Button) findViewById(R.id.bnStart);
        bnStart.setOnClickListener(bnStartOnClickListener);

        bnClose = (Button) findViewById(R.id.bnClose);
        bnClose.setOnClickListener(bnCloseOnClickListener);

        btnMinMinus = (Button) findViewById(R.id.btnMinMinus);
        btnMinPlus = (Button) findViewById(R.id.btnMinPlus);
        btnSecMinus = (Button) findViewById(R.id.btnSecMinus);
        btnSecPlus = (Button) findViewById(R.id.btnSecPlus);

        btnMinMinus.setOnClickListener(btnMinMinusListener);
        btnMinPlus.setOnClickListener(btnMinPlusListener);
        btnSecMinus.setOnClickListener(btnSecMinusListener);
        btnSecPlus.setOnClickListener(btnSecPlusListener);

        txtShots = (EditText) findViewById(R.id.txtShots);
        txtShots.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                Integer val = 1;
                try {
                    val = Integer.parseInt(s.toString());
                } catch (Exception e) {
                    s.clear();
                }
                if (val < 1)
                    val = 1;
                settings.shotCount = val;
                updateTimes();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }
        });

        tvIntervalValueMin = (TextView) findViewById(R.id.tvIntervalValueMin);
        tvIntervalValueSec = (TextView) findViewById(R.id.tvIntervalValueSec);

        tvDurationValue = (TextView) findViewById(R.id.tvDurationValue);
        tvDurationUnit = (TextView) findViewById(R.id.tvDurationUnit);
        sbDelay = (AdvancedSeekBar) findViewById(R.id.sbDelay);
        tvDelayValue = (TextView) findViewById(R.id.tvDelayValue);
        tvDelayUnit = (TextView) findViewById(R.id.tvDelayUnit);

        cbSilentShutter = (CheckBox) findViewById(R.id.cbSilentShutter);
        cbAEL = (CheckBox) findViewById(R.id.cbAEL);
        cbMF = (CheckBox) findViewById(R.id.cbMF);
        cbDOFF = (CheckBox) findViewById(R.id.cbDOFF);

        btnISOminus = (Button) findViewById(R.id.btnISOminus);
        btnISOplus = (Button) findViewById(R.id.btnISOplus);

        btnApminus = (Button) findViewById(R.id.btnApminus);
        btnApplus = (Button) findViewById(R.id.btnApplus);

        btnExpminus = (Button) findViewById(R.id.btnExpminus);
        btnExpplus = (Button) findViewById(R.id.btnExpplus);

        btnGetCur = (Button) findViewById(R.id.btnGetCur);

        btnGetCur.setOnClickListener(btnGetCurListener);
        btnISOminus.setOnClickListener(btnISOminusListener);
        btnISOplus.setOnClickListener(btnISOplusListener);

        btnApminus.setOnClickListener(btnApminusListener);
        btnApplus.setOnClickListener(btnApplusListener);

        btnExpminus.setOnClickListener(btnExpminusListener);
        btnExpplus.setOnClickListener(btnExpplusListener);


        tvISO = (TextView) findViewById(R.id.txtISO);
        tvAP = (TextView) findViewById(R.id.txtAp);
        tvExp = (TextView) findViewById(R.id.txtExp);

        sbDelay.setMax(39);
        sbDelay.setOnSeekBarChangeListener(sbDelayOnSeekBarChangeListener);
        sbDelay.setProgress(settings.rawDelay);
        sbDelayOnSeekBarChangeListener.onProgressChanged(sbDelay, settings.rawDelay, false);


        cbSilentShutter.setChecked(settings.silentShutter);
        cbSilentShutter.setOnCheckedChangeListener(cbSilentShutterOnCheckListener);
        //cbSilentShutter.setVisibility(View.INVISIBLE);

        cbAEL.setChecked(settings.ael);
        cbAEL.setOnCheckedChangeListener(cbAELOnCheckListener);

        cbMF.setChecked(settings.mf);
        cbMF.setOnCheckedChangeListener(cbMFOnCheckListener);

        cbDOFF.setChecked(settings.displayOff);
        cbDOFF.setOnCheckedChangeListener(cbDOFFOnCheckListener);

        //try {
        //CameraEx cameraEx = CameraEx.open(0, null);
        //final CameraEx.ParametersModifier modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());
        //if(modifier.isSupportedSilentShutterMode())
        //    cbSilentShutter.setVisibility(View.VISIBLE);
        /*}
        catch(Exception ignored)
        {}*/
        updateIsoApExp();
    }

    View.OnClickListener bnStartOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            settings.save(that);
            Intent intent = new Intent(that, ShootActivity.class);
            settings.putInIntent(intent);
            startActivity(intent);
        }
    },
            bnCloseOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    settings.save(that);
                    finish();
                }
            },
            btnMinMinusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Integer val = Integer.parseInt(tvIntervalValueMin.getText().toString());
                    val = val - 1;
                    if (val < 0)
                        val = 59;
                    settings.minutes = val;
                    updateTimes();
                    tvIntervalValueMin.setText(val.toString());
                }
            },
            btnMinPlusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Integer val = Integer.parseInt(tvIntervalValueMin.getText().toString());
                    val = val + 1;
                    if (val > 59)
                        val = 0;
                    settings.minutes = val;
                    updateTimes();
                    tvIntervalValueMin.setText(val.toString());
                }
            },
            btnSecMinusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Integer val = Integer.parseInt(tvIntervalValueSec.getText().toString());
                    val = val - 1;
                    if (val < 0)
                        val = 59;
                    settings.seconds = val;
                    updateTimes();
                    tvIntervalValueSec.setText(val.toString());
                }
            },
            btnSecPlusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Integer val = Integer.parseInt(tvIntervalValueSec.getText().toString());
                    val = val + 1;
                    if (val > 59)
                        val = 0;
                    settings.seconds = val;
                    updateTimes();
                    tvIntervalValueSec.setText(val.toString());
                }
            },
            btnGetCurListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraEx = CameraEx.open(0, null);
                    modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());

                    settings.iso = modifier.getISOSensitivity();
                    settings.aperture = modifier.getAperture();
                    final Pair ss = modifier.getShutterSpeed();
                    settings.exposure_num = Integer.parseInt(ss.first.toString());
                    settings.exposure_den = Integer.parseInt(ss.second.toString());

                    cameraEx.release();

                    updateIsoApExp();
                }
            },
            btnISOminusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraEx = CameraEx.open(0, null);
                    modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());

                    Integer newISO = 50;
                    for(Integer iso :(List<Integer>)modifier.getSupportedISOSensitivities()){
                        if(iso == settings.iso)
                            break;
                        newISO = iso;
                    }
                    settings.iso = newISO;
                    modifier.setISOSensitivity(settings.iso);
                    cameraEx.release();

                    updateIsoApExp();
                }
            },
            btnISOplusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraEx = CameraEx.open(0, null);
                    modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());

                    Integer newISO = 50;
                    for(Integer iso :(List<Integer>)modifier.getSupportedISOSensitivities()){
                        newISO = iso;
                        if(iso > settings.iso) {
                            break;
                        }
                    }
                    settings.iso = newISO;
                    modifier.setISOSensitivity(settings.iso);
                    cameraEx.release();

                    updateIsoApExp();
                }
            },
            btnApminusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraEx = CameraEx.open(0, null);
                    cameraEx.decrementAperture();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());

                    settings.aperture = modifier.getAperture();

                    cameraEx.release();

                    updateIsoApExp();
                }
            },
            btnApplusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraEx = CameraEx.open(0, null);
                    cameraEx.incrementAperture();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());

                    settings.aperture = modifier.getAperture();

                    cameraEx.release();

                    updateIsoApExp();
                }
            },
            btnExpminusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraEx = CameraEx.open(0, null);

                    cameraEx.decrementShutterSpeed();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());

                    final Pair ss = modifier.getShutterSpeed();
                    settings.exposure_num = Integer.parseInt(ss.first.toString());
                    settings.exposure_den = Integer.parseInt(ss.second.toString());

                    cameraEx.release();

                    updateIsoApExp();
                }
            },
            btnExpplusListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cameraEx = CameraEx.open(0, null);
                    cameraEx.incrementShutterSpeed();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    modifier = cameraEx.createParametersModifier(cameraEx.getNormalCamera().getParameters());

                    final Pair ss = modifier.getShutterSpeed();
                    settings.exposure_num = Integer.parseInt(ss.first.toString());
                    settings.exposure_den = Integer.parseInt(ss.second.toString());

                    cameraEx.release();

                    updateIsoApExp();
                }
            };


    SeekBar.OnSeekBarChangeListener sbDelayOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
            String delayTextValue = "";
            String delayUnit = "";

            settings.rawDelay = i;

            if (i < 6) {
                settings.delay = i;
                delayTextValue = Double.toString(settings.delay);
                delayUnit = "min";

            } else if (i < 16) {
                settings.delay = (i - 5) * 5 + 5;
                delayTextValue = Double.toString(settings.delay);
                delayUnit = "min";
            } else {
                settings.delay = (i - 15) * 60;
                delayTextValue = Double.toString(i - 15);
                delayUnit = "h";
            }
            tvDelayValue.setText(delayTextValue);
            tvDelayUnit.setText(delayUnit);

            updateTimes();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    AdapterView.OnItemSelectedListener spnFpsOnItemSelectedListener
            = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            String sfps = getResources().getStringArray(R.array.fps)[i];
            updateTimes();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };

    CheckBox.OnCheckedChangeListener cbSilentShutterOnCheckListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            settings.silentShutter = b;
        }
    };

    CheckBox.OnCheckedChangeListener cbAELOnCheckListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            settings.ael = b;
        }
    };


    CheckBox.OnCheckedChangeListener cbMFOnCheckListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            settings.mf = b;
        }
    };

    CheckBox.OnCheckedChangeListener cbDOFFOnCheckListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            settings.displayOff = b;
        }
    };

    void updateTimes() {
        if (settings.shotCount == Integer.MAX_VALUE) {
            tvDurationValue.setText("inf");
            tvDurationUnit.setText("");
            return;
        }

        int duration = 0;
        int videoTime = 0;
        int interval = settings.minutes * 60 + settings.seconds;
        if (interval == 0) {
            duration = settings.shotCount;
        } else {
            duration = (int) Math.round(interval * settings.shotCount);
        }

        if (duration < 60) {
            tvDurationValue.setText("" + duration);
            tvDurationUnit.setText("s");
        } else {
            tvDurationValue.setText("" + (duration / 60));
            tvDurationUnit.setText("min");
        }

    }

    void updateIsoApExp(){
        tvISO.setText(String.valueOf(settings.iso));
        tvAP.setText(Double.toString(((double)settings.aperture)/100));
        if(settings.exposure_den == 1)
            tvExp.setText(String.valueOf(settings.exposure_den));
        else
            tvExp.setText(settings.exposure_num + "/" + settings.exposure_den);
    }


    @Override
    protected boolean onMenuKeyUp() {
        onBackPressed();
        return true;
    }
}
