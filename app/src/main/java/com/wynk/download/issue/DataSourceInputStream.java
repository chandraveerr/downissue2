package com.wynk.download.issue;

import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;
import java.io.InputStream;

public class DataSourceInputStream extends InputStream {

    private final DataSource mDataSource;

    private final byte[]     mSingleByteArray;

    public DataSourceInputStream(DataSource dataSource) throws IOException {
        mDataSource = dataSource;
        mSingleByteArray = new byte[1];
    }

    public long open(DataSpec dataSpec) throws IOException {
        return mDataSource.open(dataSpec);
    }

    @Override
    public int read() throws IOException {
        int length = read(mSingleByteArray);
        if (length == -1) {
            return -1;
        }
        return mSingleByteArray[0] & 0xFF;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return mDataSource.read(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        mDataSource.close();
    }
}