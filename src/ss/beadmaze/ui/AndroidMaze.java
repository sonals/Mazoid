/*
 * Copyright (c) 2010, 2012, Sonal Santan < sonal DOT santan AT gmail DOT com >
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package ss.beadmaze.ui;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;

public class AndroidMaze extends Activity {
	private static final int MENU_ABOUT = 0;
	private static final int MENU_SPOTLIGHT_MODE = 1;
	private SensorManager mSensorManager = null;
	private Sensor mOrientationSensor = null;
	private AndroidMazeView mView = null;
	private int mPreviousKeyCode = KeyEvent.FLAG_WOKE_HERE;
	private PowerManager.WakeLock mNoScreenDim;  
	
	private class OrientationSensorListener implements SensorEventListener {
		// Converts Sensor Input to Keyboard style left/right/up/down key events
		public void onSensorChanged(SensorEvent event) {
			// If we do not have focus then ignore all sensor events
			// This can happen on a loooong press of HOME key, when window
			// manager shows the task list. Note that the Activity is
			// not PAUSED at this point it is still visible in the background 
			if (!AndroidMaze.this.hasWindowFocus())
				return;
			float[] values = event.values;
			float speed = 0.0f;
			int sensor = event.sensor.getType();
			assert(sensor == SensorManager.SENSOR_ORIENTATION);
			
			final int speedX = (int)(values[1] * AndroidMazeView.mSpeedMax) / 180;
			final int speedY = (int)(values[2] * AndroidMazeView.mSpeedMax) / 90;
			mView.onSensorEvent(speedX, speedY);
			
			int keyCode1, keyCode2, keyCode;
			if (values[1] > 0) {
				keyCode1 = KeyEvent.KEYCODE_DPAD_UP;
			}
			else {
				keyCode1 = KeyEvent.KEYCODE_DPAD_DOWN;
			}
			if (values[2] > 0) {
				keyCode2 = KeyEvent.KEYCODE_DPAD_LEFT;
			}
			else {
				keyCode2 = KeyEvent.KEYCODE_DPAD_RIGHT;
			}
			if (Math.abs(values[1]) > Math.abs(values[2])) {
				// speed ranges between 0 to mSpeedMax
				speed = (values[1] * AndroidMazeView.mSpeedMax) / 180;
				keyCode = keyCode1;
			}
			else {
				// speed ranges between 0 to mSpeedMax
				speed = (values[2] * AndroidMazeView.mSpeedMax) / 90;
				keyCode = keyCode2;
			}
			if ((keyCode != mPreviousKeyCode) && (mPreviousKeyCode != KeyEvent.FLAG_WOKE_HERE)) {
				// We just switched direction, first send a key up to indicate end previous
				// keydown. before we send the real input
				mView.onKeyUp(mPreviousKeyCode, null);
				mPreviousKeyCode = keyCode;
			}
			mView.onKeyDown(keyCode, speed > 0 ? speed : -speed);
		}
		
		//@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
		}
		
		 

	}
	
	private final OrientationSensorListener mSensorListener = new OrientationSensorListener();

	/**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     * 
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mView.saveState(outState);
    }
	
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
    }

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        // Get rid of statusbar, else the lower portion of our maze is clipped
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);  
        mNoScreenDim = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "DoNotDimScreen");  
        //mNoScreenDim = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "DoNotDimScreen");  
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mView = new AndroidMazeView(this, null, savedInstanceState);
        setContentView(mView);
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(mView);
  
        // Create orientation sensor
        List<Sensor> orientationList = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        if (orientationList.size() > 0) {
        	mOrientationSensor = orientationList.get(0);
        	mSensorManager.registerListener(mSensorListener, mOrientationSensor,
        									SensorManager.SENSOR_DELAY_GAME);
        }
    } 
    
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ABOUT, 0, "About");
        menu.add(0, MENU_SPOTLIGHT_MODE, 0, "Toggle Spotlight");
        return super.onCreateOptionsMenu(menu);
    }
    
    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ABOUT:
        {
        	mSensorManager.unregisterListener(mSensorListener);
        	mView.pause();
            aboutGame();
            return true;
        }
    	case MENU_SPOTLIGHT_MODE:
    	{
    		mSensorManager.unregisterListener(mSensorListener);
    		mView.pause();
    		toggleSpotlightMode();
    		return true;
    	}
        }
        return false;
    }
    
    private void aboutGame() {
    	AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("About " + getResources().getString(R.string.app_name));
        adb.setIcon(R.drawable.icon);
        PackageManager pm = getPackageManager();
        String version = null;
        try {
            //---get the package info---
            PackageInfo pi =  pm.getPackageInfo(this.getPackageName(), 0);
            version = pi.versionName;
        } catch (NameNotFoundException e) {
        }
        	
        String msg = "Version " + version + "\nCopyright (c) 2010 Ritual Droid" ;       
        adb.setMessage(msg);
        adb.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,
                    int which) {
                dialog.cancel();
                mSensorManager.registerListener(mSensorListener, mOrientationSensor, SensorManager.SENSOR_DELAY_GAME);
            }
        });
        AlertDialog ad = adb.create();
        ad.show();
    }
    
    private void toggleSpotlightMode() {
    	mView.toggleSpotlightMode();
    	mSensorManager.registerListener(mSensorListener, mOrientationSensor, SensorManager.SENSOR_DELAY_GAME);
    }
    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mNoScreenDim.release();
        mSensorManager.unregisterListener(mSensorListener);
        mView.pause(); // pause game when Activity pauses
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNoScreenDim.acquire();
        mSensorManager.registerListener(mSensorListener, mOrientationSensor, SensorManager.SENSOR_DELAY_GAME);
        // View automatically resumes when it receives key events
    }
}