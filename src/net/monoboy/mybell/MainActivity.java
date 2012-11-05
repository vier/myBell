package net.monoboy.mybell;

import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {
    private Cursor videocursor;
    private int video_column_index;
    private ListView videolist;
    private int count;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.video_list);
	init_phone_video_grid();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	return true;
    }

    @Override
    public void onBackPressed() {
	Intent homeIntent = new Intent(Intent.ACTION_MAIN);
	homeIntent.addCategory(Intent.CATEGORY_HOME);
	homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	startActivity(homeIntent);
    }

    @SuppressWarnings("deprecation")
    private void init_phone_video_grid() {
	System.gc();
	String[] proj = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.SIZE};
	videocursor = managedQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
	count = videocursor.getCount();
	videolist = (ListView) findViewById(R.id.PhoneVideoList);
	videolist.setAdapter(new VideoAdapter(getApplicationContext()));
	videolist.setOnItemClickListener(videogridlistener);
    }

    private OnItemClickListener videogridlistener = new OnItemClickListener() {
	@SuppressWarnings("rawtypes")
	public void onItemClick(AdapterView parent, View v, int position, long id) {
	    System.gc();
	    video_column_index = videocursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
	    videocursor.moveToPosition(position);
	    String filename = videocursor.getString(video_column_index);
	    Intent intent = new Intent(MainActivity.this, ViewVideo.class);
	    Log.d("vier", "filename  " + filename);
	    intent.putExtra("videofilename", filename);

	    SharedPreferences prefs = getSharedPreferences("myBell", MODE_PRIVATE);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString("myBellVideo", filename);
	    editor.commit();

	    startActivity(intent);
	}
    };

    public class VideoAdapter extends BaseAdapter {
	private Context vContext;

	public VideoAdapter(Context c) {
	    vContext = c;
	}

	public int getCount() {
	    return count;
	}

	public Object getItem(int position) {
	    return position;
	}

	public long getItemId(int position) {
	    return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    System.gc();
	    TextView tv = new TextView(vContext.getApplicationContext());
	    String id = null;
	    if (convertView == null) {
		video_column_index = videocursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
		videocursor.moveToPosition(position);
		id = videocursor.getString(video_column_index);
		video_column_index = videocursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
		videocursor.moveToPosition(position);
		id += " Size(KB):" + videocursor.getString(video_column_index);
		tv.setText(id);
	    } else
		tv = (TextView) convertView;

	    return tv;
	}

    }
}
