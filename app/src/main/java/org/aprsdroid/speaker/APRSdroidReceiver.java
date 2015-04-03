package org.aprsdroid.speaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/** This class is instanciated by Android when APRSdroid intents are received,
 * according to AndroidManifest.xml.
 *
 * The receiver checks if Speaker is enabled, and launches the SpeakerService to read back the
 * actual text.
 */
public class APRSdroidReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		PreferenceManager.setDefaultValues(context, R.xml.speakerprefs, false);

		if (!prefs.getBoolean("enable_speaker", true))
			return;

		Log.d("Speaker", "forwarding intent: " + intent);
		readAloud(context, intent);
	}

	private void readAloud(Context context, Intent intent) {
		intent.setClass(context, SpeakerService.class);
		context.startService(intent);
	}
}
