package se.poochoo.net;

import se.poochoo.proto.Messages;

/**
 * Created by Erik on 2013-10-07.
 */
public class PendingRequest {
    private Messages.SmartRequest request;
    private long timeout;
    private NetworkInterface.ResponseCallBack callBack;
    private Messages.SmartResponse response;
    private HttpContentLoader httpContentLoader;
    private NetworkInterface.Status status = NetworkInterface.Status.NOT_FINISHED;

    private Messages.UserFeedbackRequest userFeedbackRequest;
    PendingRequest(Messages.SmartRequest request, long timeout, NetworkInterface.ResponseCallBack callBack) {
        this.callBack = callBack;
        this.request = request;
        this.timeout = timeout;
    }

    PendingRequest(Messages.UserFeedbackRequest userFeedbackRequest) {
        this.userFeedbackRequest = userFeedbackRequest;
    }

    public void execute(HttpContentLoader httpContentLoader) {
        this.httpContentLoader = httpContentLoader;
        maintainRequest(); // Will abort the request at timeout.
        executeInternal(httpContentLoader);
        if (callBack != null) {
            callBack.handleResponse(response, status);
        }
    }

    private void executeInternal(HttpContentLoader httpContentLoader) {
        if (userFeedbackRequest != null) {
            httpContentLoader.sendFeedbackRequest(userFeedbackRequest);
        } else {
            this.response = httpContentLoader.sendRequest(request);
            if (response != null) {
                status = NetworkInterface.Status.SUCCESS;
                NetworkInterface.setlastSuccessfulRequest(this);
            } else if (status == NetworkInterface.Status.NOT_FINISHED) {
                status = NetworkInterface.Status.UNKNOWN_ERROR;
            }
        }
    }

    private void maintainRequest() {
        new Thread() {
            public void run() {
                if (status == NetworkInterface.Status.NOT_FINISHED) {
                    try {
                        synchronized (this) {
                            this.wait(timeout);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (status == NetworkInterface.Status.NOT_FINISHED) {
                    status = NetworkInterface.Status.TIMEOUT;
                    httpContentLoader.abort();
                }
            }
        }.start();
    }

    public Messages.SmartRequest getRequest() {
        return request;
    }

    public Messages.SmartResponse getResponse() {
        return response;
    }

}
