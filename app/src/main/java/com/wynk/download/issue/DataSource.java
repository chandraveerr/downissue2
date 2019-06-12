package com.wynk.download.issue;

import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;

public abstract class DataSource implements com.google.android.exoplayer2.upstream.DataSource {

    private DataSpec dataSpec;

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        this.dataSpec = dataSpec;
        return 0;
    }

    @Override
    public Uri getUri() {
        return dataSpec != null ? dataSpec.uri : null;
    }
}
