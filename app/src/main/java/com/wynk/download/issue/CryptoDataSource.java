package com.wynk.download.issue;

import com.facebook.cipher.IntegrityException;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;
import java.io.InputStream;

/**
 * A DataSource which decrypts an upstream DataSource
 */
public class CryptoDataSource extends DataSource {

    private static final int PADDING_BYTES = CryptoHelper.HEAD_PADDING + CryptoHelper.TAIL_PADDING;

    private final CryptoHelper mCryptoHelper;

    private final DataSource mUpstream;

    private DataSourceInputStream mDataSourceInputStream;

    private InputStream mCipherInputStream;

    public CryptoDataSource(CryptoHelper cryptoHelper, DataSource upstream) {
        mCryptoHelper = cryptoHelper;
        mUpstream = upstream;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        super.open(dataSpec);
        mDataSourceInputStream = new DataSourceInputStream(mUpstream);
        long length = mDataSourceInputStream.open(generateNewDataSpec(dataSpec));
        if (length != C.LENGTH_UNSET) {
            length -= PADDING_BYTES;
        }

        try {
            mCipherInputStream = mCryptoHelper.getCipherInputStream(mDataSourceInputStream);
            FileUtils.skipByReading(mCipherInputStream, dataSpec.absoluteStreamPosition);
            mDataSourceInputStream = null;
        } catch (Exception e) {
            throw new CryptoOpenFailedException("Failed to decrypt", e);
        }
        return length;
    }

    @Override
    public void close() throws IOException {
        try {
            if (mCipherInputStream != null) {
                mCipherInputStream.close();
                mCipherInputStream = null;
            }
        } catch (IntegrityException e) {
            throw new IOException("Failed to decrypt", e);
        } finally {
            if (mDataSourceInputStream != null) {
                mDataSourceInputStream.close();
                mDataSourceInputStream = null;
            }
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        try {
            int bytesRead = mCipherInputStream.read(buffer, offset, readLength);
            if (bytesRead < 0) {
                return C.RESULT_END_OF_INPUT;
            }
            return bytesRead;
        } catch (IntegrityException e) {
            throw new IOException("Failed to decrypt", e);
        }
    }

    private DataSpec generateNewDataSpec(DataSpec base) {
        return new DataSpec(base.uri, 0, 0, base.length, base.key, base.flags);
    }

}
