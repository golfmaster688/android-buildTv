package com.mediabox.tv;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(32f);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText(getString(R.string.build_info, Build.VERSION.RELEASE, Build.MODEL));
        subtitle.setTextSize(16f);
        subtitle.setGravity(Gravity.CENTER);

        root.addView(title);
        root.addView(subtitle);
        setContentView(root);
    }
}
