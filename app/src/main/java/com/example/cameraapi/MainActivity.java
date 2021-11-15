package com.example.cameraapi;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_PERMISSION_INTERNET = 1;
    static final String URL_IMAGE_POST = "http://159.89.194.114:8000/images/";
    private ImageView ivPhoto;
    private TextView tvBase64OfPhoto;
    private Button btnTakePhoto;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnTakePhoto.setOnClickListener(view -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (ActivityNotFoundException e) {
                // display error state
            }
        });

        ivPhoto = findViewById(R.id.ivPhoto);
        tvBase64OfPhoto = findViewById(R.id.tvBase64OfPhoto);

        btnTakePhoto.setEnabled(false);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
            btnTakePhoto.setEnabled(true);
        }
        else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.INTERNET}, REQUEST_PERMISSION_INTERNET);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_INTERNET) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btnTakePhoto.setEnabled(true);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                ivPhoto.setImageBitmap(imageBitmap);
                tvBase64OfPhoto.setText("Sending image to cloud server...");

                Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                sendImageToCloud(timestamp.getTime() + ".jpg", getBase64OfPhoto(imageBitmap));
            }
        }
    }

    private String getBase64OfPhoto(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void sendImageToCloud(String fileName, String imageData) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_IMAGE_POST,
            response -> {
                tvBase64OfPhoto.setText("Image has been successfully sent.");
            }, error -> {
                Log.e("IMAGE_SENT", error.toString());
        }) {
            protected Map<String, String> getParams() {
                HashMap<String, String> hashMapParams = new HashMap<>();
                hashMapParams.put("filename", fileName);
                hashMapParams.put("imagedata", imageData);
                hashMapParams.put("password", "123");
                return hashMapParams;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
            0,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(stringRequest);
    }
}