package se.poochoo.net;

import android.net.http.AndroidHttpClient;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import se.poochoo.proto.Messages;

public class HttpContentLoader {
    private static final String REMOTE_URL = "http://anka.locutus.se/F";
    private AndroidHttpClient httpClient;
    private HttpPost httpPost;

    public HttpContentLoader() {
        httpClient = AndroidHttpClient.newInstance("poochoo");
        httpClient.getParams().setBooleanParameter(
                "http.protocol.handle-redirects", false);
        httpClient.getParams().setBooleanParameter(
                "http.protocol.reject-relative-redirect", false);
    }

    public Messages.SmartResponse sendRequest(Messages.SmartRequest request) {
        InputStream stream = postToUrl(REMOTE_URL, request.toByteArray());
        if (stream == null) {
            return null;
        }
        try {
            return Messages.SmartResponse.parseFrom(stream);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Messages.UserFeedbackResponse sendFeedbackRequest(Messages.UserFeedbackRequest request) {
        InputStream stream = postToUrl(REMOTE_URL, request.toByteArray());
        if (stream == null) {
            return null;
        }
        try {
            return Messages.UserFeedbackResponse.parseFrom(stream);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private  InputStream postToUrl(String url, byte[] bytes){
        try{
            httpPost = new HttpPost(url);
            AndroidHttpClient.modifyRequestToAcceptGzipResponse(httpPost);
            httpPost.setHeader("Content-type", "application/octet-stream");
            ByteArrayEntity entity = new ByteArrayEntity(bytes);
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            httpPost = null;
            return AndroidHttpClient.getUngzippedContent(response.getEntity());
        } catch (Exception e)  {
            e.printStackTrace();
            return null;
        }
    }

    public void abort() {
        try {
            if (httpPost != null) {
              httpPost.abort();
            }
        } catch (Exception e) {
            Log.e(getClass().toString(), "Unable to abort HTTP post", e);
        }
    }

    public void shutdown() {
        httpClient.close();
    }
}
