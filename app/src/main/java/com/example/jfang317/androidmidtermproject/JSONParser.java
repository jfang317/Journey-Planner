package com.example.jfang317.androidmidtermproject;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class JSONParser {

    public JSONParser() {
    }

    public void getJSONFromUrl(final String url, final responseListener target) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void... params) {
                HttpURLConnection httpURLConnection = null;
                StringBuilder stringBuilder = new StringBuilder();
                try {
                    httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
                    InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());

                    int read;
                    char[] buff = new char[1024];
                    while ((read = inputStreamReader.read(buff)) != -1) {
                        stringBuilder.append(buff, 0, read);
                    }
                    return stringBuilder.toString();
                } catch (MalformedURLException localMalformedURLException) {
                    return "";
                } catch (IOException localIOException) {
                    return "";
                } finally {
                    if (httpURLConnection != null)
                        httpURLConnection.disconnect();
                }
            }

            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                target.onResponseComplete(result);
            }
        }.execute();
    }
}

interface responseListener{
    void onResponseComplete(String response);
}
