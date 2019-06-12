package com.wynk.download.issue

import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSource.Factory
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter

/**
 * Created by Akash on 23/04/18.
 */

class MusicDataSourceFactory(private val songId: String, private val filePath: String) : Factory {

    override fun createDataSource(): DataSource {
        return RentDataSource(songId, DefaultBandwidthMeter(), filePath)
    }
}
