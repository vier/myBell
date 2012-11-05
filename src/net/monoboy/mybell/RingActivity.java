package net.monoboy.mybell;

import java.lang.reflect.Method;
import java.util.List;

import com.android.internal.telephony.ITelephony;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class RingActivity extends Activity {

    private String incomingPhoneNumber = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d("vier", "onCreate");
	super.onCreate(savedInstanceState);
	getWindow().addFlags(
		WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
			| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
	getWindow().setFormat(PixelFormat.TRANSPARENT);
	setContentView(R.layout.ring_main);

	Bundle extras = getIntent().getExtras();
	if (extras != null) {
	    incomingPhoneNumber = extras.getString("incoming");
	    if (extras.getBoolean("KILL_ACT")) {
		close();
		Log.d("vier", "onCreate  KILL_ACT " + extras.getBoolean("KILL_ACT"));
		getIntent().putExtra("KILL_ACT", false);
		if (extras.getBoolean("GOTO_MAIN")) {
		    getIntent().putExtra("GOTO_MAIN", false);
		    Intent mainIntent = new Intent(this, MainActivity.class);
		    startActivity(mainIntent);
		} else {
		    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
		    homeIntent.addCategory(Intent.CATEGORY_HOME);
		    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    startActivity(homeIntent);
		}
	    }
	}

	if (incomingPhoneNumber != null) {
	    TextView incomingInfo = (TextView) findViewById(R.id.footer);
	    incomingInfo.setText("Incoming : [ " + findContactByNumber(RingActivity.this, incomingPhoneNumber) + " ] " + incomingPhoneNumber);
	}

	VideoView myVideoView = (VideoView) findViewById(R.id.myvideoview);

	SharedPreferences prefs = getSharedPreferences("myBell", MODE_PRIVATE);
	String videoPath = prefs.getString("myBellVideo", null);

	if (videoPath == null) {
	    Uri defaultVideofile = Uri.parse("android.resource://" + this.getPackageName() + "/raw/a");
	    myVideoView.setVideoURI(defaultVideofile);
	} else {
	    myVideoView.setVideoPath(videoPath);
	}

	myVideoView.setMediaController(new MediaController(this));

	myVideoView.setOnPreparedListener(new OnPreparedListener() {
	    @Override
	    public void onPrepared(MediaPlayer mp) {
		mp.setLooping(true);
	    }
	});

	myVideoView.requestFocus();
	myVideoView.start();
	Log.d("vier", "onCreate");
    }

    @Override
    protected void onNewIntent(Intent intent) {
	super.onNewIntent(intent);
	boolean isKill = intent.getBooleanExtra("KILL_ACT", false);
	if (isKill) {
	    close();
	}
    }

    private void close() {
	finish();

	Intent homeIntent = new Intent(Intent.ACTION_MAIN);
	homeIntent.addCategory(Intent.CATEGORY_HOME);
	homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	startActivity(homeIntent);

	new Thread(new Runnable() {
	    @SuppressWarnings("deprecation")
	    public void run() {
		ActivityManager actMng = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		String strProcessName = getApplicationInfo().processName;
		while (true) {
		    List<RunningAppProcessInfo> list = actMng.getRunningAppProcesses();
		    for (RunningAppProcessInfo rap : list) {
			if (rap.processName.equals(strProcessName)) {
			    if (rap.importance >= RunningAppProcessInfo.IMPORTANCE_BACKGROUND)
				actMng.restartPackage(getPackageName());
			    Thread.yield();
			    break;
			}
		    }
		}
	    }
	}, "Process Killer").start();
    }

    public Long fetchContactIdFromPhoneNumber(String phoneNumber) {
	Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
	Cursor cursor = this.getContentResolver().query(uri, new String[] { PhoneLookup.DISPLAY_NAME, PhoneLookup._ID }, null, null, null);

	String contactId = "";
	if (cursor.moveToFirst()) {
	    do {
		contactId = cursor.getString(cursor.getColumnIndex(PhoneLookup._ID));
	    } while (cursor.moveToNext());
	}
	if (contactId == "" || contactId == null) {
	    return null;
	} else {
	    return Long.parseLong(contactId);
	}
    }

    private String findContactByNumber(Context ctx, String phoneNumber) {
	ContentResolver resolver = ctx.getContentResolver();
	Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
	String[] phoneNoProjections = { PhoneLookup._ID, PhoneLookup.DISPLAY_NAME };

	Cursor cursor = resolver.query(lookupUri, phoneNoProjections, null, null, null);
	try {
	    if (cursor.moveToFirst()) {
		return cursor.getString(1);
	    }
	} finally {
	    if (cursor != null) {
		cursor.close();
	    }
	}
	return null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
	showDailog();
	return true;
    }

    @Override
    public void onBackPressed() {
	showDailog();
    }

    public Uri getPhotoUri(long contactId) {
	ContentResolver contentResolver = getContentResolver();
	try {
	    Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, ContactsContract.Data.CONTACT_ID + "=" + contactId + " AND "
		    + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'", null, null);
	    if (cursor != null) {
		if (!cursor.moveToFirst()) {
		    return null; // no photo
		}
	    } else {
		return null; // error in cursor process
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}

	Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
	return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    private void showDailog() {
	AlertDialog.Builder alertBuilder = new AlertDialog.Builder(RingActivity.this);

	LayoutInflater inflater = (LayoutInflater) RingActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
	View layout = inflater.inflate(R.layout.custom_dialog, null);

	ImageView contactImageView = (ImageView) layout.findViewById(R.id.contact_photo);
	TextView callInfo = (TextView) layout.findViewById(R.id.contact_name);

	Long contactId = fetchContactIdFromPhoneNumber(incomingPhoneNumber);
	Uri contactUri = null;
	if (contactId != null) {
	    contactUri = getPhotoUri(contactId);
	}
	if (contactId != null && contactUri != null) {
	    contactImageView.setImageURI(contactUri);

	    callInfo.setText(findContactByNumber(RingActivity.this, incomingPhoneNumber) + "\n" + incomingPhoneNumber);
	} else {
	    contactImageView.setVisibility(View.GONE);
	    callInfo.setText(incomingPhoneNumber);
	}

	alertBuilder.setView(layout);
	alertBuilder.setMessage("Incoming Call Info");
	alertBuilder.setPositiveButton("전화받기", new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialog, int which) {
		Intent answer = new Intent(Intent.ACTION_MEDIA_BUTTON);
		answer.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
		sendOrderedBroadcast(answer, null);

		close();
	    }
	});
	alertBuilder.setNegativeButton("전화끊기", new DialogInterface.OnClickListener() {
	    @SuppressWarnings({ "unchecked", "rawtypes" })
	    public void onClick(DialogInterface dialog, int which) {
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		try {
		    Class c = Class.forName(tm.getClass().getName());
		    Method m = c.getDeclaredMethod("getITelephony");
		    m.setAccessible(true);
		    ITelephony telephonyService = (ITelephony) m.invoke(tm);
		    telephonyService.endCall();

		} catch (Exception e) {
		    // TODO: handle exception
		}
		close();
	    }
	});

	alertBuilder.setCancelable(false);
	alertBuilder.create();
	alertBuilder.show();

    }
}
