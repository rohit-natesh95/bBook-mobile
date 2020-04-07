package roru.bbookmobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import java.util.List;
import java.util.Locale;


public class ocrClass extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    final String TAG = "ocrClass";
    Button button;
    ImageView imageView;
    Bitmap imageBitmap;
    TextToSpeech textToSpeech;
    Task<FirebaseVisionText> result;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ocr_layout);
        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.button_image);
        dispatchTakePictureIntent();
        initSpeech();
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
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }
    }

    public void process(View view) {
        button.setEnabled(false);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);

        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setWidth(480)   // 480x360 is typically sufficient for
                .setHeight(360)  // image recognition
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(0)
                .build();

        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        Log.i(TAG, "onSuccess: +++++++++++++++++++++++++++++++++++++++++++");
                        handleData(firebaseVisionText);

                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                speakVoice("No text detected. Try again.",0);

                                Log.i(TAG, "onFailure: ---------------------------------");
                                // Task failed with an exception
                                // ...
                            }
                        });


        //getting data


//        for (FirebaseVisionText.TextBlock block: result.getTextBlocks()) {
//            String blockText = block.getText();
//            Float blockConfidence = block.getConfidence();
//            for (FirebaseVisionText.Line line: block.getLines()) {
//                String lineText = line.getText();
//                Float lineConfidence = line.getConfidence();
//
//                for (FirebaseVisionText.Element element: line.getElements()) {
//                    String elementText = element.getText();
//                    Float elementConfidence = element.getConfidence();
//
//                }
//            }
//        }

    }

    void handleData(FirebaseVisionText result) {

        String resultText = result.getText();
//        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
//            String blockText = block.getText();
//            for (FirebaseVisionText.Line line : block.getLines()) {
//                String lineText = line.getText();
//                for (FirebaseVisionText.Element element : line.getElements()) {
//                    String elementText = element.getText();
//                    Log.i(TAG, "handleData: ------------------------------- elemetn values"+elementText);
//
//                }
//            }
//        }
        if(resultText.length()>0)
            speakVoice("Text detected and will be sent to bBook",0);
        else
            speakVoice("No text detected. Try again.",0);
        Log.i(TAG, "process: --------------------------------------------------------- Result is = " + resultText);
        NearbySender nearbySender = new NearbySender(getApplicationContext());
        nearbySender.sendData(resultText,0);
    }

    void initSpeech() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (TextToSpeech.SUCCESS == status) {
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.setPitch(1.2f);
                    textToSpeech.setSpeechRate(0.9f);
                    speakVoice("Please take picture of text. Use assistance if needed.", 1);

                }
            }
        });
    }

    void speakVoice(String str, int status) {
        if (status == 0)
            textToSpeech.speak(str, TextToSpeech.QUEUE_FLUSH, null, "0");
        else
            textToSpeech.speak(str, TextToSpeech.QUEUE_ADD, null, "0");
    }


}
