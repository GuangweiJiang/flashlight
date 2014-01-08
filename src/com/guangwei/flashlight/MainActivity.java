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
 * 		v1.1.0, by Guangwei.Jiang@Jan08'14
 * 		1. Add new feature which allow user to choose if open flash light by default;
 */

package com.guangwei.flashlight;

import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static final String TAG = "FlashLight";
	public static final String PREF = "flashlight_pref";
	public static final String PREF_isEnableFlashInit = "isEnableFlashInit";
	private Camera camera = null;
	private boolean bFlashState = false;
	private boolean bEnableFlashInit = false;
	
	private TextView textFlashState = null;
	private CheckBox checkboxEnableFlashInit = null;
	
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        camera = Camera.open();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
                
        textFlashState = (TextView) findViewById(R.id.textView2);
        checkboxEnableFlashInit = (CheckBox) findViewById(R.id.checkBoxEnableFlashInit);
        
        restorePrefs();     
                
        if (bFlashState)
        {
        	turnLightOn(camera);
        	textFlashState.setText(R.string.flash_off);
        }
        else
        {
        	turnLightOff(camera);
        	textFlashState.setText(R.string.flash_open);
        }
        
        textFlashState.setOnClickListener(new TextView.OnClickListener()
        {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (!bFlashState)
		        {
					bFlashState = true;
		        	turnLightOn(camera);
		        	textFlashState.setText(R.string.flash_off);
		        }
		        else
		        {
		        	bFlashState = false;
		        	turnLightOff(camera);
		        	textFlashState.setText(R.string.flash_open);
		        }
			}
        	
        }
        );
        
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
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    

    @Override 
    public boolean onKeyDown(int keyCode,KeyEvent event) {  
    	// check if it's "back" button  
    	if (keyCode == KeyEvent.KEYCODE_BACK) {  
    		super.onDestroy();
        	turnLightOff(camera);
        	// Terminate the current process  
            android.os.Process.killProcess(android.os.Process.myPid());  
        	return true;
    	} else {  
    		return super.onKeyDown(keyCode, event);  
    	}
    }
    
    // Restore preferences
    private void restorePrefs() {
    	SharedPreferences settings = getSharedPreferences(PREF, 0);
    	bEnableFlashInit = settings.getBoolean(PREF_isEnableFlashInit, false);
    	bFlashState = bEnableFlashInit;
    	checkboxEnableFlashInit.setChecked(bEnableFlashInit);
    }
}
