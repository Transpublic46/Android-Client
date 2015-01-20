package se.poochoo.net;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;

import se.poochoo.proto.Messages.UserFeedbackRequest;
import se.poochoo.proto.Messages.UserFeedbackData;
import se.poochoo.proto.Messages.SmartRequestHeader;
import se.poochoo.proto.Messages.SmartRequest;
import se.poochoo.proto.Messages.SmartResponse;

/**
 * Created by Erik on 2013-09-22.
 */
public class NetworkInterface extends Thread {
    public enum Status {
        UNKNOWN_ERROR,
        EMPTY_RESPONSE,
        TIMEOUT,
        NOT_CONNECTED,
        NOT_FINISHED, // Not final state.
        SUCCESS,
    }
    public static class NetworkException extends RuntimeException {
        private final Status status;
        public NetworkException(Status status) {
            this.status = status;
        }
        public Status getStatus() {
            return status;
        }
    }
    public static interface ResponseCallBack {
        public void handleResponse(SmartResponse response, Status status);
    }
    public static interface InterfaceProvider {
        public NetworkInterface get(Context context, boolean async);
    }
    private static NetworkInterface instance;
    protected static InterfaceProvider defaultProvider = new InterfaceProvider() {
        @Override
        public NetworkInterface get(Context context, boolean async) {
            if (instance == null || !instance.open) {
                instance = new NetworkInterface(context);
            }
            if (async && !instance.isRunning()) {
                instance.start();
            }
            return instance;
        }
    };
    public static InterfaceProvider provider = defaultProvider;

    public static int getApiVersion(Context context) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
        return pInfo.versionCode;
    }

    private static int id = 0;
    private final int apiVersion;
    private HttpContentLoader httpContentLoader = new HttpContentLoader();
    private ArrayList<PendingRequest> pendingRequests = new ArrayList<PendingRequest>();
    private PendingRequest currentRequest = null;
    private static PendingRequest lastSuccessfulRequest = null;
    private Context context;
    private boolean open = true;
    private boolean running = false;

    protected NetworkInterface(Context context) {
        apiVersion = getApiVersion(context);
        instance = this;
        this.context = context;
    }

    @Override
    public void run() {
        running = true;
        waitForRequests();
    }

    private void waitForRequests() {
        while (open) {
            if (pendingRequests.size() > 0) {
                sendRequests();
            }
            synchronized (pendingRequests) {
                try {
                    pendingRequests.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    open = false;
                }
            }
        }
    }

    private void sendRequests() {
        while (pendingRequests.size() > 0) {
            synchronized (pendingRequests) {
                if (pendingRequests.size() > 0) {
                  currentRequest = pendingRequests.remove(0);
                }
            }
            if (currentRequest != null) {
                currentRequest.execute(httpContentLoader);
            }
        }
    }

    private SmartRequestHeader buildHeader() {
        return SmartRequestHeader.newBuilder()
                .setApi(this.apiVersion)
                .setId(++id)
                .setClientId(SmartRequestHeader.ClientId.ANDROID)
                .build();
    }

    public boolean canSendFeedback() {
        return lastSuccessfulRequest != null;
    }

    public void sendUserFeedbackRequest(UserFeedbackData userFeedbackData) {
        if (!open) {
            throw new RuntimeException("Network is closed!");
        }
        if (!canSendFeedback()) {
            throw new RuntimeException("Feedback not possible. Please check canSendFeedback()!");
        }
        UserFeedbackRequest feedbackRequest = UserFeedbackRequest.newBuilder()
                .setResponse(lastSuccessfulRequest.getResponse())
                .setRequest(lastSuccessfulRequest.getRequest())
                .setFeedBackData(userFeedbackData)
                .build();
        addAndNotifyThread(new PendingRequest(feedbackRequest));
    }

    private void addAndNotifyThread(PendingRequest request) {
        synchronized (pendingRequests) {
            pendingRequests.add(request);
            pendingRequests.notify();
        }
    }

    public boolean isRunning() {
        return open && running;
    }

    public static synchronized void close() {
        if (instance != null) {
            instance.closeInternal();
            instance = null;
        }
    }

    private void closeInternal() {
        open = false;
        instance = null;
        httpContentLoader.shutdown();
        synchronized (pendingRequests) {
            pendingRequests.notify();
        }
    }

    public void sendRequest(SmartRequest.Builder request, long timeoutMillis, ResponseCallBack callBack) {
        if (!NetworkPolicy.isConnectedToAnyNetwork(context)) {
            if (callBack != null) {
                callBack.handleResponse(null, Status.NOT_CONNECTED);
            }
            return;
        }
        if (!open) {
            throw new RuntimeException("Network is closed.");
        }
        httpContentLoader.abort(); // In case something is loading..
        request.setRequestHeader(buildHeader());
        addAndNotifyThread(new PendingRequest(
                request.build(),
                timeoutMillis,
                callBack));
    }

    public SmartResponse sendRequestSynchronous(SmartRequest.Builder request) {
        try {
          return sendRequestSynchronousInternal(request);
        } catch (NetworkException expected) {
            throw expected;
        } catch (Exception unexpected) {
            throw new NetworkException(Status.UNKNOWN_ERROR);
        }
    }

    private SmartResponse sendRequestSynchronousInternal(SmartRequest.Builder request) {
        if (!NetworkPolicy.isConnectedToAnyNetwork(context)) {
            throw new NetworkException(Status.NOT_CONNECTED);
        }
        if (!open) {
            throw new RuntimeException("Network is closed.");
        }
        request.setRequestHeader(buildHeader());
        // TODO respect timeouts.
        SmartResponse response = new HttpContentLoader().sendRequest(request.build());
        if (response == null) {
            throw new NetworkException(Status.TIMEOUT);
        }
        if (response.getListDataCount() == 0) {
            throw new NetworkException(Status.EMPTY_RESPONSE);
        }
        return response;
    }

    public static void setlastSuccessfulRequest(PendingRequest lastSuccessfulRequest) {
        NetworkInterface.lastSuccessfulRequest = lastSuccessfulRequest;
    }
}
