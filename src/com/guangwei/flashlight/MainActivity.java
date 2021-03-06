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
 * 
 * Notes:
 * 		v1.2.12, by Guangwei.Jjiang@Feb25'14
 * 		1. Add "comments" and "quit" in the menu;
 * 
 * 		v1.2.11, by Guangwei.Jiang@Feb12'14
 * 		1. Fix can't detect Torch brightness node permission issue, which cause MX3 can't turn on flash!
 * 
 * 		v1.2.10, by Guangwei.Jiang@Feb12'14
 * 		1. Check if the torch brightness node is writable or not,
 * 			if writable, then operate by the node;
 * 			or, operate by camera standard APIs. 
 * 
 * 		v1.2.9, by Guangwei.Jiang@Feb11'14
 * 		1. Add feature to adjust Meizu torch brightness (don't support all of Serial, such as MX3);
 * 
 * 		v1.2.8, by Guangwei.Jiang@Feb10'14
 * 		1. Turn on flash light by default.
 * 
 * 		v1.2.7, by Guangwei.Jiang@Feb07'14
 * 		1. 修正v1.2.6带来的“解锁后意外打开手电筒”的issue。
 * 
 * 		v1.2.6, by Guangwei.Jiang@Feb06'14
 * 		1. 修正issue: 有时手电筒不亮。
 * 			复制步骤：
 * 			a. 选中“程序开启时自动打开手电”；
 * 			b. 熄灭手电筒，按“Home”键，将程序置于后台（此时程序依然在执行）；
 * 			c. 在桌面点击“手电筒”图标，此时手电不亮。 
 * 
 * 		v1.2.5, by Guangwei.Jiang@Jan17'14
 * 		1. Replace the launcher icon;
 * 
 * 		v1.2.4, by Guangwei.Jiang@Jan14'14
 * 		1. Fine tune "gravityThreshold" value to 18;
 * 		2. Fine tune "TimeDebunce" to 1000ms;
 * 		3. Unregister "SensorEventListener" on onPause(); and register again onResume();
 * 
 * 		v1.2.3, by Guangwei.Jiang@Jan13'14
 * 		1. Continue fine tune the Shake feature;
 * 
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static final String TAG = "FlashLight";
	private static final String PREF = "flashlight_pref";
	private static final String PREF_isEnableFlashInit = "isEnableFlashInit";
	private static final String PREF_isEnableShake = "isEnableShake";
	private static final String PREF_TorchBrightnessLevel = "TorchBrightnessLevel";
	private Camera camera = null;
	private boolean bFlashState = false;
	private boolean bEnableFlashInit = false;
	private boolean bEnableShake = false;
	private int TorchBrightnessLevel = 0;
	
	private TextView textFlashState = null;
	private CheckBox checkboxEnableFlashInit = null;
	private CheckBox checkboxEnableShake = null;
	
	//检测摇动相关变量
    private long lastTime = 0;
    private long curTime = 0;
    /// TimeInterval时间间隔内，两次G值均超gravityThreshold，则执行开关灯动作；
    private int TimeInterval = 100;  
    // 执行完开关灯动作后，等待TimeDebunce (ms) 后，方可进入下一次G值等待判断；
    private int TimeDebunce = 1000;
    // 重力加速度阀值
    private int gravityThreshold = 18;
    
    private SensorManager mSensorManager;      
    private Vibrator mVibrator;
    
    private boolean bTorchNodewritable = true;
    
    private final int MENU_COMMENTS = Menu.FIRST;
    private final int MENU_QUIT = Menu.FIRST+1;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        camera = Camera.open();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
                
        textFlashState = (TextView) findViewById(R.id.textView2);
        checkboxEnableFlashInit = (CheckBox) findViewById(R.id.checkBoxEnableFlashInit);
        checkboxEnableShake = (CheckBox) findViewById(R.id.checkBoxEnableShake);
        
        // The below command will check if the torch brightness node is writable or not
        String cmd[] = { "/system/bin/sh", "-c", "echo 0 > /sys/class/leds/torch_led/brightness"};
		CmdExec(cmd);
        
        restorePrefs();   
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager == null) {  
            throw new UnsupportedOperationException();  
        }  
        
        mVibrator = (Vibrator) getApplication().getSystemService(Context.VIBRATOR_SERVICE);
        
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
    	menu.add(Menu.NONE, MENU_COMMENTS, 0, R.string.action_comments);
    	menu.add(Menu.NONE, MENU_QUIT, 0, R.string.action_quit);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	super.onOptionsItemSelected(item);
    	switch(item.getItemId())
    	{
    	case MENU_COMMENTS:
    		Uri uri = null;
    		uri = Uri.parse("mstore:http://app.meizu.com/phone/apps/abfd9fa1f4a24e238b0db26a67a006ad");
    		startActivity(new Intent("android.intent.action.VIEW", uri));
    		break;
    	case MENU_QUIT:
    		quit();
    		break;
    	}
		return true;    	
    }
    
    @Override  
    protected void onResume() {  
        super.onResume();         

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

        
        // If bFlashState is true, then doesn't read the Perf data;
        // If bFlashState is false, then read Perf data again!
        if (!bFlashState) {
        	restorePrefs();  
        }
        
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
    }
    
    @Override  
    protected void onPause() {  
        super.onPause(); 
        
    	if (mSensorManager != null) {
    		mSensorManager.unregisterListener(mySensorEventListener);
    	}
    	
    	// If flash is off and go to background, then kill this process.
    	if (!bFlashState) {
    		quit();
    	}
    }
        
    @Override 
    public boolean onKeyDown(int keyCode,KeyEvent event) {  
    	// check if it's "back" button  
    	if (keyCode == KeyEvent.KEYCODE_BACK) { 
        	quit(); 
        	return true;
    	} else if (bTorchNodewritable && (keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
    		increaseMeizuTorchBrightLevel();
    		return true;
    	} else if (bTorchNodewritable && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
    		decreaseMeizuTorchBrightLevel();
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

    private void quit() { 
		super.onDestroy();
    	turnLightOff(camera);
    	if (mSensorManager != null) {
    		mSensorManager.unregisterListener(mySensorEventListener);
    	}
    	// Terminate the current process  
        android.os.Process.killProcess(android.os.Process.myPid());  
    }
    
	public void turnLightOn(Camera mCamera) {
		if (bTorchNodewritable) {
			restoreMeizuTorchBrightLevel();
		} else {
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
	}
	
	public void turnLightOff(Camera mCamera) {
		if (bTorchNodewritable) {
			saveMeizuTorchBrightLevel();
		} else {
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
	}
    
    // Restore preferences
    private void restorePrefs() {
    	SharedPreferences settings = getSharedPreferences(PREF, 0);
    	bEnableFlashInit = settings.getBoolean(PREF_isEnableFlashInit, true);
    	bEnableShake = settings.getBoolean(PREF_isEnableShake, false);
    	bFlashState = bEnableFlashInit;
    	checkboxEnableFlashInit.setChecked(bEnableFlashInit);
    	checkboxEnableShake.setChecked(bEnableShake);
    	TorchBrightnessLevel = settings.getInt(PREF_TorchBrightnessLevel, 15);
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
						lastTime = curTime + TimeDebunce;
					} else {
						lastTime = curTime;
					}
				}
			}
		}    	
    };
    
    private void CmdExec(String[] cmd) {
    	String result = "";
    	try {
    		Process proc = Runtime.getRuntime().exec(cmd);
    		InputStream in = proc.getErrorStream();
			BufferedReader mReader = new BufferedReader(new InputStreamReader(in));
			while ((result = mReader.readLine()) != null) {
	    		Log.e(TAG, result);
				bTorchNodewritable = false;
			}
			mReader.close();
			in.close();
    		proc.waitFor();
    	} catch (Exception e) {
    		e.printStackTrace();    		
    	}
    }
    
    private String CmdExec(String cmd) {
    	String result = "";
    	
    	try {
    		Process proc = Runtime.getRuntime().exec(cmd);
			InputStream in = proc.getInputStream();
    		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    		result = reader.readLine();
    		reader.close();
    		in.close();
    		proc.waitFor();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	Log.v(TAG, result);
    	return result;
    }
    
    private void saveMeizuTorchBrightLevel() {
		String cmd = "cat /sys/class/leds/torch_led/brightness";
		String brightness = CmdExec(cmd);
		
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		settings.edit().putInt(PREF_TorchBrightnessLevel, Integer.parseInt(brightness)).commit();
		
		// Turn off Torch
		String cmd02[] = { "/system/bin/sh", "-c", "echo 0 > /sys/class/leds/torch_led/brightness"};
		CmdExec(cmd02);
    }
    
    private void restoreMeizuTorchBrightLevel() {
    	SharedPreferences settings = getSharedPreferences(PREF, 0);
    	TorchBrightnessLevel = settings.getInt(PREF_TorchBrightnessLevel, 15);
    	
    	if (TorchBrightnessLevel > 105) {
    		TorchBrightnessLevel = 105;
    	} else if (TorchBrightnessLevel <= 0) {
    		TorchBrightnessLevel = 15;
    	}
    	
    	String cmd[] = { "/system/bin/sh", "-c", "echo " + Integer.toString(TorchBrightnessLevel) + " > /sys/class/leds/torch_led/brightness"};
		CmdExec(cmd);
    }
    
    private void increaseMeizuTorchBrightLevel() {    	
    	TorchBrightnessLevel += 15;
    	
    	if (TorchBrightnessLevel > 105) {
    		TorchBrightnessLevel = 105;
    	} else if (TorchBrightnessLevel <= 0) {
    		TorchBrightnessLevel = 15;
    	}
    	
    	String cmd[] = { "/system/bin/sh", "-c", "echo " + Integer.toString(TorchBrightnessLevel) + " > /sys/class/leds/torch_led/brightness"};
		CmdExec(cmd);
    }
    
    private void decreaseMeizuTorchBrightLevel() {    	
    	TorchBrightnessLevel -= 15;
    	
    	if (TorchBrightnessLevel > 105) {
    		TorchBrightnessLevel = 105;
    	} else if (TorchBrightnessLevel <= 0) {
    		TorchBrightnessLevel = 15;
    	}
    	
    	String cmd[] = { "/system/bin/sh", "-c", "echo " + Integer.toString(TorchBrightnessLevel) + " > /sys/class/leds/torch_led/brightness"};
		CmdExec(cmd);
    }
	
}
