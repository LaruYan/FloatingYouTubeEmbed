package kr.laruyan.floatingyoutubeembed;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;


public class YouTubePopUpEmbedActivity extends Activity {
    private static final boolean isDebug = MainActivity.isDebug;

    private static final String EXTRA_FINISH = "kr.laruyan.floatingyoutubeembed.youtubepopupembedactivity.finish";

    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_you_tube_pop_up_embed);
        mHandler = new Handler(Looper.getMainLooper());
        Intent intent = getIntent();

        if (intent != null) {
            String action = intent.getAction();
            String type = intent.getType();
            String data = intent.getData()+"";
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if ("text/plain".equals(type)) {
                    String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (sharedText != null) {
                        launchPopupYouTubeEmbed(sharedText);
                    }
                }
            }else if (Intent.ACTION_VIEW.equals(action)){
                //if(isDebug){
                //    System.out.println("VIEW"+data);
                //}
                if(!"null".equals(data)){

                    launchPopupYouTubeEmbed(data);
                }
            }
        }
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            finishAndRemoveTask();
        }else {
            Intent finishIntent = new Intent(this, YouTubePopUpEmbedActivity.class);
            finishIntent.putExtra(EXTRA_FINISH, true);
            finishIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(finishIntent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.getIntent().getExtras() != null && this.getIntent().getBooleanExtra(EXTRA_FINISH, false)) {
            finish();
        }
    }

    private void launchPopupYouTubeEmbed(final String strAddress){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (ShowFloatingYouTubeEmbedService.instance != null) {
                    ShowFloatingYouTubeEmbedService.instance.attachYouTubeWindow(ShowFloatingYouTubeEmbedService.parseYouTubeGetEmbedUrl(strAddress));
                } else {
                    if (isDebug) {
                        System.out.println("Waiting for service ready");
                    }
                    startService(new Intent(getApplicationContext(), ShowFloatingYouTubeEmbedService.class));
                    mHandler.postDelayed(this, ShowFloatingYouTubeEmbedService.DELAY_DEFAULT_UI_DELAY);
                }
            }
        });
    }
}
