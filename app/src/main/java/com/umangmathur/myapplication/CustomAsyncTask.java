package com.umangmathur.myapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CustomAsyncTask extends AsyncTask<String, Integer, JSONObject> implements MyConstants {

    private OnPostExecuteHandler onPostExecuteHandler;
    private OnPreExecuteHandler onPreExecuteHandler;
    private DoInBackgroundHandler doInBackgroundHandler;
    private boolean isError = false;
    private Context context;
    private String strErrorMsg;

    public CustomAsyncTask(AsyncTaskHandler asyncTaskHandler, Context context) {
        super();
        this.onPreExecuteHandler = asyncTaskHandler.getOnPreExecuteHandler();
        this.onPostExecuteHandler = asyncTaskHandler.getOnPostExecuteHandler();
        this.doInBackgroundHandler = asyncTaskHandler.getDoInBackgroundHandler();
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (this.onPreExecuteHandler != null) {
            this.onPreExecuteHandler.onPreExecute();
        }
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        JSONObject respJsonObj = new JSONObject();
        try {
            respJsonObj.put("exceptionThrown", true);
            respJsonObj = this.doInBackgroundHandler.doInBackground(params);
        } catch (JSONException e) {
            Log.e(MY_TAG, e.getMessage());
            isError = true;
            strErrorMsg = e.getMessage();
        } catch (IOException e) {
            Log.e(MY_TAG, e.getMessage());
            isError = true;
            strErrorMsg = e.getMessage();
        } catch (Exception e) {
            Log.e(MY_TAG, e.getMessage());
            isError = true;
            strErrorMsg = e.getMessage();
        }
        return respJsonObj;
    }

    @Override
    protected void onPostExecute(JSONObject responseJsonObj) {
        try {
            if (isError) {
                responseJsonObj.put("errorMsg", strErrorMsg);
            }
            this.onPostExecuteHandler.onPostExecute(responseJsonObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public interface OnPreExecuteHandler {
        void onPreExecute();
    }

    public interface DoInBackgroundHandler {
        JSONObject doInBackground(String... params) throws Exception;
    }

    public interface OnPostExecuteHandler {
        void onPostExecute(JSONObject responseJsonObj);
    }

}
