package com.wynk.download.issue;

public interface DataSink extends com.google.android.exoplayer2.upstream.DataSink {

    /**
     * Indicates that writing to sink has finished completely. Will be called just
     * before close().
     */
    void finish();

}
