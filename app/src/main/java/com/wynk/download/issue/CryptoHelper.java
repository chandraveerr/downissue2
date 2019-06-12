package com.wynk.download.issue;

import android.util.Log;
import com.facebook.android.crypto.keychain.AndroidConceal;
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.Entity;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;
import com.facebook.crypto.keychain.KeyChain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CryptoHelper implements Cancellable {

    private static final String LOG_TAG = "CRYPTO_HELPER";

    /*
     * VersionCodes.CIPHER_SERALIZATION_VERSION + VersionCodes.CIPHER_ID + IV
     */
    public static final int HEAD_PADDING = 1 + 1 + CryptoConfig.KEY_128.ivLength;

    /* TAG */
    public static final int TAIL_PADDING = CryptoConfig.KEY_128.tagLength;

    private static KeyChain sKeyChain;

    private final Crypto mCrypto;

    private final Entity mEntity;

    private volatile boolean mCancelled;

    private volatile boolean mUseEncryption = true;

    private volatile boolean mUse256;

    public CryptoHelper(String secret) throws CryptoInitializationException {
        checkKeyChainInit();
        mCrypto = AndroidConceal.get().createCrypto128Bits(sKeyChain);
        // mCrypto = new Crypto(sKeyChain, new SystemNativeCryptoLibrary(),
        // CryptoConfig.KEY_128);
        if (!mCrypto.isAvailable()) {
            Log.e(LOG_TAG, "CryptoInitFailed", new Exception("Failed to load crypto libs"));
            throw new CryptoInitializationException(new Exception("Failed to load crypto libs"));
        }
        mEntity = new Entity(secret);
    }

    public CryptoHelper(String secret, String path) throws CryptoInitializationException {
        checkKeyChainInit();
        Log.d("ENCRYPT_LOG", path != null ? path : "path empty");
        if (path != null) {
            mUse256 = path.endsWith(CryptoHelperUtils.EncryptionVersions.VERSION_2.getSuffix());
            mUseEncryption = /*!path.endsWith(CryptoHelperUtils.VERSION_1) ||*/ mUse256;
        }
        mCrypto = mUse256 ? AndroidConceal.get().createCrypto256Bits(sKeyChain) :
                AndroidConceal.get().createCrypto128Bits(sKeyChain);
        if (!mCrypto.isAvailable()) {
            Log.e(LOG_TAG, "CryptoInitFailed", new Exception("Failed to load crypto libs"));
            throw new CryptoInitializationException(new Exception("Failed to load crypto libs"));
        }
        mEntity = new Entity(secret);
    }

    private synchronized void checkKeyChainInit() {
        if (sKeyChain != null) {
            return;
        }
        sKeyChain = new DeviceIDBasedKeyChain();
    }

    public static void destroyKeys() {
        if (sKeyChain != null) {
            sKeyChain.destroyKeys();
            // Use this as an opportunity to switch KeyChain
            if (sKeyChain instanceof SharedPrefsBackedKeyChain) {
                sKeyChain = new DeviceIDBasedKeyChain();
            }
        }
    }

    public void cancel() {
        mCancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return mCancelled;
    }

    public OutputStream getCipherOutputStream(OutputStream outputStream) throws IOException, CryptoInitializationException, KeyChainException {
        return mUseEncryption ? mCrypto.getCipherOutputStream(outputStream, mEntity) : outputStream;
    }

    public InputStream getCipherInputStream(InputStream inputStream) throws IOException, CryptoInitializationException, KeyChainException {
        return mUseEncryption ? mCrypto.getCipherInputStream(inputStream, mEntity) : inputStream;
    }

    public boolean encrypt(InputStream inputStream, OutputStream outputStream) throws IOException, CryptoInitializationException, KeyChainException {
        long time = System.currentTimeMillis();
        boolean success = FileUtils.copyStreams(inputStream, getCipherOutputStream(outputStream), this);
        if (success) {
            long now = System.currentTimeMillis();
            Log.d(LOG_TAG, "Encryption completed in " + (now - time) + "ms");
        }
        return success;
    }

    public boolean decrypt(InputStream inputStream, OutputStream outputStream) throws IOException, CryptoInitializationException, KeyChainException {
        long time = System.currentTimeMillis();
        boolean success = FileUtils.copyStreams(getCipherInputStream(inputStream), outputStream, this);
        if (success) {
            long now = System.currentTimeMillis();
            Log.d(LOG_TAG, "Decryption completed in " + (now - time) + "ms");
        }
        return success;
    }
}
