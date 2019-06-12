package com.wynk.download.issue;

import android.net.Uri;
import android.util.Log;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.File;
import java.io.IOException;

/**
 * A DataSource for reading an rented song
 */
public class RentDataSource extends DataSource {

    private static final String LOG_TAG = "RENT_DATA_SOURCE";

    private DataSource mUpstream;

    private File mFile;


    public RentDataSource(String songId, TransferListener transferListener, String path) throws CryptoInitializationException {
        mFile = new File(path);
        mUpstream = new CryptoDataSource(new CryptoHelper(songId), new FileDataSource(transferListener));
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
//        super.open(dataSpec);
        long length = mUpstream.open(generateFileDataSpec(dataSpec));
        Log.d(LOG_TAG, "Rent HIT ");
        return length;
    }

    private DataSpec generateFileDataSpec(DataSpec base) throws IOException {
//        for (CryptoHelperUtils.EncryptionVersions version : CryptoHelperUtils.INSTANCE.getSupportedVersions()) {
//            File file = new File(mFile.getPath() + version.getSuffix());
//            if (file.exists()) {
//                return new DataSpec(Uri.fromFile(file)/*, base.absoluteStreamPosition, base.position, base.length, base.key, base.flags*/);
//            }
//        }
        try {
            return new DataSpec(Uri.fromFile(mFile)/*, base.absoluteStreamPosition, base.position, base.length, base.key, base.flags*/);
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IOException("Rent MISS ");
    }

    @Override
    public void close() throws IOException {
        mUpstream.close();
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return mUpstream.read(buffer, offset, readLength);
    }

}
