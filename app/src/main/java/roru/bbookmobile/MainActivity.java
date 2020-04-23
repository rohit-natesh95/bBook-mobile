package roru.bbookmobile;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private final int READ_REQUEST_CODE = 42;
    Button fromCamera;
    Button fromDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fromCamera = findViewById(R.id.fromCamera);
        fromDoc = findViewById(R.id.fromDoc);
        AccessibilityManager accessibilityManager = (AccessibilityManager) getApplicationContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (!Objects.requireNonNull(accessibilityManager).isTouchExplorationEnabled()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Enable accessibility?")
                    .setMessage("Enabling accessibility can assist you in using bBook Mobile.")
                    .setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivityForResult(
                                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                                    0);
                        }
                    }
            ).show();
        }
    }

    public void cameraMode(View view) {
        startActivity(new Intent(getApplicationContext(), Ocr.class));
    }

    public void docMode(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        int READ_FAILURE = 0;
        if (READ_REQUEST_CODE == requestCode && READ_FAILURE != resultCode) {
            Uri uri = resultIntent.getData();
            String filePath = RealPathUtil.getPath(this, uri);
            String data = getPdfContent(filePath);
            String TAG = "MainActivity";
            Log.i(TAG, "onActivityResult: Data: " + data);
            if (data != null) {
                NearbySender nearbySender = new NearbySender(getApplicationContext());
                nearbySender.sendData(data);
                Toast.makeText(getApplicationContext(),
                        "Document will be sent to device.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getPdfContent(String filePath) {
        try {
            StringBuilder sb = new StringBuilder();
            PdfReader pdfReader = new PdfReader(filePath);
            PdfReaderContentParser pdfReaderContentParser = new PdfReaderContentParser(pdfReader);
            TextExtractionStrategy textExtractionStrategy;
            for (int i = 1; i <= pdfReader.getNumberOfPages(); i++) {
                textExtractionStrategy = pdfReaderContentParser
                        .processContent(i, new SimpleTextExtractionStrategy());
                sb.append(textExtractionStrategy.getResultantText());
            }
            pdfReader.close();
            sb.append('.');
            return sb.toString().trim();
        } catch (Exception | NoClassDefFoundError ignored) {
            Toast.makeText(this, "Could not select file.", Toast.LENGTH_LONG).show();
            return null;
        }
    }
}
