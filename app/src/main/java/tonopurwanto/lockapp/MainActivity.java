package tonopurwanto.lockapp;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
{
    public static final String EXTRA_FILEPATH = "tonopurwanto.lockapp.EXTRA_FILEPATH";

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private static final String FILE_TAG = "File Creation";

    private Button mTakePicButton;
    private Button mLockButton;
    private ImageView mImageView;
    private String mPhotoPath;
    private int mPermissionCheck;
    private DevicePolicyManager mDevicePolicyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTakePicButton = (Button) findViewById(R.id.pic_button);
        mTakePicButton.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View v)
            {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException e) {
                        Log.e(FILE_TAG, e.getMessage());
                    }

                    if (photoFile != null) {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No camera apps", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mImageView = (ImageView) findViewById(R.id.main_imageView);

        mPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (mPermissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        mLockButton = (Button) findViewById(R.id.start_lock_button);
        mLockButton.setOnClickListener(new View.OnClickListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override public void onClick(View v)
            {
                if (mDevicePolicyManager.isLockTaskPermitted(getApplicationContext().getPackageName())) {
                    Intent lockIntent = new Intent(getApplicationContext(), LockedActivity.class);
                    lockIntent.putExtra(EXTRA_FILEPATH, mPhotoPath);
                    startActivity(lockIntent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "Not whitelisted", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private File createImageFile() throws IOException
    {
        if (mPermissionCheck == PackageManager.PERMISSION_GRANTED) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "JPEG_" + timeStamp + "_";
            File storageDir = getApplicationContext().getExternalFilesDir(null);
            File image = File.createTempFile(fileName, ".jpg", storageDir);

            mPhotoPath = image.getAbsolutePath();

            return image;
        }

        return null;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            setImageToView();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissionCheck = grantResults[0];
                } else {
                    mTakePicButton.setEnabled(false);
                }
        }
    }

    private void setImageToView()
    {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(mPhotoPath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

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
