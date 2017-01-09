package com.almoturg.sprog.api;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.almoturg.sprog.model.ParentComment;
import com.almoturg.sprog.model.Poem;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import in.uncod.android.bypass.Bypass;

public class SprogApiClient {
    private static SprogApiClient sSharedInstance;
    private RequestQueue mRequestQueue;
    private Context mContext;
    private HashMap<String, Poem> mMainPoemLinks = new HashMap<>();

    private SprogApiClient(@NonNull Context context) {
        mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
        mContext = context;
    }

    public static synchronized SprogApiClient getInstance(Context context) {
        if (sSharedInstance == null) {
            sSharedInstance = new SprogApiClient(context);
        }

        return sSharedInstance;
    }

    public interface OnGetPoemsCompletionListener {
        void onComplete(VolleyError error);
    }

    public void getPoems(final OnGetPoemsCompletionListener listener) {
        String url = "https://almoturg.com/poems.json";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                File poemsFile = new File(mContext.getExternalFilesDir(Environment
                        .DIRECTORY_DOWNLOADS), "poems.json");

                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(poemsFile);
                    fileOutputStream.write(response.toString().getBytes());
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                listener.onComplete(null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onComplete(error);
            }
        });

        mRequestQueue.add(jsonArrayRequest);
    }
}
