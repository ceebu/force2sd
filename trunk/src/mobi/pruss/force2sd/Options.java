package mobi.pruss.force2sd;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Options extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String PREF_CHECK_ORPHANS = "checkOrphans";
	public static final String PREF_METHOD = "method";
	public static final int METHOD_OLD = 0;
	public static final int METHOD_NEW = 1;
	
	private static String[] summaryKeys = { PREF_METHOD };
	private static int[] summaryEntryValues = { R.array.methods };
	private static int[] summaryEntries = { R.array.method_labels};
	private static String[] summaryDefaults = { "1" };
	
	public static String getString(SharedPreferences options, String key) {
		for (int i=0; i<summaryKeys.length; i++)
			if (summaryKeys[i].equals(key)) 
				return options.getString(key, summaryDefaults[i]);
		
		return options.getString(key, "");
	}
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		addPreferencesFromResource(R.xml.options);
	}

	@Override
	public void onResume() {
		super.onResume();

		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		setSummaries();
	}
	
	public void setSummaries() {
		for (int i=0; i<summaryKeys.length; i++) {
			setSummary(i);
		}
	}
	
	public void setSummary(String key) {		
		for (int i=0; i<summaryKeys.length; i++) {
			if (summaryKeys[i].equals(key)) {
				setSummary(i);
				return;
			}
		}
	}
	
	public void setSummary(int i) {
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();
		
		Preference pref = findPreference(summaryKeys[i]);
		String value = options.getString(summaryKeys[i], summaryDefaults[i]);
		
		String[] valueArray = res.getStringArray(summaryEntryValues[i]);
		String[] entryArray = res.getStringArray(summaryEntries[i]);
		
		for (int j=0; j<valueArray.length; j++) {
			if (valueArray[j].equals(value)) {
				pref.setSummary(entryArray[j]);
				return;
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences options, String key) {
		setSummary(key);
	}

	public static int getMethod(SharedPreferences options) {
		String s = options.getString(Options.PREF_METHOD, "1");
		return Integer.parseInt(s);
	}
}
