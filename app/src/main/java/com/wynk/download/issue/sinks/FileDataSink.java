package com.wynk.download.issue.sinks;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.wynk.download.issue.DataSink;
import com.wynk.download.issue.FailSafeFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A DataSink to write files. DataSpec should contain file path as URI
 */
public class FileDataSink implements DataSink {

    private static final String LOG_TAG = "FILE_DATA_SINK";

    private FailSafeFile mFile;

    private OutputStream mOutputStream;

    private DataSpec mDataSpec;

    private boolean             mWriteFinished;

    @Override
    public void open(DataSpec dataSpec) throws IOException {
        cleanup();
        mDataSpec = dataSpec;
        File file = new File(dataSpec.uri.getPath());
        mFile = new FailSafeFile(file);
        mOutputStream = new BufferedOutputStream(mFile.startWrite());
    }

    @Override
    public void close() throws IOException {
        try {
            if (mFile != null && mOutputStream != null) {
                if (mWriteFinished && mFile.finishWrite(mOutputStream)) {
//                    LogUtils.debugLog(LOG_TAG, "Write successful " + mDataSpec.uri);
                    return;
                } else {
                    mFile.failWrite(mOutputStream);
//                    LogUtils.debugLog(LOG_TAG, "Write failed " + mDataSpec.uri);
                }
                throw new IOException("Write failed");
            }
        } finally {
            cleanup();
        }
    }

    @Override
    public void finish() {
        mWriteFinished = true;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        mOutputStream.write(buffer, offset, length);
    }

    private void cleanup() {
        mFile = null;
        mOutputStream = null;
        mDataSpec = null;
        mWriteFinished = false;
    }
}
