package com.umangmathur.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.umangmathur.myapplication.CustomAsyncTask.OnPostExecuteHandler;
import com.umangmathur.myapplication.CustomAsyncTask.OnPreExecuteHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

import cz.msebera.android.httpclient.HttpException;

import static com.umangmathur.myapplication.CustomAsyncTask.DoInBackgroundHandler;

/**
 * Created by umang on 8/10/15.
 */
public class MainActivity extends AppCompatActivity implements OnClickListener, MyConstants {

    private Button btnLogin;
    private TextView txtUsrData;
    private ProgressDialog progressDialog;
    private int webViewReqCode = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtUsrData = (TextView) findViewById(R.id.txtUsrData);
        btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLogin:
                AsyncTaskHandler oAuthTokenTaskHandler = new AsyncTaskHandler(getOAuthTokenTaskPreExec(), getOAuthTokeTaskDoInBackgroundHandler(), getOAuthTokenTaskPostExec());
                CustomAsyncTask oAuthTokenAsyncTask = new CustomAsyncTask(oAuthTokenTaskHandler, MainActivity.this);
                oAuthTokenAsyncTask.execute();
                break;
        }
    }

    private OnPreExecuteHandler getOAuthTokenTaskPreExec() {
        return new OnPreExecuteHandler() {
            @Override
            public void onPreExecute() {
                String progDialogMsg = "Fetching OAuthToken...";
                showProgDialog(progDialogMsg);
            }
        };
    }

    private DoInBackgroundHandler getOAuthTokeTaskDoInBackgroundHandler() {
        return new DoInBackgroundHandler() {
            @Override
            public JSONObject doInBackground(String... params) throws HttpException, GeneralSecurityException, JSONException, IOException {
                JSONObject responseJsonObj = Twitter.startTwitterAuthentication();
                return responseJsonObj;
            }
        };
    }

    private OnPostExecuteHandler getOAuthTokenTaskPostExec() {
        return new OnPostExecuteHandler() {
            @Override
            public void onPostExecute(JSONObject responseJsonObj) {
                showToast(responseJsonObj.toString());
                progressDialog.dismiss();
                boolean exceptionsThrownInAsyncTask = responseJsonObj.optBoolean("exceptionThrown", false);
                if (!exceptionsThrownInAsyncTask) {
                    String oauthToken = responseJsonObj.optString("oauth_token");
                    String responseStatus = responseJsonObj.optString("response_status");
                    boolean success = (responseStatus.equalsIgnoreCase("success") ? true : false);
                    if (success) {
                        openWebViewToLoginAndFetchOAuthVerifier(oauthToken);
                    } else {
                        showToast("Fetching OAuth Token Failed : ");
                    }
                } else {
                    String errorMsg = responseJsonObj.optString("errorMsg");
                    showToast("Fetching OAuth Token Failed : " + errorMsg);
                }
            }

        };
    }

    private void openWebViewToLoginAndFetchOAuthVerifier(String oAuthToken) {
        String WEBVIEW_TARGET_URL = WEBVIEW_INCOMPLETE_URL + oAuthToken;
        Intent webViewIntent = new Intent(MainActivity.this, WebViewActivity.class);
        webViewIntent.putExtra("extra_url", WEBVIEW_TARGET_URL);
        webViewIntent.putExtra("oauth_token", oAuthToken);
        startActivityForResult(webViewIntent, webViewReqCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK && requestCode == webViewReqCode) {
            String verifier = intent.getStringExtra("oauth_verifier");
            String oAuthToken = intent.getStringExtra("oauth_token");
            showToast("Verifier : " + verifier);
            getTwitterAccessToken(verifier, oAuthToken);
        }
    }

    private void getTwitterAccessToken(String verifier, String oAuthToken) {
        AsyncTaskHandler accessTokenTaskHandler = new AsyncTaskHandler(getAccessTokenTaskPreExec(), getAccessTokenTaskDoInBackgroundHandler(verifier, oAuthToken), getAccessTokenTaskPostExec());
        CustomAsyncTask getAccessTokenTask = new CustomAsyncTask(accessTokenTaskHandler, this);
        getAccessTokenTask.execute();
    }

    private OnPreExecuteHandler getAccessTokenTaskPreExec() {
        return new OnPreExecuteHandler() {
            @Override
            public void onPreExecute() {
                showProgDialog("Fetching Access Token");
            }
        };
    }

    private DoInBackgroundHandler getAccessTokenTaskDoInBackgroundHandler(final String verifier, final String oAuthToken) {
        return new DoInBackgroundHandler() {
            @Override
            public JSONObject doInBackground(String... params) throws HttpException, GeneralSecurityException, JSONException, IOException {
                JSONObject respJsonObj = Twitter.getTwitterAccessTokenFromAuthorizationCode(verifier, oAuthToken);
                return respJsonObj;
            }
        };
    }

    private OnPostExecuteHandler getAccessTokenTaskPostExec() {
        return new OnPostExecuteHandler() {
            @Override
            public void onPostExecute(JSONObject responseJsonObj) {
                progressDialog.dismiss();
                boolean exceptionsThrownInAsyncTask = responseJsonObj.optBoolean("exceptionThrown", false);
                if (!exceptionsThrownInAsyncTask) {
                    String responseStatus = responseJsonObj.optString("response_status");
                    boolean success = (responseStatus.equalsIgnoreCase("success") ? true : false);
                    if (success) {
                        final String accessTokenStr = responseJsonObj.optString("access_token");
                        final String accessTokenSecretStr = responseJsonObj.optString("access_token_secret");
                        startVerifyCredentialsTaskAndPrintUserData(accessTokenStr, accessTokenSecretStr);
                    } else {
                        showToast("Fetching Access Token Failed : ");
                    }
                    showToast(responseJsonObj.toString());
                    Log.i(MY_TAG, responseJsonObj.toString());
                } else {
                    String errorMsg = responseJsonObj.optString("errorMsg");
                    showToast("Fetching Access Token Failed : " + errorMsg);
                }
            }
        };
    }

    private void startVerifyCredentialsTaskAndPrintUserData(String accessTokenStr, String accessTokenSecretStr) {
        AsyncTaskHandler verifyCredsTaskHandler = new AsyncTaskHandler(getVerifyCredsPreExec(), getVerifyCredsDoInBackgroundHandler(accessTokenStr, accessTokenSecretStr), getVerifyCredsPostExec());
        CustomAsyncTask verifyCredsTask = new CustomAsyncTask(verifyCredsTaskHandler, this);
        verifyCredsTask.execute();
    }

    private OnPreExecuteHandler getVerifyCredsPreExec() {
        return new OnPreExecuteHandler() {
            @Override
            public void onPreExecute() {
                showProgDialog("Verifying Credentials and fetching Basic user info...");
            }
        };
    }

    private DoInBackgroundHandler getVerifyCredsDoInBackgroundHandler(final String accessTokenStr, final String accessTokenSecretStr) {
        return new DoInBackgroundHandler() {
            @Override
            public JSONObject doInBackground(String... params) throws HttpException, GeneralSecurityException, JSONException, IOException {
                JSONObject respJsonObj = Twitter.verifyCredentials(accessTokenStr, accessTokenSecretStr);
                return respJsonObj;
            }
        };
    }

    private OnPostExecuteHandler getVerifyCredsPostExec() {
        return new OnPostExecuteHandler() {
            @Override
            public void onPostExecute(JSONObject responseJsonObj) {
                progressDialog.dismiss();
                boolean exceptionsThrownInAsyncTask = responseJsonObj.optBoolean("exceptionThrown", false);
                if (!exceptionsThrownInAsyncTask) {
                    showToast(responseJsonObj.toString());
                    JSONObject twitterJsonObj = responseJsonObj.optJSONObject("twitter_jo");
                    String name = twitterJsonObj.optString("name");
                    String screenName = twitterJsonObj.optString("screen_name");
                    String location = twitterJsonObj.optString("location");
                    int statusCount = twitterJsonObj.optInt("statuses_count");
                    String NEW_LINE = "\n";
                    String usrDataStr = name + NEW_LINE + screenName + NEW_LINE + "Location : " + location + NEW_LINE + "Status Count : " + statusCount;
                    txtUsrData.setText(usrDataStr);
                } else {
                    String errorMsg = responseJsonObj.optString("errorMsg");
                    showToast("Failed to verify credentials and fetch user data : " + errorMsg);
                }
            }
        };
    }

    private void showProgDialog(String progDialogMsg) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage(progDialogMsg);
        progressDialog.show();
    }

    protected void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
