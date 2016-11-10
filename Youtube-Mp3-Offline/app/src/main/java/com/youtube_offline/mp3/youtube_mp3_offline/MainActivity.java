package com.youtube_offline.mp3.youtube_mp3_offline;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private String videoName = "videoName";
    private String thisName = "Youtube-mp3-offline";
    private boolean videoDownloaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        Log.i(thisName, "activity started");

        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyHavePermission()) {
                requestForSpecificPermission();
            }
        }

        TextView txtMessage = (TextView) findViewById(R.id.txtMessage);

        if (Intent.ACTION_SEND.equals(action) && type.equals("text/plain")) {

            Log.i(thisName, "handle share");

            txtMessage.setText(getResources().getString(R.string.downloading));

            // Handle shared URL from another application
            handleSendText(intent);
        } else {

            Log.i(thisName, "normal start");

            txtMessage.setText(getResources().getString(R.string.use_share));

//            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//            fab.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                            .setAction("Action", null).show();
//                }
//            });
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean checkIfAlreadyHavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
    }

    private void handleSendText(Intent intent) {

        videoDownloaded = false;

        String url = intent.getStringExtra(Intent.EXTRA_TEXT);

        Log.i(thisName, "Url: " + url);
        // check if we have a youtube url - full or short
        //if (url != null && url.matches("(^.+?youtube|^.+?youtu.be)")) {

        Log.i(thisName, "Youtube url valid");

        WebView webView = (WebView) findViewById(R.id.webView);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new MyWebClient(url));

        webView.getSettings().setJavaScriptEnabled(true);

        Log.i(thisName, "Loading page");

        // When Ajax call is done, JS will do a callback to JavaScriptApp to return the title of the video - here we know for sure that the request is done
        webView.addJavascriptInterface(new JavaScriptApp(), "javaMain");
        webView.loadData("", "text/html", null);

        webView.loadUrl(getResources().getString(R.string.youtube_mp3_url));

        webView.setDownloadListener(new DownloadListener() {

            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

                if (videoDownloaded == false) {

                    videoDownloaded = true;

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                    request.allowScanningByMediaScanner();
                    //Notify client once download is completed!
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, videoName + ".mp3");
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

                    dm.enqueue(request);

                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); //This is important!
                    intent.addCategory(Intent.CATEGORY_OPENABLE); //CATEGORY.OPENABLE
                    intent.setType("*/*");//any application,any extension

                    //Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();

                    // close application - the download will run in the background
                    finish();
                }
            }
        });

        //}
    }

    private class MyWebClient extends WebViewClient {

        private String youtubeUrl = null;

        public MyWebClient(String url) {

            super();

            youtubeUrl = url;
        }

        @Override
        public void onPageFinished(WebView view, String url) {

            Log.i(thisName, "Data loaded " + url);

            // if it's the first load
            if (url.equals(getResources().getString(R.string.youtube_mp3_url))) {

                Log.i(thisName, "Page loaded - add javascript");

                // this will be executed even when the download occurs - infinite loop
                JSSetLink(view);
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {

            Log.i(thisName, url);

            if (url.contains("youtube") || url.contains(".js")) {
                Log.i(thisName, "Is youtube url");
                return null;
            }

            Log.i(thisName, "Not youtube url");
            return new WebResourceResponse("text/plain", "utf-8", null);
        }

        public void JSSetLink(WebView webView) {

            webView.evaluateJavascript("(function() {\n" +
                    
                    "    document.onreadystatechange = function () {\n" +
                    
                    "        if(document.readyState !== 'complete')\n" +
                    "            return false;\n" +
                    "    \n" +
                    "        document.getElementById('youtube-url').value = '\" + youtubeUrl + \"';\n" +
                    "        document.getElementById('submit').click();\n" +
                    
                    "        var title = document.getElementById('title');\n" +
                    "        title.innerHTML = 'waiting';\n" +
                    
                    "        var titleText = null;\n" +
                    
                    "        var interval = setInterval(function() {\n" +
                    "            \n" +
                    "             if(title.innerHTML.search('</b>') > 0) {\n" +
                    
                    "                titleText = title.innerHTML;\n" +
                    "                titleText = titleText.replace(/<b>.*?<\\/b> /g, '');\n" +
                    
                    "                clearInterval(interval);\n" +
                    
                    "                javaMain.titleReady(titleText);\n" +
                    
                    "                var links = document.querySelectorAll(\"#dl_link a\");\n" +
                    
                    "                for(var i = 0; i < links.length; i++) {\n" +
                    
                    "                    if(links[i].style.display !== \"none\") {\n" +
                    "                        links[i].click();\n" +
                    "                        break;\n" +
                    "                    }\n" +
                    "                }\n" +
                    "             }\n" +
                    "        }, 200);\n" +
                    
                    "})();\n", null);
        }

    }

    private class JavaScriptApp {

        @JavascriptInterface
        public void titleReady(String title) {

            Log.i(thisName, "Title ready " + title);

            videoName = title;
        }
    }
}
