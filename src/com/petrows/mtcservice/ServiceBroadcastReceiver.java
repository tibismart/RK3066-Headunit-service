package com.petrows.mtcservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ServiceBroadcastReceiver extends BroadcastReceiver {
	
	public ServiceBroadcastReceiver() {}
	
	@Override
	public void onReceive(Context ctx, Intent arg1) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
	}

}