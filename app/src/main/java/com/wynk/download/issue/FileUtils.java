package com.wynk.download.issue;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.exoplayer2.C;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FileUtils {

    private static final String LOG_TAG = "FILE_UTILS";

    private static final AtomicReference<byte[]> SHARED_BUFFER = new AtomicReference<>();

    private static final int BUFFER_SIZE = 16 * 1024;

    public static final int DEFAULT_RECURSION_DEPTH = 5;

    private static byte[] acquireBuffer() {
        byte[] buffer = SHARED_BUFFER.getAndSet(null);
        if (buffer == null) {
            buffer = new byte[BUFFER_SIZE];
        }
        return buffer;
    }

    private static void releaseBuffer(byte[] buffer) {
        SHARED_BUFFER.set(buffer);
    }

    public static void witeObjectToFile(Context context, Object object, String filename) {

        ObjectOutputStream objectOut = null;
        try {

            FileOutputStream fileOut = context.openFileOutput(filename, Activity.MODE_PRIVATE);
            objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(object);
            fileOut.getFD().sync();

        } catch (IOException e) {
            Log.w(LOG_TAG, "Failed to write " + filename, e);
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close();
                } catch (IOException e) {
                    Log.w(LOG_TAG, "Failed to close ObjectOutputStream", e);
                }
            }
        }
    }

    public static Object readObjectFromFile(Context context, String filename) {

        ObjectInputStream objectIn = null;
        Object object = null;
        try {

            FileInputStream fileIn = context.getApplicationContext().openFileInput(filename);
            objectIn = new ObjectInputStream(fileIn);
            object = objectIn.readObject();

        } catch (Exception e) {
            Log.w(LOG_TAG, filename + " not found", e);
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                    Log.w(LOG_TAG, "Failed to close ObjectInputStream", e);
                }
            }
        }
        return object;
    }

    public static boolean isValidDirectory(File file) {
        return file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..");
    }

    public static void cleanFileOrDirectory(File fileOrDirectory) {
        cleanFileOrDirectory(fileOrDirectory, DEFAULT_RECURSION_DEPTH);
    }

    /**
     * Removes all .tmp files and empty directories recursively. A directory is
     * treated empty if there are no files or directories inside it.
     */
    public static void cleanFileOrDirectory(File fileOrDirectory, int depth) {
        if (fileOrDirectory == null || depth < 0) {
            return;
        }

        if (depth > 0 && isValidDirectory(fileOrDirectory)) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    cleanFileOrDirectory(child, depth - 1);
                }
            }
        }

        if (isValidDirectory(fileOrDirectory) || (fileOrDirectory.isHidden() && fileOrDirectory.getName().endsWith(".tmp"))) {
            fileOrDirectory.delete();
        }
    }

    public static boolean deleteFileOrDirectory(File fileOrDirectory) {
        return deleteFileOrDirectory(fileOrDirectory, DEFAULT_RECURSION_DEPTH);
    }

    public static boolean deleteFileOrDirectory(File fileOrDirectory, int depth) {
        if (fileOrDirectory == null || depth < 0) {
            return false;
        }

        boolean success = true;
        if (depth > 0 && isValidDirectory(fileOrDirectory)) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    success = deleteFileOrDirectory(child, depth - 1) && success;
                }
            }
        }
        boolean isFileDeleted = false;// initialize otherwise IDE giving error, looks irritating
        try {
            isFileDeleted = fileOrDirectory.delete();
        } catch (SecurityException securityException) {
            Log.e(LOG_TAG, "Security Exception", new Exception(securityException));
        }

        return success && isFileDeleted;
    }

    /**
     * @param sourceLocation location of source file
     * @param targetLocation location of target file
     * @return true is copy operation of file or directory is successful
     * @throws IOException
     */
    public static boolean copyFileOrDirectory(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation == null || !sourceLocation.exists()) {
            return false;
        }
        boolean success = true;
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    success = success && copyFileOrDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
                }
            }
        } else {
            try {
                success = copyStreams(new FileInputStream(sourceLocation), new FileOutputStream(targetLocation));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to copy files", e);
            }
        }
        return success;
    }

    public static List<File> getFiles(File parent, boolean recursive) {
        return getFiles(parent, recursive, DEFAULT_RECURSION_DEPTH);
    }

    public static List<File> getFiles(File parent, boolean recursive, int depth) {
        if (parent == null || depth < 0) {
            return null;
        }

        List<File> output = new ArrayList<>();
        File[] files = parent.listFiles();
        if (files != null) {
            for (File file : files) {
                if (FileUtils.isValidDirectory(file)) {
                    if (recursive) {
                        List<File> children = getFiles(file, recursive, depth - 1);
                        if (children != null) {
                            output.addAll(children);
                        }
                    }
                } else if (file.isFile()) {
                    output.add(file);
                }
            }
        }
        return output;
    }

    public static String getFileOrDirectoryName(String absolutePath) {
        if (TextUtils.isEmpty(absolutePath)) {
            return null;
        }
        File f = new File(absolutePath);
        return f.getName();
    }

    public static String getParentDirectoryName(String absolutePath) {
        if (TextUtils.isEmpty(absolutePath)) {
            return null;
        }

        File f = new File(absolutePath);
        return f.getParentFile().getName();
    }

    public static String getParentDirectoryPath(String absolutePath) {
        if (TextUtils.isEmpty(absolutePath)) {
            return null;
        }

        File f = new File(absolutePath);
        return f.getParent();
    }

    public static boolean fileOrDirectoryExists(String path) {
        File f = new File(path);
        return f.exists();
    }

    public static boolean copyFiles(File inputFile, File outputFile) {
        File folder = outputFile.getParentFile();

        if ((folder.exists() || folder.mkdirs()) && inputFile.renameTo(outputFile)) {
            return true;
        }

        boolean success = false;

        try {
            success = copyStreams(new FileInputStream(inputFile), new FileOutputStream(outputFile), null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to copy files", e);
        }
        return success;
    }

    public static String getFileOrContentPathByUri(Context context, String path) {
        Uri uri = Uri.parse(path);
        if (uri.getScheme().compareTo("content") == 0) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                String pathStr = cursor.getString(column_index);
                cursor.close();
                return pathStr;
            }
            cursor.close();
        } else if (uri.getScheme().compareTo("file") == 0) {
            return path;
        }
        return null;
    }

    public static boolean copyStreams(InputStream inputStream, OutputStream outputStream, Cancellable cancellable) {
        byte[] buffer = acquireBuffer();
        int length = 0;

        try {
            while (true) {
                length = inputStream.read(buffer);
                if (length == -1 || cancellable.isCancelled()) {
                    break;
                }
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to copy streams", e);
        } finally {
            safeClose(inputStream);
            safeClose(outputStream);
            releaseBuffer(buffer);
        }

        return length == -1;
    }

    public static boolean copy(DataSource upstream, DataSink downstream/*, DataSpec dataSpec*/) throws IOException {
        boolean success = false;
//        if (cancellable.isCancelled()) {
//            return false;
//        }

        DataSource teeDataSource = new CopyDataSource(upstream, downstream, false);
        try {
            teeDataSource.open(/*dataSpec*/null);
            final byte[] buffer = new byte[BUFFER_SIZE];
            while (/*!cancellable.isCancelled()*/true) {
                if (teeDataSource.read(buffer, 0, BUFFER_SIZE) == C.RESULT_END_OF_INPUT) {
                    break;
                }
            }
//            if (!cancellable.isCancelled()) {
//                success = true;
//            }
        } finally {
            try {
                teeDataSource.close();
            } catch (Exception e) {
            }
        }

        return success;
    }

    public static boolean copyStreams(InputStream inputStream, OutputStream outputStream) {
        byte[] buffer = acquireBuffer();
        int length = 0;

        try {
            while (true) {
                length = inputStream.read(buffer);
                if (length == -1) {
                    break;
                }
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to copy streams", e);
        } finally {
            safeClose(inputStream);
            safeClose(outputStream);
            releaseBuffer(buffer);
        }

        return length == -1;
    }

    public static void safeClose(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
        }
    }

    public static long skipByReading(InputStream in, long byteCount) throws IOException {
        byte[] buffer = acquireBuffer();
        long skipped = 0;

        try {
            while (skipped < byteCount) {
                int toRead = (int) Math.min(byteCount - skipped, buffer.length);
                int read = in.read(buffer, 0, toRead);
                if (read == -1) {
                    break;
                }
                skipped += read;
                if (read < toRead) {
                    break;
                }
            }
        } finally {
            releaseBuffer(buffer);
        }

        return skipped;
    }


}
