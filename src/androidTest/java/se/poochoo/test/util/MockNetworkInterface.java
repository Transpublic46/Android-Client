package se.poochoo.test.util;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import se.poochoo.net.NetworkInterface;
import se.poochoo.proto.Messages.SmartRequest;
import se.poochoo.proto.Messages.SmartResponse;

/**
 * Created by Erik on 2013-10-05.
 */
public class MockNetworkInterface extends NetworkInterface {

    public static class MissingMockdataException extends RuntimeException {
        public MissingMockdataException(String s) {
            super(s);
        }
    }

    public static void useMockInterface() {
        NetworkInterface.provider = new InterfaceProvider() {
            @Override
            public NetworkInterface get(Context context, boolean async) {
              return new MockNetworkInterface(context);
            }
        };
    }
    public static void useRealInterface() {
        NetworkInterface.provider = NetworkInterface.defaultProvider;
    }

    public static SmartRequest lastRequest;

    private static class MockedResponse {
        private MockedResponse(SmartResponse response, long delay) {
            this.response = response;
            this.delay = delay;
        }

        SmartResponse response;
        long delay;
    }

    private static ArrayList<MockedResponse> responses = new ArrayList<MockedResponse>();
    public static void addMockResponse(SmartResponse response) {
        addMockResponse(response, 100L);
    }
    public static void addMockResponse(SmartResponse response, long delay) {
        responses.add(new MockedResponse(response, delay));
    }

    private MockNetworkInterface(Context context) {
        super(context);
    }

    private void verifyRequestExists() {
        if (responses.isEmpty()) {
            throw new MissingMockdataException(
                    "No responses available to handle " +
                    lastRequest +
                    ", you need to call addMockResponse first!");
        } else {
            Log.i(getClass().getName(), responses.size() + " responses available.");
        }
    }

    private SmartResponse getAndWait() {
        MockedResponse response = null;
        synchronized (responses) {
            response = responses.remove(0);
        }
        try {
            Thread.sleep(response.delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return response.response;
    }

    @Override
    public void sendRequest(SmartRequest.Builder request,final long timeoutMillis, final ResponseCallBack callBack) {
        lastRequest = request.build();
        verifyRequestExists();
        new Thread() {
            public void run() {
                long now = System.currentTimeMillis();
                SmartResponse response = getAndWait();
                if ((System.currentTimeMillis() - now) > timeoutMillis) {
                    callBack.handleResponse(null, Status.TIMEOUT);
                } else {
                    callBack.handleResponse(response, Status.SUCCESS);
                }
            }
        }.start();
    }

    @Override
    public SmartResponse sendRequestSynchronous(SmartRequest.Builder request) {
        lastRequest = request.build();
        verifyRequestExists();
        return getAndWait();
    }
}
