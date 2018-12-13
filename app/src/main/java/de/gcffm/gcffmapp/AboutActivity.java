package de.gcffm.gcffmapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final ImageView img = findViewById(R.id.logo);
        img.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://gcffm.de"));
                startActivity(intent);
            }
        });

        final TextView infoText = findViewById(R.id.app_info_text);
        infoText.setMovementMethod(LinkMovementMethod.getInstance());
    }

}
