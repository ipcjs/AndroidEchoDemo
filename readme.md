## 使用 android 自带回声消除

Android 在4.1 API leve 16 添加了回声消除处理 **AcousticEchoCanceler**

不过由于手机厂商太多，碎片化严重，实现效果也不一样，这里只做测试性使用

记录一下步骤：

**检测系统是否支持回声消除**

**创建录制，并拿到sessionid**

**创建回声消除 关联录制的sessionid**

**创建播放，关联录制的sessionid**

这三者由录制的sessionid 关联起来了，就能够实处理掉mic采集到的由喇叭播放出的声音了。

看代码：
```java
    public static boolean chkNewDev()
    {
          return android.os.Build.VERSION.SDK_INT >= 16;
    }
    
    public static boolean isDeviceSupport()
    {
        return AcousticEchoCanceler.isAvailable();
    }
```
检测是否支持，居然存在不准确的情况？
```java
        int initAudioRecord()
        {
            mBufferSizeInBytes = AudioRecord.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);
            if (chkNewDev())
            {
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mSampleRateInHz, mChannelConfig, mAudioFormat, mBufferSizeInBytes);
            }
            else
            {
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRateInHz, mChannelConfig, mAudioFormat, mBufferSizeInBytes);
            }

            int packSize = Math.min(960, mBufferSizeInBytes *2);
            audioData = new short[packSize];
            return 0;
        }
```
如果系统支持回声消除，则用**MediaRecorder.AudioSource.VOICE_COMMUNICATION**
```java
        public int getSessionId()
        {
            return mAudioRecord.getAudioSessionId();
        }
```
获取mic的sessionid，给回声消除和播放关联用
```java
    public boolean initAEC(int audioSession)
    {
        if (mCanceler != null)
        {
            return false;
        }
        mCanceler = AcousticEchoCanceler.create(audioSession);
        mCanceler.setEnabled(true);
        return mCanceler.getEnabled();
    }
```
创建回声消除 并使能
```java
        int initAudioTrack()
        {
            int sampleRateInHz = 48000;
            int channelConfig  =  AudioFormat.CHANNEL_IN_STEREO;
            int audioFormat    = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSizeInBytes = 1024*4;

            if (chkNewDev() && mRecorder != null)
            {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM, mRecorder.getSessionId());
            }
            else
            {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM);
            }
            return 0;
        }
```
如果系统支持回声处理 则创建关联mic 的sessionid

ok，走通了

```java
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
```

忘了说，在程序启动的时候需要设置一下 **AudioManager.MODE_IN_COMMUNICATION** 和 外放来验证回声处理效果