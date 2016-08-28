package kr.laruyan.floatingyoutubeembed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends Activity {
    public static final boolean isDebug= true;

    private EditText editText_Address = null;
    private Button button_Open = null;
    //private WebView webView_Testing = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText_Address = (EditText)findViewById(R.id.editText_address);
        button_Open = (Button)findViewById(R.id.button_open);
        //webView_Testing = (WebView)findViewById(R.id.webView_Testing);


        button_Open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String strAddress = ShowFloatingYouTubeEmbedService.parseYouTubeGetEmbedUrl("" + editText_Address.getText());
                if(strAddress == null){
                    //no valid youtube address.. skipping
                    return ;
                }

                //webView_Testing.setWebChromeClient(new WebChromeClient());
                //webView_Testing.getSettings().setJavaScriptEnabled(true);
                //webView_Testing.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                //webView_Testing.loadUrl(strAddress);
                button_Open.post(new Runnable() {
                    @Override
                    public void run() {
                        if (ShowFloatingYouTubeEmbedService.instance != null) {
                            ShowFloatingYouTubeEmbedService.instance.attachYouTubeWindow(strAddress);
                        }else {
                            if(isDebug){
                                System.out.println("Waiting for service ready");
                            }
                            startService(new Intent(getApplicationContext(), ShowFloatingYouTubeEmbedService.class));
                            button_Open.postDelayed(this, ShowFloatingYouTubeEmbedService.DELAY_DEFAULT_UI_DELAY);
                        }
                    }
                });
                editText_Address.setText("");
            }
        });




    }
}
