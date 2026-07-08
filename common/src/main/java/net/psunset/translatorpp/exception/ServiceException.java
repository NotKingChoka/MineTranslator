package net.psunset.translatorpp.exception;

/**
 * An exception indicating a service error from the API.
 * Includes a message with the HTTP status code.
 */
public sealed abstract class ServiceException extends RuntimeException {
    public final int statusCode;

    public ServiceException(int statusCode) {
        this.statusCode = statusCode;
    }

    public ServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ServiceException(Throwable cause, int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }

    public ServiceException(String message, Throwable cause,  int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public static final class Google extends ServiceException {
        public Google(int statusCode) {
            super(statusCode);
        }
        public Google(String message, int statusCode) {
            super(message, statusCode);
        }
        public Google(Throwable cause, int statusCode) {
            super(cause, statusCode);
        }
        public Google(String message, Throwable cause, int statusCode) {
            super(message, cause, statusCode);
        }
    }

    public static final class OpenAI extends ServiceException {
        public OpenAI(int statusCode) {
            super(statusCode);
        }
        public OpenAI(String message, int statusCode) {
            super(message, statusCode);
        }
        public OpenAI(Throwable cause, int statusCode) {
            super(cause, statusCode);
        }
        public OpenAI(String message, Throwable cause, int statusCode) {
            super(message, cause, statusCode);
        }
    }

    public static final class DeepL extends ServiceException {
        public DeepL(int statusCode) {
            super(statusCode);
        }
        public DeepL(String message, int statusCode) {
            super(message, statusCode);
        }
        public DeepL(Throwable cause, int statusCode) {
            super(cause, statusCode);
        }
        public DeepL(String message, Throwable cause, int statusCode) {
            super(message, cause, statusCode);
        }
    }

    public static final class Gemini extends ServiceException {
        public Gemini(int statusCode) {
            super(statusCode);
        }
        public Gemini(String message, int statusCode) {
            super(message, statusCode);
        }
        public Gemini(Throwable cause, int statusCode) {
            super(cause, statusCode);
        }
        public Gemini(String message, Throwable cause, int statusCode) {
            super(message, cause, statusCode);
        }
    }

    public static final class Claude extends ServiceException {
        public Claude(int statusCode) {
            super(statusCode);
        }
        public Claude(String message, int statusCode) {
            super(message, statusCode);
        }
        public Claude(Throwable cause, int statusCode) {
            super(cause, statusCode);
        }
        public Claude(String message, Throwable cause, int statusCode) {
            super(message, cause, statusCode);
        }
    }
}
