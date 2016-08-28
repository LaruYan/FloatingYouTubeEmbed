package kr.laruyan.floatingyoutubeembed;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.SeekBar;

public class ShowFloatingYouTubeEmbedService extends Service {
    private static final boolean isDebug = MainActivity.isDebug;
    public static ShowFloatingYouTubeEmbedService instance;

    private WindowManager windowManager = null;
    private int popupAreaWidth = 0;
    private boolean isLowRamDevice = false;
    private int numberOfOpenPlayers = 0;

    private static final String YOUTUBE_EMBED_ADDRESS = "https://www.youtube.com/embed/";
    private static final long DELAY_DEFAULT_UI_WAIT = 5000;
    public static final long DELAY_DEFAULT_UI_DELAY = 250;

    @Override
    public void onCreate(){
        super.onCreate();
        instance = this;
        if(isDebug){
            System.out.println("onCreate() succeed");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if(isDebug){
            System.out.println("onDestroy() succeed");
        }

    }

    public void attachYouTubeWindow(final String url){
        if(url == null){
            //no valid youtube Address, skipping..
            return ;
        }

        if(windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }

        if(popupAreaWidth <= 0) {
            popupAreaWidth = (int) ((2f / 3f) * calculateSmallestWidth(windowManager));
        }

        final View inflatedView =// new View(this);
                View.inflate(this, R.layout.floating_youtube_embed, null);
        final WebView webView_YouTubeEmbed = (WebView)inflatedView.findViewById(R.id.webView);
        final Button button_Close = (Button)inflatedView.findViewById(R.id.button_close);
        final Button button_Move = (Button)inflatedView.findViewById(R.id.button_move);
        final Button button_Size = (Button)inflatedView.findViewById(R.id.button_size);
        final SeekBar seekBar_Transparency = (SeekBar)inflatedView.findViewById(R.id.seekBar_transparency);
        final View view_blocker = (View)inflatedView.findViewById(R.id.view_blocker);
        final ButtonWaitMemoObject waitMemoObject = new ButtonWaitMemoObject();

        final WindowManager.LayoutParams paramsRL = new WindowManager.LayoutParams(
                //WindowManager.LayoutParams.MATCH_PARENT,
                popupAreaWidth,
                (int) ((((float) popupAreaWidth) / 16f) * 9f),
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        paramsRL.gravity = Gravity.TOP | Gravity.LEFT;

        //webView_YouTubeEmbed.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        webView_YouTubeEmbed.setWebChromeClient(new WebChromeClient());
        webView_YouTubeEmbed.getSettings().setJavaScriptEnabled(true);
        webView_YouTubeEmbed.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                webView_YouTubeEmbed.setWebContentsDebuggingEnabled(true);
            }
        }
/*
        webView_YouTubeEmbed.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                //Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
            }
        });/**/

        if(isLowRamDevice) {
            webView_YouTubeEmbed.loadUrl(url+"?rel=0&vq=small");
        }else{
            webView_YouTubeEmbed.loadUrl(url);
            //String customHtml = "<html><head><title>Sample</title></head><body><video controls><source src='file://storage/emulated/0/erindockclock/ef%20game%20ntop.mp4'></video></body></html>";
            //webView_YouTubeEmbed.loadData(customHtml, "text/html", "UTF-8");
        }


        webView_YouTubeEmbed.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        waitMemoObject.isTouchingWebView = true;
                    case MotionEvent.ACTION_UP:
                        waitMemoObject.isTouchingWebView = false;
                        break;
                    default:
                        waitMemoObject.isTouchingWebView = false;
                        break;
                }
                return false;
            }
        });
       /* webView_YouTubeEmbed.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (isDebug) {
                    System.out.println("WebView has Focus:" + hasFocus);
                }
                waitMemoObject.isTouchingWebView = hasFocus;
            }
        });*/

        button_Close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //waitMemoObject.isTouchingCloseButton = true;
                windowManager.removeView(inflatedView);
                webView_YouTubeEmbed.loadUrl("about:blank");
                numberOfOpenPlayers--;
                if(numberOfOpenPlayers<1){
                    stopSelf();
                }
            }
        });
        button_Close.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //TODO: launch YouTube App
                //waitMemoObject.isTouchingCloseButton = true;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                windowManager.removeView(inflatedView);
                webView_YouTubeEmbed.loadUrl("about:blank");
                numberOfOpenPlayers--;
                if(numberOfOpenPlayers<1){
                    stopSelf();
                }
                return false;
            }
        });
        button_Move.setOnTouchListener(new View.OnTouchListener() {
            private WindowManager.LayoutParams paramsF = paramsRL;

            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //waitMemoObject.isTouchingMoveButton = v.isPressed();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        waitMemoObject.isTouchingMoveButton = true;
                        initialX = paramsF.x;
                        initialY = paramsF.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        waitMemoObject.isTouchingMoveButton = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                        paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(inflatedView, paramsF);
                        break;
                    default:
                        waitMemoObject.isTouchingMoveButton = false;
                        break;
                }
                return false;
            }
        });
        button_Size.setOnTouchListener(new View.OnTouchListener() {
            private WindowManager.LayoutParams paramsF = paramsRL;

            private int initialWidth;
            private int initialHeight;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //waitMemoObject.isTouchingSizeButton = v.isPressed();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        waitMemoObject.isTouchingSizeButton = true;
                        initialWidth = paramsF.width;
                        initialHeight = paramsF.height;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        waitMemoObject.isTouchingSizeButton = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        paramsF.width = initialWidth + (int) (event.getRawX() - initialTouchX);
                        paramsF.height = initialHeight + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(inflatedView, paramsF);
                        break;
                    default:
                        waitMemoObject.isTouchingSizeButton = false;
                        break;
                }
                return false;
            }
        });

        seekBar_Transparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                inflatedView.setAlpha(((float)progress)/100f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                waitMemoObject.isTouchingTranparencySlider = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                waitMemoObject.isTouchingTranparencySlider = false;
            }
        });/*
        seekBar_Transparency.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        waitMemoObject.isTouchingTranparencySlider = true;
                    case MotionEvent.ACTION_UP:
                        waitMemoObject.isTouchingTranparencySlider = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        waitMemoObject.isTouchingTranparencySlider = true;
                        break;
                    default:
                        waitMemoObject.isTouchingTranparencySlider = false;
                        break;
                }
                return false;
            }
        });/**/

        view_blocker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_Close.setVisibility(View.VISIBLE);
                button_Move.setVisibility(View.VISIBLE);
                button_Size.setVisibility(View.VISIBLE);
                if(!isLowRamDevice) {
                    seekBar_Transparency.setVisibility(View.VISIBLE);
                }
                view_blocker.setVisibility(View.GONE);

                view_blocker.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(isDebug){
                            System.out.println("isTouchingWebView "+waitMemoObject.isTouchingWebView+
                                    " / isTouchingMoveButton "+ waitMemoObject.isTouchingMoveButton +
                                    " / isTouchingSizeButton " + waitMemoObject.isTouchingSizeButton +
                                    " / isTouchingSlider "+waitMemoObject.isTouchingTranparencySlider);
                        }
                        if( (!waitMemoObject.isTouchingWebView) &&
                            //! waitMemoObject.isTouchingCloseButton &&
                                (!waitMemoObject.isTouchingMoveButton) &&
                                (!waitMemoObject.isTouchingSizeButton) &&
                                (!waitMemoObject.isTouchingTranparencySlider)) {
                            button_Close.setVisibility(View.GONE);
                            button_Move.setVisibility(View.GONE);
                            button_Size.setVisibility(View.GONE);
                            if(!isLowRamDevice) {
                                seekBar_Transparency.setVisibility(View.GONE);
                            }
                            view_blocker.setVisibility(View.VISIBLE);
                        }else{
                            view_blocker.postDelayed(this,DELAY_DEFAULT_UI_WAIT);
                        }
                    }
                },DELAY_DEFAULT_UI_WAIT);
            }
        });


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //checkLowRamDevice

            ActivityManager activityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
            isLowRamDevice = activityManager.isLowRamDevice();
            activityManager = null;
            if (isLowRamDevice) {
                if(isDebug){
                    System.out.println("this is LowRamDevice! you should get another workaround");
                }
                //scrollView_floating.setBackgroundColor(0x42FF0000);

            }
        }

        windowManager.addView(inflatedView,paramsRL);
        numberOfOpenPlayers++;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static final int calculateSmallestWidth(WindowManager windowManager){
        Display display =  windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return size.x < size.y ? size.x : size.y;
    }

    public static final String parseYouTubeGetEmbedUrl(String url) {

        try {
            //https://www.youtube.com/embed/sT26YHeIYog
            if (url.contains("youtube.com/watch?v=")) {
                //this url has browser-pasted youtube address
                url = YOUTUBE_EMBED_ADDRESS + url.substring(url.indexOf("youtube.com/watch?v=")+20);
            } else if (url.contains("youtu.be/")) {
                //this url has shortened youtube address
                url = YOUTUBE_EMBED_ADDRESS + url.substring(url.indexOf("youtu.be/")+9);
            }else{
               //no youtube address
                url = null;
            }
        }catch(IndexOutOfBoundsException ioobe){
            ioobe.printStackTrace();
            url = null;
        }

        //due to performance issue setting lowest quality possible
        //https://www.youtube.com/embed/6tK0XUQQ3wA?rel=0&vq=small
        return url;//+"?rel=0&vq=small";
    }
    public class ButtonWaitMemoObject{
        boolean isTouchingWebView = false;
        //boolean isTouchingCloseButton = false;
        boolean isTouchingTranparencySlider = false;
        boolean isTouchingMoveButton = false;
        boolean isTouchingSizeButton = false;
    }
}
