package roru.bbookmobile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    final int READ_REQUEST_CODE = 42;
    final int READ_FAILURE = 0;
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
            Toast.makeText(getApplicationContext(), "Please enable Accessibility for better usage.",
                    Toast.LENGTH_LONG).show();
            startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);
        }
    }

    public void cameraMode(View view) {
        startActivity(new Intent(this, Ocr.class));
    }

    public void docMode(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        if (READ_REQUEST_CODE == requestCode && READ_FAILURE != resultCode) {
            Uri uri = resultIntent.getData();
            String filePath = RealPathUtil.getPath(this, uri);
            getPdfContent(filePath);
        }
    }

    void getPdfContent(String filePath) {
        try {
            StringBuffer sb = new StringBuffer();
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
            NearbySender nearbySender = new NearbySender(getApplicationContext());
            nearbySender.sendData(sb.toString().trim(), 0);
        } catch (Exception | NoClassDefFoundError ignored) {
            Toast.makeText(this, "Could not select file.", Toast.LENGTH_SHORT).show();
        }
    }
}
