package com.wynk.download.issue;

public interface Cancellable {

    public void cancel();

    public boolean isCancelled();
}
