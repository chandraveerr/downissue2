package com.wynk.download.issue;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * FailSafeFile guarantees that the complete file is written before renaming it
 * to its actual name. Everything is written to a .tmp hidden file unless
 * finishWrite() is called.
 */
public class FailSafeFile {

    private final File mBaseFile;

    private final File mTempFile;

    private FileDescriptor mWriteFD;

    public FailSafeFile(File baseFile) {
        mBaseFile = baseFile;
        mTempFile = new File(baseFile.getParent(), "." + baseFile.getName() + ".tmp");
    }

    public FileInputStream openRead() throws FileNotFoundException {
        mTempFile.delete();
        return new FileInputStream(mBaseFile);
    }

    public FileOutputStream startWrite() throws IOException {
        mTempFile.delete();
        mTempFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(mTempFile);
        mWriteFD = fos.getFD();
        return fos;
    }

    public boolean finishWrite(OutputStream str) throws IOException {
        mWriteFD.sync();
        str.close();
        mBaseFile.delete();
        return mTempFile.renameTo(mBaseFile);
    }

    public void failWrite(OutputStream str) throws IOException {
        mWriteFD.sync();
        str.close();
        mTempFile.delete();
    }
}
