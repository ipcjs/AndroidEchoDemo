package com.newrtc.echo.androidecho;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.util.Log;

public class AudioAec
{
	private AcousticEchoCanceler mCanceler = null;
	private Recorder mRecorder = null;
	private Player mPlayer = null;


    public static boolean chkNewDev()
    {
          return android.os.Build.VERSION.SDK_INT >= 16;
    }

	public static boolean isDeviceSupport()
	{
        return AcousticEchoCanceler.isAvailable();
	}

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

	public boolean setAECEnabled(boolean enable)
	{
		if (null == mCanceler)
		{
			return false;
		}
		mCanceler.setEnabled(enable);
		return mCanceler.getEnabled();
	}

	public boolean release()
	{
	    if (null == mCanceler)
	    {
	        return false;
	    }
	    mCanceler.setEnabled(false);
	    mCanceler.release();
	    return true;
	}

	public int startRecorderAndPlayer()
	{
		int iRet = 0;
		mRecorder = new Recorder();

		iRet = mRecorder.initAudioRecord();
		if(iRet < 0)
		{
			return -1;
		}

		if(isDeviceSupport())
		{
			if(initAEC(mRecorder.GetSessionId()))
			{
				setAECEnabled(true);
			}
		}

		mPlayer = new Player();
		if(iRet < 0)
		{
			return -1;
		}
		iRet = mPlayer.initAudioTrack();
		if(iRet < 0)
		{
			return -1;
		}

		mPlayer.startAudioTrack(); //start player
		mRecorder.startAudioRecord(); //start recorder


		return 0;
	}

	public int stopRecorderAndPlayer()
	{
		return 0;
	}




	class Recorder
	{
		AudioRecord mAudioRecord = null;
		Thread mAudioWorker = null;
		int mSampleRateInHz = 48000;
		int mChannelConfig =  AudioFormat.CHANNEL_IN_STEREO;
		int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
		int mBitRate = 64000;
		int mBufferSizeInBytes = 0;

		short audioData[] = null;

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

		public int GetSessionId()
		{
			return mAudioRecord.getAudioSessionId();
		}

		int startAudioRecord()
		{
			Thread audioWorker = new Thread(new Runnable()
			{
	            @Override
	            public void run()
	            {
	            	readMic();
	            }
	        });
			audioWorker.start();

			return 0;
		}

		void readMic()
		{
			if(mAudioRecord == null)
			{
				return ;
			}
	        mAudioRecord.startRecording();
	        while (!Thread.interrupted())
	        {
	        	Log.d("TAG", "readMic");
	            int size = mAudioRecord.read(audioData, 0, audioData.length);
	            if (size <= 0)
	            {
	                break;
	            }
	            mPlayer.playAudio(audioData, audioData.length);
	        }
		}
	}

	class Player
	{
		private AudioTrack mAudioTrack = null;

		int initAudioTrack()
		{
			int sampleRateInHz = 48000;
			int channelConfig  =  AudioFormat.CHANNEL_IN_STEREO;
			int audioFormat    = AudioFormat.ENCODING_PCM_16BIT;
			int bufferSizeInBytes = 1024*4;

			if (chkNewDev() && mRecorder != null)
			{
				mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM, mRecorder.GetSessionId());
			}
			else
			{
				mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM);
			}
			return 0;
		}

		int startAudioTrack()
		{
			mAudioTrack.play();
			return 0;
		}

		public int playAudio(short[] audioData, int sizeInShort)
		{
			mAudioTrack.write(audioData, 0, sizeInShort);
			return 0;
		}
	}

}
