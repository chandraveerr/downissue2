package com.wynk.download.issue;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A DataSource to read files. DataSpec should contain file path as Uri
 */
public class FileDataSource extends DataSource {

    private final TransferListener mTransferListener;

    private boolean                mIsOpened;

    private InputStream            mInputStream;

    public FileDataSource(TransferListener transferListener) {
        mTransferListener = transferListener;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        super.open(dataSpec);
        File file = new File(dataSpec.uri.getPath());
        mInputStream = new BufferedInputStream(new FailSafeFile(file).openRead());
        mInputStream.skip(dataSpec.absoluteStreamPosition);

        mIsOpened = true;
        if (mTransferListener != null) {
            mTransferListener.onTransferStart(this, dataSpec);
        }

        return file.length();
    }

    @Override
    public void close() throws IOException {
        if (mInputStream != null) {
            mInputStream.close();
            mInputStream = null;
        }

        if (mIsOpened) {
            mIsOpened = false;
            if (mTransferListener != null) {
                mTransferListener.onTransferEnd(this);
            }
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        int bytesRead = mInputStream.read(buffer, offset, readLength);

        if (mTransferListener != null) {
            mTransferListener.onBytesTransferred(this, bytesRead);
        }

        return bytesRead;
    }

}