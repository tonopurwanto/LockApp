package tonopurwanto.lockapp;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class LockedActivity extends AppCompatActivity
{
    private static final String PREFS_FILE_NAME = "PrefsFile";
    private static final String PHOTO_PATH = "Photo Path";

    private ImageView mImageView;
    private Button mUnlockButton;
    private String mPhotoPath;
    private DevicePolicyManager mDevicePolicyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locked);

        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        mUnlockButton = (Button) findViewById(R.id.stop_lock_button);
        mUnlockButton.setOnClickListener(new View.OnClickListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override public void onClick(View v)
            {
                ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_LOCKED) {
                    stopLockTask();
                }

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        setImageToView();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override protected void onStart()
    {
        super.onStart();

        if (mDevicePolicyManager.isLockTaskPermitted(this.getPackageName())) {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        }
    }

    @Override protected void onStop()
    {
        super.onStop();

        SharedPreferences settings = getSharedPreferences(PREFS_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PHOTO_PATH, mPhotoPath);
        editor.commit();
    }

    private void setImageToView()
    {
        SharedPreferences settings = getSharedPreferences(PREFS_FILE_NAME, 0);
        String savedPhotoPath = settings.getString(PHOTO_PATH, null);

        mImageView = (ImageView) findViewById(R.id.lock_imageView);
        Intent intent = getIntent();

        String passedPhotoPath = intent.getStringExtra(MainActivity.EXTRA_FILEPATH);
        if (passedPhotoPath != null) {
            mPhotoPath = passedPhotoPath;
        } else {
            mPhotoPath = savedPhotoPath;
        }

        if (mPhotoPath != null) {
            int targetH = mImageView.getMaxHeight();
            int targetW = mImageView.getMaxWidth();

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mPhotoPath, bmOptions);

            int photoH = bmOptions.outHeight;
            int photoW = bmOptions.outWidth;

            int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;

            Bitmap imageBitmap = BitmapFactory.decodeFile(mPhotoPath, bmOptions);
            mImageView.setImageBitmap(imageBitmap);
        }
    }
}
