package com.czt.mp3recorder.core;


import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.czt.mp3recorder.util.LameUtil;

public class DataEncodeThread extends HandlerThread implements OnRecordPositionUpdateListener {
    private StopHandler mHandler;
    private static final int PROCESS_STOP = 1;
    private byte[] mMp3Buffer;
    private FileOutputStream mFileOutputStream;
    private List<Task> mTasks = Collections.synchronizedList(new ArrayList());

    public DataEncodeThread(File file, int bufferSize) throws FileNotFoundException {
        super("DataEncodeThread");
        if (file != null)
            this.mFileOutputStream = new FileOutputStream(file);
        this.mMp3Buffer = new byte[(int) (7200.0D + (double) (bufferSize * 2) * 1.25D)];
    }

    @Override
    public synchronized void start() {
        super.start();
        this.mHandler = new StopHandler(this.getLooper(), this);
    }

    private void check() {
        if (this.mHandler == null) {
            throw new IllegalStateException();
        }
    }

    public void sendStopMessage() {
        this.check();
        this.mHandler.sendEmptyMessage(1);
    }

    public Handler getHandler() {
        this.check();
        return this.mHandler;
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
        this.processData();
    }

    private int processData() {
        if (this.mTasks.size() > 0) {
            Task task = (Task) this.mTasks.remove(0);
            short[] buffer = task.getData();
            int readSize = task.getReadSize();
            TransformByteCallBack callBack = task.getCallBack();
            int encodedSize = LameUtil.encode(buffer, buffer, readSize, this.mMp3Buffer);
            if (callBack != null) {
                byte[] bytes = new byte[encodedSize];
                System.arraycopy(this.mMp3Buffer, 0, bytes, 0, encodedSize);
                callBack.audioByte(bytes);
            }
            if (encodedSize > 0 && this.mFileOutputStream != null) {
                try {
                    this.mFileOutputStream.write(this.mMp3Buffer, 0, encodedSize);
                } catch (IOException var6) {
                    var6.printStackTrace();
                }
            }

            return readSize;
        } else {
            return 0;
        }
    }

    private void flushAndRelease() {
        int flushResult = LameUtil.flush(this.mMp3Buffer);
        if (flushResult > 0) {
            try {
                if (this.mFileOutputStream != null)
                    this.mFileOutputStream.write(this.mMp3Buffer, 0, flushResult);
            } catch (IOException var11) {
                var11.printStackTrace();
            } finally {
                if (this.mFileOutputStream != null) {
                    try {
                        this.mFileOutputStream.close();
                    } catch (IOException var10) {
                        var10.printStackTrace();
                    }
                }

                LameUtil.close();
            }
        }

    }

    public void addTask(short[] rawData, int readSize) {
        this.mTasks.add(new Task(rawData, readSize));
    }

    public void addTask(short[] rawData, int readSize, TransformByteCallBack callBack) {
        this.mTasks.add(new Task(rawData, readSize, callBack));
    }

    private class Task {
        private short[] rawData;
        private int readSize;
        private TransformByteCallBack callBack;

        public Task(short[] rawData, int readSize) {
            this.rawData = (short[]) rawData.clone();
            this.readSize = readSize;
        }

        public Task(short[] rawData, int readSize, TransformByteCallBack callBack) {
            this.rawData = (short[]) rawData.clone();
            this.readSize = readSize;
            this.callBack = callBack;
        }

        public short[] getData() {
            return this.rawData;
        }

        public int getReadSize() {
            return this.readSize;
        }

        public TransformByteCallBack getCallBack() {
            return this.callBack;
        }
    }

    private static class StopHandler extends Handler {
        private DataEncodeThread encodeThread;

        public StopHandler(Looper looper, DataEncodeThread encodeThread) {
            super(looper);
            this.encodeThread = encodeThread;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                while (true) {
                    int d = this.encodeThread.processData();
                    Log.d("My_Log" , "当前我想要停止" + d);
                    if (d <= 0) {
                        this.removeCallbacksAndMessages((Object) null);
                        this.encodeThread.flushAndRelease();
                        this.getLooper().quit();
                        Log.d("My_Log" , "当前我正在努力停下");
                        break;
                    }
                }
            }
        }
    }

    public interface TransformByteCallBack {
        void audioByte(byte[] data);
    }
}

