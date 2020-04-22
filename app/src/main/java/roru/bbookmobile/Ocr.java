package roru.bbookmobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.Objects;


public class Ocr extends Activity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private final String TAG = "ocrClass";
    private Button button;
    private ImageView imageView;
    private Bitmap imageBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ocr_layout);
        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.button_image);
        dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) Objects.requireNonNull(extras).get("data");
            imageView.setImageBitmap(imageBitmap);
        } else {
            finish();
        }
    }

    public void process(View view) {
        button.setEnabled(false);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        detector.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                handleData(firebaseVisionText);
                            }
                        }
                )
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(),
                                        "Text could not be extracted. Try again!",
                                        Toast.LENGTH_SHORT).show();
                                Log.i(TAG, "Failed to extract text");
                            }
                        }
                );
    }

    private void handleData(FirebaseVisionText result) {
        String resultText = result.getText();
        Log.i(TAG, "The extracted text = " + resultText);
        if (resultText.length() > 0) {
            Toast.makeText(getApplicationContext(),
                    "Text extracted and will be sent to device.",
                    Toast.LENGTH_SHORT).show();
            NearbySender nearbySender = new NearbySender(getApplicationContext());
            nearbySender.sendData(resultText);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Text could not be extracted. Try again!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
