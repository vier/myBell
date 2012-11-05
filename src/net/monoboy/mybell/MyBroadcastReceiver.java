package net.monoboy.mybell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "MyBell";
    
    private Context mContext;
    private Intent controlVideoIntent;
    private String incomingPhoneNumber;

    @Override
    public void onReceive(Context context, Intent intent) {
	Log.i(TAG, "MyBroadcastReceiver->onReceive();");
	
	TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	MyPhoneStateListener phoneListener = new MyPhoneStateListener();
	telephony.listen(phoneListener, PhoneStateListener.LISTEN_SERVICE_STATE);
	telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);

	incomingPhoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
	
	mContext = context.getApplicationContext();
	controlVideoIntent = new Intent(context, RingActivity.class);
	controlVideoIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	
	Thread playVideo = new Thread() {
	    public void run() {
		try {
		    sleep(1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		} finally {
		    controlVideoIntent.putExtra("KILL_ACT", false);
		    controlVideoIntent.putExtra("incoming", incomingPhoneNumber);
		    mContext.startActivity(controlVideoIntent);
		}
	    }
	};
	
	Thread stopVideo = new Thread() {
	    public void run() {
		try {
		    sleep(1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		} finally {
		    controlVideoIntent.putExtra("KILL_ACT", true);
		    mContext.startActivity(controlVideoIntent);
		}
	    }
	};

	Log.d("vier", "state : " + telephony.getCallState());
	if (telephony.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
	    playVideo.start();
	} else if (telephony.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK && telephony.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
	    stopVideo.start();
	}

    }

}
