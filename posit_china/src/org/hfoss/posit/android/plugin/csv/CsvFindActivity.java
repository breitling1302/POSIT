/**
 * 
 */
package org.hfoss.posit.android.plugin.csv;

import org.hfoss.posit.android.api.activity.FindActivity;

import android.os.Bundle;

/**
 * FindActivity subclass for CsvFind plugin.
 */
public class CsvFindActivity extends FindActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		find = new CsvFindFragment();
		super.onCreate(savedInstanceState);
	}
}
