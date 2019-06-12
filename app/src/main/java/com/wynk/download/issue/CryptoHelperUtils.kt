
package com.wynk.download.issue
import java.util.*

object CryptoHelperUtils {

    var supportedVersions = ArrayList<EncryptionVersions>(Arrays.asList(EncryptionVersions.VERSION_2, EncryptionVersions.VERSION_1,EncryptionVersions.VERSION_0))

    enum class EncryptionVersions(val suffix: String) {

        VERSION_0(""),    //legacy encryption with conceal
        VERSION_1("_v1"), //plain java based encryption
        VERSION_2("_v2")  //legacy encryption with conceal with 256 bit key length

    }

    fun getVersionStrippedString(id: String?): String? {
        id?.let {
            for (version in supportedVersions) {
                if (id.endsWith(version.suffix)) {
                    return id.substring(0, id.length - version.suffix.length)
                }
            }
        }
        return id
    }

    fun getVersionForSong(id: String?): EncryptionVersions {
        id?.let {
            for (version in supportedVersions) {
                if (id.endsWith(version.suffix)) {
                    return version
                }
            }
        }
        return EncryptionVersions.VERSION_0
    }

    fun getSupportedEncryption(): EncryptionVersions {
        return EncryptionVersions.VERSION_1
    }


}