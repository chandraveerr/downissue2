package com.wynk.download.issue;

import java.io.IOException;

public class CryptoOpenFailedException extends IOException {

    public CryptoOpenFailedException(){
        super();
    }

    public CryptoOpenFailedException(String detailMessage) {
        super(detailMessage);
    }

    public CryptoOpenFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoOpenFailedException(Throwable cause) {
        super(cause == null ? null : cause.toString(), cause);
    }
}
