package com.example.dailyselfie;
import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final long INTERVAL_TWO_MINUTES = 60*1000L;
    public SelfieRecordAdapter mAdapter;
    private String mCurrentSelfieName;
    private String mCurrentPhotoPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView selfieList = (ListView) findViewById(R.id.daily_selfie);
        mAdapter = new SelfieRecordAdapter(getApplicationContext());
        selfieList.setAdapter(mAdapter);
        selfieList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SelfieRecord selfieRecord = (SelfieRecord) mAdapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT, selfieRecord.getPath());
                startActivity(intent);
            }
        });
        createSelfieAlarm();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_camera) {
            dispatchTakePictureIntent();
            return true;
        }
        if (id == R.id.action_delete_selected) {
            deleteSelectedSelfies();
            return true;
        }
        if (id == R.id.action_delete_all) {
            deleteAllSelfies();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private File createImageFile() throws IOException {
        mCurrentSelfieName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        mCurrentSelfieName +=  "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(mCurrentSelfieName, ".jpg", storageDir);
        mCurrentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void dispatchTakePictureIntent() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
        else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                }

                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            File photoFile = new File(mCurrentPhotoPath);
            File selfieFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), mCurrentSelfieName + ".jpg");
            photoFile.renameTo(selfieFile);

            SelfieRecord selfieRecord = new SelfieRecord(Uri.fromFile(selfieFile).getPath(), mCurrentSelfieName);
            Log.d(LOG_TAG, selfieRecord.getPath() + " - " + selfieRecord.getDisplayName());
            mAdapter.add(selfieRecord);
        }
        else {
            File photoFile = new File(mCurrentPhotoPath);
            photoFile.delete();
        }
    }

    private void deleteSelectedSelfies() {
        ArrayList<SelfieRecord> selectedSelfies = mAdapter.getSelectedRecords();
        for (SelfieRecord selfieRecord : selectedSelfies) {
            File selfieFile = new File(selfieRecord.getPath());
            selfieFile.delete();
        }
        mAdapter.clearSelected();
    }

    private void deleteAllSelfies() {
        for (SelfieRecord selfieRecord : mAdapter.getAllRecords()) {
            File selfieFile = new File(selfieRecord.getPath());
            selfieFile.delete();
        }
        mAdapter.clearAll();
    }

    private void createSelfieAlarm() {
        Intent intent = new Intent(this, SelfieNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL_TWO_MINUTES,
                INTERVAL_TWO_MINUTES,
                pendingIntent);
    }
}