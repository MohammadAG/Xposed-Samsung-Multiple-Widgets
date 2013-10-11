package com.mohammadag.multiplewidgets;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

@SuppressLint("WorldReadableFiles")
public class SettingsActivity extends PreferenceActivity {
	
	@SuppressWarnings("deprecation")
	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return super.getSharedPreferences(name, MODE_WORLD_READABLE);
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == android.R.id.home)
			finish();
		return super.onMenuItemSelected(featureId, item);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.pref_general);
	}
}
