package org.aprsdroid.speaker;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SpeakerService extends Service
		implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener {
	static final String TAG = "SpeakerService";

	static final long POSITION_THRESHOLD = 5*60*1000; // 5 minutes in milliseconds between repeating a callsign
	TextToSpeech tts;

	SharedPreferences prefs;

	private String[] read_nato_alphabet;
	private String[] read_bearing;

	boolean initialized;
	boolean aprsdroid_running = false;
	String user_callsign;
	Location user_location;
	HashMap<String, Long> positionTimestamps = new HashMap<>();

	String text;
	int last_text_id = 0;

	Map<String,Integer> last_heard = new HashMap();

	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		read_nato_alphabet = getResources().getStringArray(R.array.read_nato_alphabet);
		read_bearing = getResources().getStringArray(R.array.read_bearing);

		tts = new TextToSpeech(this, this);
		tts.setOnUtteranceCompletedListener(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().startsWith("org.aprsdroid.app")) {
			// strip bare action name from event
			readAloudIntent(intent);
		}
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		tts.stop();
		tts.shutdown();
		Log.d(TAG, "Stopping Speaker.");
	}

	@Override
	public void onInit(int i) {
		if (i == TextToSpeech.SUCCESS) {
			int result = tts.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
				initialized = true;
				if (text != null)
					readAloudText();
			}
		}
		if (!initialized) {
			Toast.makeText(this, R.string.error_tts_load, Toast.LENGTH_SHORT).show();
			stopSelf();
		}
	}
	@Override
	public void onUtteranceCompleted(String s) {
		Log.d(TAG, "reading finished " + s);
		if (Integer.parseInt(s) >= last_text_id && !aprsdroid_running)
			stopSelf();
	}

	public void readAloudIntent(Intent intent) {
		String a = intent.getAction().replace("org.aprsdroid.app.", "");
		switch (a) {
			case "SPEAKER_TEST":
				addText("Greetings from " + call2nato("DO1GL"));
				break;
			case "SERVICE_STARTED":
				user_callsign = intent.getStringExtra("callsign");
				if (prefs.getBoolean("events", true) && !aprsdroid_running)
					addText(getString(R.string.read_started));
				aprsdroid_running = true;
				positionTimestamps.clear();
				break;
			case "SERVICE_STOPPED":
				if (prefs.getBoolean("events", true))
					addText(getString(R.string.read_stopped));
				aprsdroid_running = false;
				break;
			case "LINK_OFF":
				if (prefs.getBoolean("events", true))
					addText(getString(R.string.read_linkoff));
				break;
			case "LINK_ON":
				if (prefs.getBoolean("events", true))
					addText(getString(R.string.read_linkon));
				break;
			case "MESSAGE":
				String source = intent.getStringExtra("source");
				String message = intent.getStringExtra("body");
				if (prefs.getBoolean("messages", true))
					addText(String.format(getString(R.string.read_message), call2nato(source), message));
				break;
			case "POSITION":
				String callsign = intent.getStringExtra("callsign");
				Location location = intent.getParcelableExtra("location");
				if (callsign.equals(user_callsign)) {
					user_location = location;
					if (prefs.getBoolean("own_positions", true))
						addText(getString(R.string.read_txpos));
				} else readAloudPosition(callsign, location);
				break;
		}
	}

	private void readAloudPosition(String callsign, Location l) {
		Long last = positionTimestamps.get(callsign);
		if (last != null && last + POSITION_THRESHOLD > System.currentTimeMillis())
			return;
		if (!prefs.getBoolean("positions", true))
			return;

		if (user_location != null) {
			float dist = user_location.distanceTo(l)/1000;
			float bearing = user_location.bearingTo(l);
			int bearing_idx = ((int)(bearing + 22 + 720) % 360) / 45;
			String text = String.format(getString(R.string.read_position), call2nato(callsign), dist, read_bearing[bearing_idx]);
			Log.d(TAG, text);
			addText(text);
		} else
			addText(call2nato(callsign));
		positionTimestamps.put(callsign, System.currentTimeMillis());
	}

	private String call2nato(String call) {
		if (!prefs.getBoolean("nato", true))
			return call.replace("0", "-zero-");
		StringBuilder sb = new StringBuilder();
		for (char c : call.toUpperCase().toCharArray()) {
			if (c >='A' && c <= 'Z') {
				sb.append(read_nato_alphabet[(int)(c - 'A')]);
			} else sb.append(c);
			sb.append(" ");
		}
		return sb.toString();
	}

	synchronized private void addText(String new_text) {
		if (text != null)
			text = String.format("%s\n%s", text, new_text);
		else text = new_text;

		if (initialized)
			readAloudText();
	}

	synchronized private void readAloudText() {
		last_text_id += 1;
		HashMap<String, String> tts_options = new HashMap();
		tts_options.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "" + last_text_id);
		Log.d(TAG, "reading aloud " + last_text_id + ": " + text);
		tts.speak(text, TextToSpeech.QUEUE_ADD, tts_options);
		text = null;
	}

}
