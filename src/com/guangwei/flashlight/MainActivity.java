/*
 * File Name: MainActivity.java
 * 
 * Copyright (C) 2014 Guangwei.Jiang
 * All rights reserved.
 * 
 * Description: 
 * 		This is a simple application, which control the camera LED on/off.
 * 
 * Author/Create Date:
 * 		Guangwei.Jiang, Jan07'14
 * 
 * Modify History:
 * 		
 * 
 * Notes:
 * 		v1.2.2, by Guangwei.Jiang@Jan13'14
 * 		1. Touch screen to turn on/off flash light (the old version can only click the TextView of "Light State");
 * 
 * 		v1.2.1, by Guangwei.Jiang@Jan13'14
 * 		1. Fine tune the "TimeInterval" of Shake to 200ms;
 * 
 * 		v1.2.0, by Guangwei.Jiang@Jan09'14
 * 		1. Add new feature which allow user to "shake" to control flash on/off;
 * 		2. Add the version label;
 * 
 * 		v1.1.0, by Guangwei.Jiang@Jan08'14
 * 		1. Add new feature which allow user to choose if open flash light by default;
 */

package com.guangwei.flashlight;

import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static final String TAG = "FlashLight";
	public static final String PREF = "flashlight_pref";
	public static final String PREF_isEnableFlashInit = "isEnableFlashInit";
	public static final String PREF_isEnableShake = "isEnableShake";
	private Camera camera = null;
	private boolean bFlashState = false;
	private boolean bEnableFlashInit = false;
	private boolean bEnableShake = false;
	
	private TextView textFlashState = null;
	private CheckBox checkboxEnableFlashInit = null;
	private CheckBox checkboxEnableShake = null;
	
	//检测摇动相关变量
    private long lastTime = 0;
    private long curTime = 0;
    // 控制时间间隔
    private int TimeInterval = 200;
    // 重力加速度阀值
    private int gravityThreshold = 14;
    
    private SensorManager mSensorManager;      
    private Vibrator mVibrator;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        camera = Camera.open();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
                
        textFlashState = (TextView) findViewById(R.id.textView2);
        checkboxEnableFlashInit = (CheckBox) findViewById(R.id.checkBoxEnableFlashInit);
        checkboxEnableShake = (CheckBox) findViewById(R.id.checkBoxEnableShake);
        
        restorePrefs();   
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager == null) {  
            throw new UnsupportedOperationException();  
        }  
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); 
        if (sensor == null) {  
            throw new UnsupportedOperationException();  
        }  
        boolean success = mSensorManager.registerListener(mySensorEventListener, sensor,SensorManager.SENSOR_DELAY_GAME);
        if (!success) {  
            throw new UnsupportedOperationException();  
        }else{
        	System.out.println("Register Listener sucess");
        }
        
        mVibrator = (Vibrator) getApplication().getSystemService(Context.VIBRATOR_SERVICE);
                        
        if (bFlashState) {
        	turnLightOn(camera);
        	if (bEnableShake) {
        		textFlashState.setText(R.string.flash_off_shake);
        	} else {
        		textFlashState.setText(R.string.flash_off);
        	}        	
        }
        else {
        	turnLightOff(camera);
        	if (bEnableShake) {
        		textFlashState.setText(R.string.flash_open_shake);
        	} else {
        		textFlashState.setText(R.string.flash_open);
        	} 
        }
        
        /*textFlashState.setOnClickListener(new TextView.OnClickListener()
        {
			@Override
			public void onClick(View arg0) {
				if (!bEnableShake) {
					if (!bFlashState) {
						bFlashState = true;
						turnLightOn(camera);
						textFlashState.setText(R.string.flash_off);
					} else {
						bFlashState = false;
						turnLightOff(camera);
						textFlashState.setText(R.string.flash_open);
					}
				}
			}        	
        });*/
        
        checkboxEnableFlashInit.setOnCheckedChangeListener
        (new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				SharedPreferences settings = getSharedPreferences(PREF, 0);
				
				if (checkboxEnableFlashInit.isChecked()) {
					settings.edit().putBoolean(PREF_isEnableFlashInit, true).commit();
				} else {
					settings.edit().putBoolean(PREF_isEnableFlashInit, false).commit();
				}					
			}
        	
        });        
        
        checkboxEnableShake.setOnCheckedChangeListener
        (new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				SharedPreferences settings = getSharedPreferences(PREF, 0);
				
				if (checkboxEnableShake.isChecked()) {
					bEnableShake = true;
				} else {
					bEnableShake = false;
				}	
				settings.edit().putBoolean(PREF_isEnableShake, bEnableShake).commit();
				
				if (bEnableShake) {
					if (bFlashState) {
						textFlashState.setText(R.string.flash_off_shake);
					} else {
						textFlashState.setText(R.string.flash_open_shake);
					}
				} else {
					if (bFlashState) {
						textFlashState.setText(R.string.flash_off);
					} else {
						textFlashState.setText(R.string.flash_open);
					}
				}
			}        	
        });        
    }        

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override  
    protected void onResume() {  
        super.onResume(); 
    }
    
    @Override  
    protected void onPause() {  
        super.onPause(); 
    }
    
    @Override 
    public boolean onKeyDown(int keyCode,KeyEvent event) {  
    	// check if it's "back" button  
    	if (keyCode == KeyEvent.KEYCODE_BACK) {  
    		super.onDestroy();
        	turnLightOff(camera);
        	if (mSensorManager != null) {
        		mSensorManager.unregisterListener(mySensorEventListener);
        	}
        	// Terminate the current process  
            android.os.Process.killProcess(android.os.Process.myPid());  
        	return true;
    	} else {  
    		return super.onKeyDown(keyCode, event);  
    	}
    }
    
    @Override
    public boolean onTouchEvent (MotionEvent event) {
    	switch (event.getAction()) {
    	case MotionEvent.ACTION_DOWN:
    		if (!bEnableShake) {
				if (!bFlashState) {
					bFlashState = true;
					turnLightOn(camera);
					textFlashState.setText(R.string.flash_off);
				} else {
					bFlashState = false;
					turnLightOff(camera);
					textFlashState.setText(R.string.flash_open);
				}
			}
    		break;
    	default:
    		break;
    	}
    	return super.onTouchEvent(event);
    }
    
	public static void turnLightOn(Camera mCamera) {
		try{
			if (mCamera == null) {
				return;
			}
			Parameters parameters = mCamera.getParameters();
			if (parameters == null) {
				return;
			}
			List<String> flashModes = parameters.getSupportedFlashModes();
			// Check if camera flash exists
			if (flashModes == null) {
				// Use the screen as a flashlight (next best thing)
				return;
			}
			String flashMode = parameters.getFlashMode();
			if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
				// Turn on the flash
				if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
					parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
					mCamera.setParameters(parameters);
				} else {
				}
			}
		} catch(Exception ex){}
	}
	
	public static void turnLightOff(Camera mCamera) {
		try{
			if (mCamera == null) {
				return;
			}
			Parameters parameters = mCamera.getParameters();
			if (parameters == null) {
				return;
			}
			List<String> flashModes = parameters.getSupportedFlashModes();
			String flashMode = parameters.getFlashMode();
			// Check if camera flash exists
			if (flashModes == null) {
				return;
			}
			if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
				// Turn off the flash
				if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
					parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
					mCamera.setParameters(parameters);
				} else {
					Log.e(TAG, "FLASH_MODE_OFF not supported");
				}
			}
		} catch(Exception ex){}
	}
    
    // Restore preferences
    private void restorePrefs() {
    	SharedPreferences settings = getSharedPreferences(PREF, 0);
    	bEnableFlashInit = settings.getBoolean(PREF_isEnableFlashInit, false);
    	bEnableShake = settings.getBoolean(PREF_isEnableShake, false);
    	bFlashState = bEnableFlashInit;
    	checkboxEnableFlashInit.setChecked(bEnableFlashInit);
    	checkboxEnableShake.setChecked(bEnableShake);
    }
    
    public final SensorEventListener mySensorEventListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			//获取加速度传感器的三个参数
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			//获取当前时刻的毫秒数
			curTime = System.currentTimeMillis();
			if(bEnableShake){
				if ((curTime - lastTime) > TimeInterval) {
					if ((Math.abs(x) > gravityThreshold) || (Math.abs(y) > gravityThreshold)
							|| (Math.abs(z) > gravityThreshold)) {
						//此处开始执行 
						mVibrator.vibrate( new long[]{100, 10, 100, 500}, -1);
						if (!bFlashState) {
							bFlashState = true;
							turnLightOn(camera);
							textFlashState.setText(R.string.flash_off_shake);
						} else {
							bFlashState = false;
							turnLightOff(camera);
							textFlashState.setText(R.string.flash_open_shake);
						}
					}
					lastTime = curTime;
				}
			}
		}    	
    };
}
