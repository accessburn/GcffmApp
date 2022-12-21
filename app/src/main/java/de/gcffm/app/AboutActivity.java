package de.gcffm.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final ImageView img = findViewById(R.id.logo);
        img.setOnClickListener(v -> {
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://gcffm.de"));
            startActivity(intent);
        });

        final TextView infoText = findViewById(R.id.app_info_text);
        infoText.setMovementMethod(LinkMovementMethod.getInstance());
    }

}
