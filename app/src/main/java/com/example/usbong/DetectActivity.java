package com.example.usbong;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.usbong.ml.SoilClassifier;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DetectActivity extends AppCompatActivity {
    private TextView mResultEt;
    private ImageView mPreviewIv;
    private Button detect;
    private BitmapDrawable drawable;
    private Bitmap bitmap;

    //Permission Code
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 400;
    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 2001;

    private String cameraPermission[];
    private String storagePermission[];
    private Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle("Click + button to insert image");

        mResultEt   = (TextView) findViewById(R.id.resultEt);
        mPreviewIv  = (ImageView) findViewById(R.id.imageIv);
        detect = (Button) findViewById(R.id.detect);

        //camera permission
        cameraPermission = new String[] {Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //storage permission
        storagePermission = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};


        if(!Python.isStarted())
            Python.start(new AndroidPlatform( this));

        Python py = Python.getInstance();
        final PyObject pyobj = py.getModule("main");



        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawable = (BitmapDrawable) mPreviewIv.getDrawable();
                bitmap = drawable.getBitmap();
                String imgStr = getStringImage(bitmap);
//                String imagepath = image_uri.getPath();   //Originally commented out

//                PyObject obj = pyobj.callAttr("treshcolor",imgStr);
//                String result = obj.toString();

                PyObject obj = pyobj.callAttr("decodeImage",imgStr);
                String str = obj.toString();

                byte data[]=android.util.Base64.decode(str, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(data,0, data.length);

                //Classifying image using tflite model
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmp, 224, 224, true);

                try {
                    SoilClassifier model= SoilClassifier.newInstance(getApplicationContext());

//                    // Creates inputs for reference.
//                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
//
                    ByteBuffer input = ByteBuffer.allocateDirect(224 * 224 * 3 * 4).order(ByteOrder.nativeOrder());
//                    for (int y = 0; y < 224; y++) {
//                        for (int x = 0; x < 224; x++) {
//                            int px = scaledBitmap.getPixel(x, y);
//
//                            // Get channel values from the pixel value.
//                            int r = Color.red(px);
//                            int g = Color.green(px);
//                            int b = Color.blue(px);
//
//                            // Normalize channel values to [-1.0, 1.0]. This requirement depends
//                            // on the model. For example, some models might require values to be
//                            // normalized to the range [0.0, 1.0] instead.
//                            float rf = (r - 127) / 255.0f;
//                            float gf = (g - 127) / 255.0f;
//                            float bf = (b - 127) / 255.0f;
//
//                            input.putFloat(rf);
//                            input.putFloat(gf);
//                            input.putFloat(bf);
//                        }
//                    }
//
//                    TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
//                    tensorImage.load(scaledBitmap);
//                    ByteBuffer byteBuffer = input;
//                    inputFeature0.loadBuffer(byteBuffer);

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                    inputFeature0.loadBuffer(input);

                    // Runs model inference and gets result.
                    SoilClassifier.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

//                    // Runs model inference and gets result.
//                    SoilClassifier.Outputs outputs = model.process(inputFeature0);
//                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    // Releases model resources if no longer used.
                    model.close();


                    int result = getMax(outputFeature0.getFloatArray());
                    mResultEt.setText(result);

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
    }

    private int getMax(float[] arr) {
        if (arr.length == 0) {
            return -1;
        }

        float max = arr[0];
        int maxIndex = 0;

        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                maxIndex = i;
                max = arr[i];
            }
        }
        System.out.println("Max value:" + max);
        return maxIndex;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addImage:
                showImageImportDialog();
                break;

            case R.id.about:
                dialogAbout();
                break;
        }
        return true;
    }

    private String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void dialogAbout() {
        new AlertDialog.Builder(this)
                .setTitle("About App")
                .setMessage("This app is made by The group Chichi")
                .setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showImageImportDialog() {
        String[] items = {"Camera", "Gallery"};
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    if (!checkCameraPermission()) {
                        //camera permission not allowed, request it
                        requestCameraPermission();
                    } else {
                        //permission allowed, take picture
                        pickCamera();
                    }
                }

                if (which == 1) {
                    if (!checkStoragePermission()) {
                        //storage permission not allowed, request it
                        requestStoragePermission();
                    } else {
                        //permission allowed, take picture
                        pickGallery();
                    }
                }
            }
        });
        dialog.create().show();
    }

    private void pickGallery() {
        //intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set intent type to image
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickCamera() {
        //intent to take image from camera, it will also be save to storage to get high quality image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "NewPick"); //title of the picture
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image To Text"); //title of the picture
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    //handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && writeStorageAccepted) {
                        pickCamera();
                    } else {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (writeStorageAccepted) {
                        pickGallery();
                    } else {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    //handle image result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //got image from gallery now crop it
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON) //enable image guid lines
                        .start(this);
            }

            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //got image from camera now crop it
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON) //enable image guid lines
                        .start(this);
            }
        }

        //get cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri(); //get image uri
                //set image to image view
                mPreviewIv.setImageURI(resultUri);

                //get drawable bitmap for text recognition
                BitmapDrawable bitmapDrawable = (BitmapDrawable) mPreviewIv.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                if (!recognizer.isOperational()) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                } else {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();
                    //get text from sb until there is no text
                    for (int i = 0; i < items.size(); i++) {
                        TextBlock myItem = items.valueAt(i);
                        sb.append(myItem.getValue());
                        sb.append("\n");
                    }

                    //set text to edit text
                    mResultEt.setText(sb.toString());
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                //if there is any error show it
                Exception error = result.getError();
                Toast.makeText(this, "" + error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}