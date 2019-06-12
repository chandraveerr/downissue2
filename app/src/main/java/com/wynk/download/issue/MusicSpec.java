package com.wynk.download.issue;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;

import java.io.File;

public class MusicSpec implements Comparable<MusicSpec> {

    private static final String LOG_TAG = "MUSIC_SPEC";

    public static final String MASTER = "master";

    public static final String INDEX = "index";

    public static final String SEGMENT = "segment";

    private transient int mHashCode = 0;

    private static final String BITRATE_IDENTIFIER = "/music/";

    private final String mSongId;

    private final String mSegmentId;

    private int mBitrate;

    private boolean multipleBitrateIndex = false;

    public boolean isMultipleBitrateIndex() {
        return multipleBitrateIndex;
    }

    /**
     * Hashed value of authority for the Uri (Only for master and index specs)
     **/
    private final int mAuthorityCode;

    public static MusicSpec create(String songId) {
        return new MusicSpec(songId, null, -1, -1);
    }

    public static MusicSpec create(String songId, Uri uri) {
        Log.d(LOG_TAG, "create :::: songId : uri" + songId + "---" + uri.toString());
        if (URLUtil.isNetworkUrl(uri.toString())) {
            MusicSpec spec = parseCm4Url(songId, uri);
            if (spec == null) {
                return parseOpHlsUrl(songId, uri);
            } else {
                return spec;
            }
        } else {
            return parseFilePath(songId, uri);
        }
    }

    private static MusicSpec parseFilePath(String songId, Uri uri) {
        Log.d(LOG_TAG, "song id : uri " + songId + "---" + uri.toString());
        String filePath = uri.getPath();
        String segmentId = null;
        int bitrate = -1;
        int authorityCode = -1;

        try {
            File file = new File(filePath);
            segmentId = file.getName();
            if (segmentId.contains(MASTER) || segmentId.contains(INDEX)) {
                int lastIndex = segmentId.lastIndexOf("_");
                if (lastIndex != -1) {
                    authorityCode = Integer.parseInt(segmentId.substring(lastIndex + 1));
                    segmentId = segmentId.substring(0, lastIndex);
                }
            }
            try {
                bitrate = Integer.parseInt(file.getParentFile().getName());
            } catch (NumberFormatException e) {
                if (!segmentId.contains(MASTER)) {
                    throw e;
                }
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Failed to parse file path: " + filePath);
        }
        Log.d(LOG_TAG, "song id : uri " + songId + "---" + segmentId + "----" + bitrate + "-----" + authorityCode);
        return new MusicSpec(songId, segmentId, bitrate, authorityCode);
    }

    private static MusicSpec parseOpHlsUrl(String songId, Uri uri) {
        String url = uri.getPath();
        String segmentId = null;
        int bitrate = -1;
        int authorityCode = -1;
        boolean isMultipleIndexes = false;

        try {
            String[] splits = url.substring(url.lastIndexOf("/") + 1).split("_");
            segmentId = splits[0];

            if (segmentId.contains(MASTER) || segmentId.contains(INDEX)) {
                String authority = uri.getAuthority();
                if (!TextUtils.isEmpty(authority)) {
                    authorityCode = authority.hashCode();
                }
            }

            String tempString = url.substring(url.indexOf(BITRATE_IDENTIFIER) + BITRATE_IDENTIFIER.length());
            String bitrateString = tempString.substring(0, tempString.indexOf("/"));
            if (bitrateString.charAt(0) == ',') {
                bitrateString = bitrateString.substring(1);
            }
            String[] bitrates = bitrateString.split(",");

            if (segmentId.contains(INDEX) && bitrates.length > 1) {
                isMultipleIndexes = true;
            }

            if (splits.length > 1) {
                bitrate = Integer.parseInt(bitrates[Integer.parseInt(splits[1])]);
            } else if (bitrates.length == 1) {
                bitrate = Integer.parseInt(bitrates[0]);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Failed to parse url: " + uri.toString(), e);
        }
        Log.d(LOG_TAG, "song id : uri " + songId + "---" + segmentId + "----" + bitrate + "-----" + authorityCode);
        MusicSpec musicSpec = new MusicSpec(songId, segmentId, bitrate, authorityCode);
        if (isMultipleIndexes)
            musicSpec.multipleBitrateIndex = true;
        return musicSpec;
    }

    private static MusicSpec parseCm4Url(String songId, Uri uri) {
        String url = uri.getPath();
        String segmentId = null;
        int bitrate = -1;
        int authorityCode = -1;

        try {
            String[] splits = url.substring(url.lastIndexOf("/") + 1).split("\\.");
            segmentId = splits[0];

            if (segmentId.contains(MASTER) || segmentId.contains(INDEX)) {
                String authority = uri.getAuthority();
                Log.d(LOG_TAG, "authority : " + authority);
                if (!TextUtils.isEmpty(authority)) {
                    authorityCode = authority.hashCode();
                    Log.d(LOG_TAG, "authorityCode : " + authorityCode);
                }
            }
            // find quality of song
            if (!segmentId.equalsIgnoreCase(MASTER)) {
                int secondLastSlashIndex = url.substring(0, url.lastIndexOf('/')).lastIndexOf('/');
                Log.d(LOG_TAG, "secondLastIndex : " + secondLastSlashIndex);
                String qualityString = url.substring(secondLastSlashIndex + 1, url.lastIndexOf('/'));
                Log.d(LOG_TAG, "qualityString : " + qualityString);
                bitrate = Integer.parseInt(qualityString);
                Log.d(LOG_TAG, "Bitrate : " + qualityString);

            } else if (isCM4MasterBitratePresent(url)) {
                String tempString = url.substring(url.indexOf(BITRATE_IDENTIFIER) + BITRATE_IDENTIFIER.length());
                String bitrateString = tempString.substring(0, tempString.indexOf("/"));
                if (bitrateString.charAt(0) == ',') {
                    bitrateString = bitrateString.substring(1);
                }
                String[] bitrates = bitrateString.split(",");

                if (splits.length > 1) {
                    bitrate = Integer.parseInt(bitrates[Integer.parseInt(splits[1])]);
                } else if (bitrates.length == 1) {
                    bitrate = Integer.parseInt(bitrates[0]);
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "Bitrate finding failed in CM4");
            return null;
        }
        Log.d(LOG_TAG, "song id : uri " + songId + "---" + segmentId + "----" + bitrate + "-----" + authorityCode);
        return new MusicSpec(songId, segmentId, bitrate, authorityCode);
    }

    private static boolean isCM4MasterBitratePresent(String url) {
        int index = url.indexOf(BITRATE_IDENTIFIER) + BITRATE_IDENTIFIER.length();
        String[] splits = url.substring(index).split("/");
        if (splits.length == 1) {
            return url.substring(index).contains(MASTER);
        }
        return false;
    }

    /*
     * @deprecated
     */
    public MusicSpec(String songId, String segmentId, int bitrate, int authorityCode) {
        mSongId = songId;
        mSegmentId = segmentId;
        mBitrate = bitrate;
        mAuthorityCode = authorityCode;
    }

    public String getSongId() {
        return mSongId;
    }

    public String getSegmentId() {
        return mSegmentId;
    }

    public int getBitrate() {
        return mBitrate;
    }

    public int getAuthorityCode() {
        return mAuthorityCode;
    }

    public boolean isIndex() {
        return mSegmentId != null && mSegmentId.contains(INDEX);
    }

    public boolean isMaster() {
        return mSegmentId != null && mSegmentId.contains(MASTER);
    }

    public boolean isSegment() {
        return mSegmentId != null && mSegmentId.contains(SEGMENT);
    }

    /*
     * public void setBitrate(int bitrate){ mBitrate =
     * MusicUtils.getBitrates(MusicUtils.getSongQuality(bitrate))[0]; }
     */

    public void setBitrate(int bitrate) {
        mBitrate = bitrate;
    }

    @Override
    public int hashCode() {
        int result = mHashCode;
        if (result == 0) {
            result = mSongId != null ? mSongId.hashCode() : 0;
            result = result * 37 + mBitrate;
            result = result * 37 + mAuthorityCode;
            result = result * 37 + (mSegmentId != null ? mSegmentId.hashCode() : 0);
            mHashCode = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MusicSpec) {
            MusicSpec otherSpec = (MusicSpec) other;
            return equals(mSongId, otherSpec.mSongId) && equals(mSegmentId, otherSpec.mSegmentId) && mBitrate == otherSpec.mBitrate && mAuthorityCode == otherSpec.mAuthorityCode;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[" + mSongId + ", " + mSegmentId + ", " + mBitrate + ", " + mAuthorityCode + "]";
    }

    @Override
    public int compareTo(MusicSpec otherSpec) {

        if (equals(mSongId, otherSpec.mSongId) && equals(mSegmentId, otherSpec.mSegmentId) && mBitrate == otherSpec.mBitrate && mAuthorityCode == otherSpec.mAuthorityCode) {
            return 0;
        }

        return -1;
    }

    public int customCompareTo(MusicSpec that) {
        Log.d(LOG_TAG, "this :" + this.toString() + "     that : " + that.toString());

        int diff = this.mSongId.compareTo(that.mSongId);
        if (diff == 0) {
            String seg1 = this.mSegmentId == null ? "" : this.mSegmentId;
            String seg2 = that.mSegmentId == null ? "" : that.mSegmentId;
            diff = seg1.compareTo(seg2);
        } else {
            return -1;
        }

        if (diff == 0) {
            int lhs = this.mBitrate;
            int rhs = that.mBitrate;
            diff = lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
        } else {
            return -1;
        }

        if (diff >= 0) {
            int lhs = this.mAuthorityCode;
            int rhs = that.mAuthorityCode;
            diff = lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
        }

        Log.d(LOG_TAG, " final diff :" + diff);
        return diff;
    }

    public static boolean equals(Object o1, Object o2) {
        if (o1 != null && o2 != null) {
            return o1.equals(o2);
        } else {
            if (o1 == null && o2 == null) {
                return true;
            } else {
                return false;
            }
        }
    }
}
