//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.czt.mp3recorder.core;

import android.media.AudioRecord;
import android.os.Process;

import java.io.File;
import java.io.IOException;

import com.czt.mp3recorder.util.LameUtil;
import com.zs.record.pool.JobManager;

public class MP3Recorder {
    public static final String TAG = MP3Recorder.class.getSimpleName();
    private static final int DEFAULT_AUDIO_SOURCE = 1;
    private static int DEFAULT_SAMPLING_RATE = 44100;
    private static final int DEFAULT_CHANNEL_CONFIG = 16;
    private static final PCMFormat DEFAULT_AUDIO_FORMAT;
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;
    private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;
    private static final int FRAME_COUNT = 160;
    private AudioRecord mAudioRecord = null;
    private int mBufferSize;
    private short[] mPCMBuffer;
    private DataEncodeThread mEncodeThread;
    private volatile boolean mIsRecording = false;
    private File mRecordFile;
    private int mVolume;
    private static final int MAX_VOLUME = 2000;
    private AudioBufferCallback callback;
    private boolean isVolumeCallback=true;

    public MP3Recorder(File recordFile,boolean volumeCallback) {
        this.mRecordFile = recordFile;
        isVolumeCallback=volumeCallback;
    }


    public void setDefaultSamplingRate(int samplingRate) {
        DEFAULT_SAMPLING_RATE = samplingRate;
    }

    public void setMinBufferSizeForByte(int bufferSize) {
        this.mBufferSize = bufferSize / 2;
    }

    public void setAudioBufferCallBack(AudioBufferCallback callBack) {
        this.callback = callBack;
    }

    public void start() throws IOException {
        if (!this.mIsRecording) {
            this.mIsRecording = true;
            this.initAudioRecorder();
            this.mAudioRecord.startRecording();

            JobManager.getInstance().submitRunnable(new Runnable() {
                boolean isStop = false;

                @Override
                public void run() {
                    Process.setThreadPriority(-19);
                    while (!isStop) {
                        int readSize = MP3Recorder.this.mAudioRecord.read(MP3Recorder.this.mPCMBuffer, 0, MP3Recorder.this.mBufferSize);
                        if (!MP3Recorder.this.mIsRecording){
                            isStop = true;
                        }
                        if (readSize > 0) {
                            MP3Recorder.this.mEncodeThread.addTask(MP3Recorder.this.mPCMBuffer, readSize);
                            if(isVolumeCallback){
                                calculateRealVolume(MP3Recorder.this.mPCMBuffer, readSize);
                            }
                        }
                    }
                    MP3Recorder.this.mAudioRecord.stop();
                    MP3Recorder.this.mAudioRecord.release();
                    MP3Recorder.this.mAudioRecord = null;
                    MP3Recorder.this.mEncodeThread.sendStopMessage();
                }
            });
        }
    }

    private void calculateRealVolume(short[] buffer, int readSize) {
        double sum = 0.0D;

        for (int i = 0; i < readSize; ++i) {
            sum += (double) (buffer[i] * buffer[i]);
        }

        if (readSize > 0) {
            double amplitude = sum / (double) readSize;
            MP3Recorder.this.mVolume = (int) Math.sqrt(amplitude);
        }

    }

    public int getRealVolume() {
        return this.mVolume;
    }

    public int getVolume() {
        return this.mVolume >= 2000 ? 2000 : this.mVolume;
    }

    public int getMaxVolume() {
        return 2000;
    }

    public void stop() {
        this.mIsRecording = false;
    }

    public boolean isRecording() {
        return this.mIsRecording;
    }

    private void initAudioRecorder() throws IOException {
        this.mBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE, 16, DEFAULT_AUDIO_FORMAT.getAudioFormat());
        int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
        int frameSize = this.mBufferSize / bytesPerFrame;
        if (frameSize % 160 != 0) {
            frameSize += 160 - frameSize % 160;
            this.mBufferSize = frameSize * bytesPerFrame;
        }

        this.mAudioRecord = new AudioRecord(1, DEFAULT_SAMPLING_RATE, 16, DEFAULT_AUDIO_FORMAT.getAudioFormat(), this.mBufferSize);
        this.mPCMBuffer = new short[this.mBufferSize];
        LameUtil.init(DEFAULT_SAMPLING_RATE, 1, DEFAULT_SAMPLING_RATE, 32, 7);
        this.mEncodeThread = new DataEncodeThread(this.mRecordFile, this.mBufferSize);
        this.mEncodeThread.start();
        this.mAudioRecord.setRecordPositionUpdateListener(this.mEncodeThread, this.mEncodeThread.getHandler());
        this.mAudioRecord.setPositionNotificationPeriod(160);
    }

    public interface AudioBufferCallback {
        void audioBuffer(byte[] buffer, boolean isEnd);
    }

    static {
        DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;
    }
}
