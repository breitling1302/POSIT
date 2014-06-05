package org.hfoss.posit.android.functionplugin.twitter;

import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.FindActivityProvider;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

public class LoginTwitterActivity extends OrmLiteBaseActivity<DbManager> {

	public static final String TAG = "LoginTwitterActivity";

	/** Name to store the users access token 
	 * This is returned after the user provides login information to twitter.*/
	private static String PREF_ACCESS_TOKEN = "";
	/** Name to store the users access token secret.
	 * This is returned after the user provides login information to twitter.*/
	private static String PREF_ACCESS_TOKEN_SECRET = "";
	/** Consumer Key generated when you registered your app at https://dev.twitter.com/apps/ 
	 * It is linked to an application created by a dev account. */
	private static final String CONSUMER_KEY = "s47vJySEVLHBnuhXy3yw";
	/** Consumer Secret generated when you registered your app at https://dev.twitter.com/apps/  
	 * It is linked to an application created by a dev account. */
	private static final String CONSUMER_SECRET = "75MMxh4QYKmJnnm1TtR7o1BiJuloDN1DqVmNF3838"; // XXX Encode in your app
	/** The url that Twitter will redirect to after a user log's in - this will be picked up by your app manifest and redirected into this activity 
	 * This is also defined in the <data android:scheme="twitFinds" /> of the intent filter for this activity. */
	private static final String CALLBACK_URL = "twitFinds:///";
	
	
	/** Twitter4j object */
	private Twitter mTwitter;
	/** The request token signifies the unique ID of the request you are sending to twitter  */
	private RequestToken mReqToken;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		//Toast.makeText(this,"On Create Twit Finds Activity Stub", Toast.LENGTH_LONG).show();
		
		// Load the twitter4j helper
		mTwitter = new TwitterFactory().getInstance();
		Log.i(TAG, "Got Twitter4j");
		
		// Tell twitter4j that we want to use it with our app
		mTwitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		
		// Let user login
		loginNewUser();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
	}
	
	private void loginNewUser() {
		try {
			Log.i(TAG, "Request App Authentication");
			mReqToken = mTwitter.getOAuthRequestToken(CALLBACK_URL);

			Log.i(TAG, "Starting Webview to login to twitter");
			WebView twitterSite = new WebView(this);
			twitterSite.loadUrl(mReqToken.getAuthenticationURL());
			twitterSite.requestFocus(View.FOCUS_DOWN);		// This command is used to fix a weird behaviour that does not allow the user to type in the webview. 
			setContentView(twitterSite);

		} catch (TwitterException e) {
			Log.i(TAG, e.getMessage());
			Toast.makeText(this, "Was not able to access Twitter to login.", Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Catch when Twitter redirects back to our {@link CALLBACK_URL}</br> 
	 * We use onNewIntent as in our manifest we have singleInstance="true" if we did not the
	 * getOAuthAccessToken() call would fail
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.i(TAG, "New Intent Arrived");
		parseCallBack(intent);
	}
	
	/**
	 * Twitter has used the callback parameter to send the Access Key back to the application
	 * 
	 * @param intent
	 */
	private void parseCallBack(Intent intent) {
		Uri uri = intent.getData();
		Log.i(TAG, "Parse Call Back: "+uri.toString());
		if (uri != null && uri.toString().startsWith(CALLBACK_URL)) { 
			String oauthVerifier = uri.getQueryParameter("oauth_verifier");
			AccessToken at;
			try {
				at = mTwitter.getOAuthAccessToken(mReqToken, oauthVerifier);
				PREF_ACCESS_TOKEN = at.getToken();
				PREF_ACCESS_TOKEN_SECRET = at.getTokenSecret();
				if(PREF_ACCESS_TOKEN != null && PREF_ACCESS_TOKEN != "" &&
					PREF_ACCESS_TOKEN_SECRET != null && PREF_ACCESS_TOKEN_SECRET != ""){
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);		
					Editor ed = prefs.edit();
					ed.putString("prefaccesstoken", PREF_ACCESS_TOKEN);
					ed.putString("prefaccesstokensecret", PREF_ACCESS_TOKEN_SECRET);
					ed.commit();
				} else {
					throw new TwitterException("Empty Access Token");
				}
			} catch (TwitterException e) {
				Toast.makeText(this, "Twitter auth error x01, try again later", Toast.LENGTH_SHORT).show();
			}
			//TODO: Redirect elsewhere.
			Intent in = new Intent();
			in.setAction(Intent.ACTION_SEND);
			in.setClass(this, FindActivityProvider.getListFindsActivityClass());
			startActivity(in);
		}
	}
}
