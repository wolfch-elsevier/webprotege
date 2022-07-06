package edu.stanford.bmir.protege.web.http;

public class WPHttpException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public WPHttpException() {
    }

    public WPHttpException(String message) {
	super(message);
    }

    public WPHttpException(Throwable cause) {
	super(cause);
    }

    public WPHttpException(String message, Throwable cause) {
	super(message, cause);
    }

    public WPHttpException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }

}
