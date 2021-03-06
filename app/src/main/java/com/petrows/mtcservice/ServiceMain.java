package com.petrows.mtcservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.petrows.mtcservice.appcontrol.ControllerList;

import java.util.List;

public class ServiceMain extends Service implements LocationListener {

	private final static String TAG = "ServiceMain";

	//dsa
	SWCReceiver mtc;

	public static boolean isRunning = false;
	public double last_speed = 0;
	ControllerList appController;

	@Override
	public IBinder onBind(Intent intent) {
		return (null);
	}

	@Override
	public void onCreate() {
		if (isRunning) return;

		isRunning = true;

		Log.d(TAG, "MTCService onCreate");

		//dsa
		IntentFilter intf = new IntentFilter();
		intf.addAction(Settings.MTCBroadcastIrkeyUp);
		intf.addAction(Settings.MTCBroadcastACC);
		intf.addAction(Settings.MTCBroadcastWidget);
		mtc = new SWCReceiver();
		registerReceiver(mtc, intf);
		Log.d(TAG, "SWCReceiver registerReceiver");

		appController = ControllerList.get(this);

		//dsa
		Settings.get(this).startMyServices();

		// Root?
		RootSession.get(this).open();

		ServiceBtReciever.get(this).connectBt();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mtc);
		mtc = null;
		isRunning = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (Settings.getServiceEnable()) {
			Log.d(TAG, "MTCService onStartCommand");

			Settings.get(this).announce(this, startId);

			LocationManager locationManager = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);
			if (null != locationManager) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			}

			// Set safe volume?
			if (Settings.get(this).getSafeVolumeEnable()) {
				Settings.get(this).setVolumeSafe();
			}

			Settings.get(this).getCallerVersionAuto();

			// Check that MTC app i snot running before start
			if (!Settings.get(this).isMTCAppRunning())
			{
				if (Settings.get(this).getMediaPlayerAutorun())
				{
					Settings.get(this).startMediaPlayer();
				}
				if (Settings.get(this).getMediaPlayerAutoplay())
				{
					ControllerList.get(this).sendKeyPlay();
				}
			} else {
				Log.d(TAG, "MTC app is running!");
			}

			return (START_STICKY);
		}
		stopSelf();
		return (START_NOT_STICKY);
	}

	@Override
	public void onLocationChanged(Location location) {

		if (!Settings.get(this).getSpeedEnable()) return; // Speed control is disabled
		if (!location.hasSpeed()) return; // Speed control is disabled

		if (Settings.get(this).getMute()) {
			// Skip volume change on Mute
			Log.d(TAG, "Set volume skipped - mute is active");
			return;
		}

		List<Integer> speed_steps = Settings.get(this).getSpeedValues();
		int vol = Settings.get(this).getVolume();
		int volNew = vol;
		int volChange = Settings.get(this).getSpeedChangeValue();

		if (0 == vol) {
			// Skip volume change on Volume == 0
			Log.d(TAG, "Set volume skipped - volume == 0");
			return;
		}

		double speed = location.getSpeed();
		speed = speed * 3.6; // m/s => km/h
		if (speed == last_speed) return;
		Log.d(TAG, "Speed is: " + speed + ", spteps is: " + speed_steps.toString());
		Log.d(TAG, "Last Speed is: " + last_speed);
		if (speed > last_speed) {
			// Speed is bigger!
			for (Integer spd_step : speed_steps) {
				if ((last_speed < spd_step) && (speed > spd_step)) {
					Log.d(TAG, "Set (+) voume: " + volNew + "+" + volChange + " / " + last_speed + " / " + speed + "(" + spd_step + ")");
					volNew = volNew + volChange;
				}
			}
		} else {
			// Speed is lower!
			for (Integer spd_step : speed_steps) {
				if ((last_speed > spd_step) && (speed < spd_step)) {
					// Speed is changed! (lower)
					Log.d(TAG, "Set (-) voume: " + volNew + "-" + volChange + " / " + last_speed + " / " + speed + "(" + spd_step + ")");
					volNew = volNew - volChange;
				}
			}
		}

		last_speed = speed;

		if (volNew > 0 && volNew != vol) {
			// Change it!
			Settings.get(this).setVolume(volNew);
			Settings.get(this).showToast("Volume " + (volNew > vol ? "+" : "-") + " (" + volNew + ")");
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}
}
