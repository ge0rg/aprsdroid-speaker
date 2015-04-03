package org.aprsdroid.speaker;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;


/** This class is the Activity that is used to configure APRSdroid Speaker.
 *
 * The only logic contained in here is to launch a speaker service test.
 */
public class SpeakerActivity extends PreferenceActivity {

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.speakerprefs, false);

		addPreferencesFromResource(R.xml.speakerprefs);

		findPreference("test").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent read_test = new Intent(SpeakerActivity.this, SpeakerService.class);
				read_test.setAction("org.aprsdroid.app.SPEAKER_TEST");
				startService(read_test);
				return true;
			}
		});
	}

}
