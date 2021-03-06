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
import android.media.ThumbnailUtils;
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
import com.example.usbong.ml.Model;
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
    
    private int imageSize= 224;

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

                if(drawable == null) {
                    System.out.println("NO OBJECT UPLOADED");
                }

                bitmap = drawable.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);

                String imgStr = getStringImage(bitmap);

                PyObject treshcolor_py = pyobj.callAttr("treshcolor",imgStr);
                String treshResults = treshcolor_py.toString();


                try {
//                    SoilClassifier model= SoilClassifier.newInstance(getApplicationContext());
                    Model model= Model.newInstance(getApplicationContext());

                    ByteBuffer input = ByteBuffer.allocateDirect(imageSize * imageSize * 3 * 4).order(ByteOrder.nativeOrder());

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
                    inputFeature0.loadBuffer(input);

                    // get 1D array of imageSize * imageSize pixels in image
                    int [] intValues = new int[imageSize * imageSize];
                    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                    // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
                    int pixel = 0;
                    for(int i = 0; i < imageSize; i++){
                        for(int j = 0; j < imageSize; j++){
                            int val = intValues[pixel++]; // RGB
                            input.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                            input.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                            input.putFloat((val & 0xFF) * (1.f / 255.f));
                        }
                    }

                    inputFeature0.loadBuffer(input);
                    
                    // Runs model inference and gets result.
//                    SoilClassifier.Outputs outputs = model.process(inputFeature0);
                    Model.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    // Releases model resources if no longer used.
                    model.close();

                    int maxIndex = getMax(outputFeature0.getFloatArray());

                    String resultStr= "";

                    float[] confidences = outputFeature0.getFloatArray();
                    // find the index of the class with the biggest confidence.
                    int maxPos = 0;
                    float maxConfidence = 0;
                    for(int i = 0; i < confidences.length; i++){
                        if(confidences[i] > maxConfidence){
                            maxConfidence = confidences[i];
                            maxPos = i;
                        }
                    }
                    String[] classes = {"Clay", "Loam", "Peaty", "Sandy"};
                    resultStr = "Highest Match: " + classes[maxPos];

                    String s = "";
                    for(int i = 0; i < classes.length; i++){
                        s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
                    }

                    resultStr += "\n\n" + s;

                    mResultEt.setText(resultStr + "\n\n" + treshResults);

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