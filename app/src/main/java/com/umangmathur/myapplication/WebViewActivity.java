package com.umangmathur.myapplication;

/**
 * Created by umang on 7/10/15.
 */

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebViewActivity extends Activity implements MyConstants {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        setTitle("Login To Twitter");
        Intent webViewIntent = getIntent();
        final String url = webViewIntent.getStringExtra("extra_url");
        String oAuthToken = webViewIntent.getStringExtra("oauth_token");
        if (null == url) {
            Log.e(MY_TAG, "URL cannot be null");
            finish();
        }
        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new MyWebViewClient(oAuthToken));
        webView.loadUrl(url);
    }


    class MyWebViewClient extends WebViewClient {
        private String oAuthTokenFromEarlierRequest;

        MyWebViewClient(String oAuthTokenFromEarlierRequest) {
            this.oAuthTokenFromEarlierRequest = oAuthTokenFromEarlierRequest;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains("umangmathur")) {
                Uri uri = Uri.parse(url);
                String verifier = uri.getQueryParameter("oauth_verifier");
                String oAuthToken = uri.getQueryParameter("oauth_token");
                Intent resultIntent = new Intent();
                resultIntent.putExtra("oauth_verifier", verifier);
                resultIntent.putExtra("oauth_token", oAuthToken);
                if (oAuthToken.equals(oAuthTokenFromEarlierRequest)) {
                    setResult(RESULT_OK, resultIntent);
                } else {
                    setResult(RESULT_CANCELED, resultIntent);
                    Toast.makeText(view.getContext(), "OAuthToken does not match", Toast.LENGTH_LONG).show();
                }
                finish();
                return true;
            }
            return false;
        }
    }

}
