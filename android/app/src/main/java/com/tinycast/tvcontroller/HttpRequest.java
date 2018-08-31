package com.tinycast.tvcontroller;

import android.preference.PreferenceActivity;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.StringRequest;
import com.tinycast.helper.FunctionCall;
import com.tinycast.helper.FunctionCallResult;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HttpRequest {
    final static String TAG = HttpRequest.class.getSimpleName();

    private RequestQueue mRequestQueueFollow;
    private RequestQueue mRequestQueueNotFollow;

    public HttpRequest() {
        mRequestQueueFollow = new RequestQueue(new NoCache(), new BasicNetwork(new HurlStack()));
        mRequestQueueNotFollow = new RequestQueue(new NoCache(), new BasicNetwork(new HurlStack() {
            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpURLConnection conn = super.createConnection(url);
                conn.setInstanceFollowRedirects(false);
                return conn;
            }
        }));
        mRequestQueueFollow.start();
        mRequestQueueNotFollow.start();
    }

    public void close() {
        if(mRequestQueueFollow != null) {
            mRequestQueueFollow.stop();
            mRequestQueueFollow = null;
        }
        if(mRequestQueueNotFollow != null) {
            mRequestQueueNotFollow.stop();
            mRequestQueueNotFollow = null;
        }
    }

    public void openUrl(final String url, final String body, final Map<String, Object> options,
                        final FunctionCall<Void, FunctionCallResult<HttpRepsonse>> cb)
    {

        Request<String> req = new Request<String>(
                body == null ? Request.Method.GET : Request.Method.POST,
                url,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(error == null || error.networkResponse == null) {
                            cb.callOnce(FunctionCallResult.<HttpRepsonse>asError(error));
                        } else {
                            NetworkResponse response = error.networkResponse;
                            String data = null;
                            if(response.data != null) {
                                try {
                                    data = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                                } catch (UnsupportedEncodingException e) {
                                    data = new String(response.data);
                                }
                            }
                            HttpRepsonse httpRepsonse = new HttpRepsonse();
                            httpRepsonse.setData(data);
                            httpRepsonse.setStatusCode(response.statusCode);
                            httpRepsonse.setHeaders(response.headers);
                            cb.callOnce(FunctionCallResult.asResult(httpRepsonse));
                        }
                    }
                }
        ) {

            @Override
            public byte[] getBody() throws AuthFailureError {
                if(body == null) return null;

                try {
                    return body.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String data = null;
                try {
                    data = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                } catch (UnsupportedEncodingException e) {
                    data = new String(response.data);
                }
                Response<String> res = Response.success(data, HttpHeaderParser.parseCacheHeaders(response));
                HttpRepsonse httpRepsonse = new HttpRepsonse();
                httpRepsonse.setData(data);
                httpRepsonse.setStatusCode(response.statusCode);
                httpRepsonse.setHeaders(res.cacheEntry != null ? res.cacheEntry.responseHeaders : null);
                cb.callOnce(FunctionCallResult.asResult(httpRepsonse));

                return res;
            }

            @Override
            protected void deliverResponse(String response) {

            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String contentType = options != null ? (String)options.get("contentType") : null;
                if(contentType != null)
                    headers.put("Content-Type", contentType);
                return headers;
            }

        };

        if(options != null && Boolean.TRUE.equals(options.get("disableRedirection")))
            mRequestQueueNotFollow.add(req);
        else
            mRequestQueueFollow.add(req);

    }

    public static class HttpRepsonse {
        private Map<String, String> headers;
        private int statusCode;
        private String data;

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

    }
}
