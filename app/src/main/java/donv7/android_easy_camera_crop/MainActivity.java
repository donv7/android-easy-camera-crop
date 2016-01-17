package donv7.android_easy_camera_crop;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    // upload your photo
    private static Uri mImageUri;
    private Bitmap mImageToUpload;
    private final int ACTION_REQUEST_GALLERY = 0;
    private final int ACTION_REQUEST_CAMERA = 1;
    private final int CROP_INTENT_RESULT_CODE = 2;
    private boolean mImageChosen;

    private ImageView testImageView;

    // region CTOR
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.testButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Log.d("MainActivity", "testButton clicked!");

                //Add camera stuff here
                testCameraCrop();
            }
        });

        testImageView = (ImageView) findViewById(R.id.testImageView);
    }
    // endregion

    // region TEST CAMERA CROP
    public void testCameraCrop() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Title");
        builder.setItems(new CharSequence[]{"Gallery", "Camera"},
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:

                                // Get the image from gallery
                                Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                intent.setType("image/*");
                                startActivityForResult(intent, ACTION_REQUEST_GALLERY);

                                break;

                            case 1:

                                //take a picture with the camera
                                Intent getCameraImage = new Intent("android.media.action.IMAGE_CAPTURE");

                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
                                String timeStamp = dateFormat.format(new Date());
                                String imageFileName = "full_" + timeStamp + ".jpg";

                                // get the path to save the file
                                File path = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                                File photo = new File(path, imageFileName);
                                getCameraImage.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
                                mImageUri = Uri.fromFile(photo);

                                startActivityForResult(getCameraImage, ACTION_REQUEST_CAMERA);

                                break;

                            default:
                                break;
                        }
                    }
                });

        builder.show();

    }
    // endregion

    // region ON ACTIVITY RESULT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // check if the result succeeded
        if (resultCode == RESULT_OK) {
            // get the timestamp for this action
            String timeStamp = (new SimpleDateFormat("yyyyMMdd'T'HHmmss")).format(new Date());

            // create the file name of the image that will be cropped
            String imageFileName = "crop_" + timeStamp + ".jpg";

            // get the path to save the file
            File path = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            // create the actual file in the FS of the device
            File croppedPhotoFile = new File(path, imageFileName);

            // cache the resource identifier of this image so we can access it later
            Uri croppedPhotoUri = Uri.fromFile(croppedPhotoFile);

            // determine where the picture will be/is coming from
            if(requestCode == ACTION_REQUEST_GALLERY || requestCode == ACTION_REQUEST_CAMERA) {
                // determine the appropriate resource id of the image to be cropped
                mImageUri = requestCode == ACTION_REQUEST_GALLERY
                        ? data.getData()
                        : mImageUri;

                try {
                    // generate the Intent to crop the pic and temporarily save it on the device
                    Intent cropIntent = new Intent("com.android.camera.action.CROP");
                    cropIntent.setDataAndType(mImageUri, "image/*");
                    cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, croppedPhotoUri);
                    cropIntent.putExtra("crop", "true");
                    cropIntent.putExtra("aspectX", 1);
                    cropIntent.putExtra("aspectY", 1);
                    cropIntent.putExtra("outputX", 256);
                    cropIntent.putExtra("outputY", 256);
                    cropIntent.putExtra("return-data", true);
                    startActivityForResult(cropIntent, CROP_INTENT_RESULT_CODE);
                } catch (ActivityNotFoundException anfe) {
                    // the device does not support cropping...attempt to auto crop the image...
                    Log.e("MainActivity", "anfe", anfe);
                }

            } else if(requestCode == CROP_INTENT_RESULT_CODE) {
                // setup vars
                Bitmap displayImage = null;

                // attempt to get the image from the intent extras
                try {
                    displayImage = (Bitmap) data.getExtras().get("data");
                } catch(NullPointerException npe) {
                    displayImage = null;
                    Log.e("MainActivity", "npe", npe);

                }

                // if we have a valid display image, use it
                if(displayImage == null) {
                    // attempt to get the image to upload from the data
                    try {
                        // this is the image that we will send in the post request
                        displayImage = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), data.getData());
                    } catch (IOException e) {
                        Log.e("MainActivity", "ioe", e);
                    } catch (NullPointerException npe) {
                        Log.e("MainActivity", "npe", npe);
                    }
                }

                mImageToUpload = displayImage;

                if(displayImage != null) {
                    // scale the display image
                    displayImage = Bitmap.createScaledBitmap(displayImage,
                            convertDpsToPixels(MainActivity.this, 50),
                            convertDpsToPixels(MainActivity.this, 50), false);
                    // set it in the UI
                    testImageView.setImageBitmap(displayImage);

                    mImageChosen = true;
                }

            } else {
                Log.e("MainActivity", "idk i think something went wrong maybe, whatever");
            }
        } else {
            Log.d("CreateAccount", String.valueOf(resultCode));
        }
    }
    // endregion

    private static int convertDpsToPixels(Context ctx, int dps) {
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

}
