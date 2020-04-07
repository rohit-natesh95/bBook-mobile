package roru.bbookmobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    final String TAG = "MainActivity";
    final int READ_REQUEST_CODE = 42;
    final int READ_FAILURE = 0;


    Button fromCamera;
    Button fromDoc;
    TextToSpeech textToSpeech;
    boolean docTalk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSpeech();
        setContentView(R.layout.activity_main);
        fromCamera = findViewById(R.id.fromCamera);
        fromDoc = findViewById(R.id.fromDoc);
    }

    @Override
    public void onResume() {
        super.onResume();
//      TODO: uncomment the following
//        speakVoice("b Book app home screen opened.",0);
//        speakVoice("Click upper half to choose document or " +
//                "click lower half to scan document.",1);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!docTalk)
            speakVoice("", 0);
        else
            docTalk = false;
    }

    /*
     *@param status = 0 for QUEUE_FLUSH
     * @param status = 1 for QUEUE_ADD
     */
    public void cameraMode(View view) {
        speakVoice("Scanning mode.", 0);

    startActivity(new Intent(this, ocrClass.class));

    }

    public void docMode(View view) {
        docTalk = true;
        speakVoice("Reading from document.", 0);
        speakVoice("Please select a document. " +
                "Use assistance feature to help.", 1);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        if (READ_REQUEST_CODE == requestCode && READ_FAILURE != resultCode) {
            Log.i("pdf result inside", ":" + resultCode);
            Uri uri = resultIntent.getData();
            String filePath = RealPathUtil.getPath(this, uri);
            getPdfContent(filePath);
        }
    }

    void getPdfContent(String filePath) {
        try {
            StringBuffer sb = new StringBuffer();
            PdfReader pdfReader = new PdfReader(filePath);
            PdfReaderContentParser pdfReaderContentParser =
                    new PdfReaderContentParser(pdfReader);
            TextExtractionStrategy textExtractionStrategy;

            for (int i = 1; i <= pdfReader.getNumberOfPages(); i++) {

                textExtractionStrategy = pdfReaderContentParser
                        .processContent(i, new SimpleTextExtractionStrategy());
                sb.append(textExtractionStrategy.getResultantText());
            }
            sb.append('.');

            pdfReader.close();
            NearbySender nearbySender = new NearbySender(getApplicationContext());
            nearbySender.sendData(sb.toString().trim(), 0);


            speakVoice("Document is being sent to b book.", 0);
            return;

        } catch (Exception | NoClassDefFoundError e) {
            Log.i(TAG, "PDF content: Some error has occurred.");
        }
        speakVoice("Could not select file", 0);
        Toast.makeText(this, "Could not select file.", Toast.LENGTH_SHORT).show();
    }

//    public void toWifi(View view) {
//        startActivity(new Intent(this, sendWifi.class));
//    }


    void initSpeech() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (TextToSpeech.SUCCESS == status) {
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.setPitch(1.2f);
                    textToSpeech.setSpeechRate(0.9f);
                    Log.i("Pdf inside", "speech");
                    speakVoice("b Book app home screen opened.", 0);
                    speakVoice("Click upper half to choose document or " +
                            "click lower half to scan document.", 1);
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
