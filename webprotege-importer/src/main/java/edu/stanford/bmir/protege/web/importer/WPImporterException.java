package edu.stanford.bmir.protege.web.importer;

public class WPImporterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WPImporterException() {
    }

    public WPImporterException(String message) {
	super(message);
    }

    public WPImporterException(Throwable cause) {
	super(cause);
    }

    public WPImporterException(String message, Throwable cause) {
	super(message, cause);
    }

    public WPImporterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }
}
