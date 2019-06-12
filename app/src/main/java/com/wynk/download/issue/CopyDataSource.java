package com.wynk.download.issue;

import android.util.Log;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;

import java.io.IOException;

public class CopyDataSource extends DataSource {

    private static final String LOG_TAG = "COPY_DATA_SOURCE";

    private final DataSource    mUpstream;

    private final DataSink      mDownstream;

    private final boolean       mIgnoreSinkExceptions;

    private boolean             mSinkOpened;

    public CopyDataSource(DataSource upstream, DataSink downstream, boolean ignoreSinkExceptions) {
        mUpstream = Assertions.checkNotNull(upstream);
        mDownstream = Assertions.checkNotNull(downstream);
        mIgnoreSinkExceptions = ignoreSinkExceptions;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
//        Log.d(LOG_TAG, "opened copydatasource for id=" + dataSpec.uri);
//        super.open(dataSpec);
        long dataLength = mUpstream.open(dataSpec);
//        if (dataSpec.length == C.LENGTH_UNSET && dataLength > 0) {
            // Reconstruct dataSpec in order to provide the resolved length to
            // the sink.
//            dataSpec = new DataSpec(dataSpec.uri/*, dataSpec.absoluteStreamPosition, dataSpec.position, dataLength, dataSpec.key, dataSpec.flags*/);
//        }

        openDownstream(/*dataSpec*/);

        return dataLength;
    }

    private void openDownstream(/*DataSpec dataSpec*/) throws IOException {
        try {
            if (!mSinkOpened) {
                mDownstream.open(/*dataSpec*/null);
                mSinkOpened = true;
            }
        } catch (IOException e) {
            if (!mIgnoreSinkExceptions) {
                throw e;
            }
            printSilentLog(e);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int max) throws IOException {
        int num = mUpstream.read(buffer, offset, max);
        if (num == C.RESULT_END_OF_INPUT) {
            finishDownstream();
        } else if (num > 0) {
            writeDownstream(buffer, offset, num);
        }
        return num;
    }

    private void writeDownstream(byte[] buffer, int offset, int max) throws IOException {
        try {
            if (mSinkOpened) {
                mDownstream.write(buffer, offset, max);
            }
        } catch (IOException e) {
            if (!mIgnoreSinkExceptions) {
                throw e;
            }
            printSilentLog(e);
        }
    }

    private void finishDownstream() {
        if (mSinkOpened) {
            mDownstream.finish();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            mUpstream.close();
        } finally {
            closeDownstream();
        }
    }

    private void closeDownstream() throws IOException {
        try {
            if (mSinkOpened) {
                mSinkOpened = false;
                mDownstream.close();
            }
        } catch (IOException e) {
            if (!mIgnoreSinkExceptions) {
                throw e;
            }
            printSilentLog(e);
        }
    }

    private void printSilentLog(Exception e) {
        Log.d(LOG_TAG, "Silently consuming " + e.getMessage());
    }

}
