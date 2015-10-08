package com.umangmathur.myapplication;

import com.umangmathur.myapplication.CustomAsyncTask.DoInBackgroundHandler;
import com.umangmathur.myapplication.CustomAsyncTask.OnPostExecuteHandler;
import com.umangmathur.myapplication.CustomAsyncTask.OnPreExecuteHandler;

public class AsyncTaskHandler {

    private OnPostExecuteHandler onPostExecuteHandler;
    private OnPreExecuteHandler onPreExecuteHandler;
    private DoInBackgroundHandler doInBackgroundHandler;

    public AsyncTaskHandler() {
    }

    public AsyncTaskHandler(OnPreExecuteHandler onPreExecuteHandler, DoInBackgroundHandler doInBackgroundHandler,OnPostExecuteHandler onPostExecuteHandler) {
        this.onPostExecuteHandler = onPostExecuteHandler;
        this.onPreExecuteHandler = onPreExecuteHandler;
        this.doInBackgroundHandler = doInBackgroundHandler;
    }

    public OnPostExecuteHandler getOnPostExecuteHandler() {
        return onPostExecuteHandler;
    }

    public OnPreExecuteHandler getOnPreExecuteHandler() {
        return onPreExecuteHandler;
    }

    public DoInBackgroundHandler getDoInBackgroundHandler() {
        return doInBackgroundHandler;
    }
}
