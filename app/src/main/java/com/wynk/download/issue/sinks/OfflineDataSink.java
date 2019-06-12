package com.wynk.download.issue.sinks;

import android.net.Uri;
import android.os.Environment;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.wynk.download.issue.DataSink;

import java.io.File;
import java.io.IOException;

/**
 * A DataSink for storing an offline song
 */
public class OfflineDataSink implements DataSink {

    private static String LOG_TAG = "OFFLINE_DATA_SINK";

    private final String mSongId;

    private DataSink mDownstream;

    private final boolean  mHls;

    public OfflineDataSink(String songId, boolean hls) throws CryptoInitializationException {
        mSongId = songId;
        mHls = hls;
        mDownstream  = new FileDataSink();
    }

    @Override
    public void open(DataSpec dataSpec) throws IOException {
//        MusicSpec musicSpec = mHls ? MusicSpec.create(mSongId, dataSpec.uri) : MusicSpec.create(mSongId);
        mDownstream.open(generateFileDataSpec(mSongId, dataSpec));
    }

    @Override
    public void close() throws IOException {
        mDownstream.close();
    }

    @Override
    public void finish() {
        mDownstream.finish();
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        mDownstream.write(buffer, offset, length);
    }

    private DataSpec generateFileDataSpec(String songid, DataSpec baseDataSpec) throws IOException {
//        String relPath = DownloadUtils.getRelativeRentedSongPath(musicSpec);
//        for (String dir : DownloadUtils.getRentDirPaths(MusicApplication.getInstance())) {
//            if (DownloadUtils.isPathAvailable(dir)) {
//                File file = null;
//                if (!mHls) {
//                    file = new File(dir, relPath + CryptoHelperUtils.INSTANCE.getSupportedEncryption().getSuffix());
////                    LogUtils.debugLog("ENCRYPT_LOG", relPath + CryptoHelperUtils.INSTANCE.getSupportedEncryption().getSuffix());
//                }
//
//                return DataSpecUtils.generateFileDataSpec(file, baseDataSpec);
//            }
//        }

        try {
            File file = new File(Environment.getExternalStorageDirectory().toString() + "/wynk-rented-decrypted", songid);
            return new DataSpec(Uri.fromFile(file)/*, baseDataSpec.absoluteStreamPosition, baseDataSpec.position, baseDataSpec.length, baseDataSpec.key, baseDataSpec.flags*/);
        } catch (Exception e) {
            throw new IOException("Write failed in offline ");
        }

//        throw new IOException("Write failed in offline " + musicSpec);
    }
}
