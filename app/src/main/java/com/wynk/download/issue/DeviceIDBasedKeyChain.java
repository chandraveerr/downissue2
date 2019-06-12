package com.wynk.download.issue;

import com.facebook.android.crypto.keychain.SecureRandomFix;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.exception.KeyChainException;
import com.facebook.crypto.keychain.KeyChain;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * This KeyChain implementation derives the key from deviceId. Because deviceId
 * can change for a device under certain circumstances, it is possible that
 * keychain might be affected by that. We use DeviceUtils.getUDID(Context) to
 * fetch deviceId which creates and caches the ID in following priority: <br/>
 * 1. ANDROID_ID <br/>
 * 2. SERIAL <br/>
 * 3. TelephonyManager.getDeviceId() <br/>
 * 4. UUID.randomUUID()
 */

public class DeviceIDBasedKeyChain implements KeyChain {

    private static final String LOG_TAG = "DEVICE_ID";

    private static final int ITERATION_COUNT = 1000;

    private static final SecureRandom SECURE_RANDOM_FIX = SecureRandomFix.createLocalSecureRandom();

    //private final SecureRandom        mSecureRandom;

    private byte[] mCipherKey;

    private boolean mSetCipherKey;

    public DeviceIDBasedKeyChain() {
        // mSecureRandom = new SecureRandom();
    }

    @Override
    public byte[] getCipherKey() throws KeyChainException {
        if (!mSetCipherKey) {
            mCipherKey = generateKey("23f8274137b0681b", CryptoConfig.KEY_128.keyLength);
//            mCipherKey = generateKey("731204114fd691b8", CryptoConfig.KEY_128.keyLength);
        }
        mSetCipherKey = true;
        return mCipherKey;
    }

    @Override
    public byte[] getMacKey() throws KeyChainException {
        throw new IllegalStateException("Method not supported");
    }

    @Override
    public byte[] getNewIV() throws KeyChainException {
        return generateRandomBytes(CryptoConfig.KEY_128.ivLength);
    }

    @Override
    public void destroyKeys() {
        mSetCipherKey = false;
        if (mCipherKey != null) {
            Arrays.fill(mCipherKey, (byte) 0);
        }
        mCipherKey = null;
    }

    private byte[] generateRandomBytes(int length) throws KeyChainException {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM_FIX.nextBytes(randomBytes);
        return randomBytes;
    }

    private byte[] generateSalt(int length) {
        byte[] salt = new byte[length];
        byte[] packageName = "com.bsbportal.music".getBytes();
        for (int i = 0; i < salt.length; i++) {
            salt[i] = packageName[i % packageName.length];
        }
        return salt;
    }

    /**
     * http://android-developers.blogspot.in/2013/02/using-cryptography-to-store
     * -credentials.html
     */
    private byte[] generateKey(String password, int keyLength) throws KeyChainException {
        try {
            byte[] salt = generateSalt(keyLength);
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, keyLength * 8);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return keyFactory.generateSecret(keySpec).getEncoded();
        } catch (Exception e) {
            throw new KeyChainException("Failed to generate key", e);
        }
    }

}
