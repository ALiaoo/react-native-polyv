package com.whalew.polyv;

import java.util.HashMap;

import android.support.annotation.Nullable;
import android.util.Log;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.widget.FrameLayout;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.LifecycleEventListener;

import com.easefun.polyvsdk.video.PolyvVideoView;
import com.easefun.polyvsdk.video.PolyvMediaInfoType;
import com.easefun.polyvsdk.video.PolyvPlayErrorReason;

import com.easefun.polyvsdk.srt.PolyvSRTItemVO;

import com.easefun.polyvsdk.video.listener.IPolyvOnPreloadPlayListener;
import com.easefun.polyvsdk.video.listener.IPolyvOnPlayPauseListener;
import com.easefun.polyvsdk.video.listener.IPolyvOnCompletionListener2;
import com.easefun.polyvsdk.video.listener.IPolyvOnErrorListener2;
import com.easefun.polyvsdk.video.listener.IPolyvOnInfoListener2;
import com.easefun.polyvsdk.video.listener.IPolyvOnPreparedListener2;
import com.easefun.polyvsdk.video.listener.IPolyvOnVideoPlayErrorListener2;
import com.easefun.polyvsdk.video.listener.IPolyvOnVideoStatusListener;
import com.easefun.polyvsdk.video.listener.IPolyvOnVideoSRTListener;


import com.easefun.polyvsdk.sub.danmaku.auxiliary.BilibiliDanmakuTransfer;
import com.easefun.polyvsdk.sub.danmaku.auxiliary.PolyvDanmakuTransfer;
import com.easefun.polyvsdk.sub.danmaku.entity.PolyvDanmakuEntity;
import com.easefun.polyvsdk.sub.danmaku.entity.PolyvDanmakuInfo;
import com.easefun.polyvsdk.sub.danmaku.main.PolyvDanmakuManager;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public class ReactPlayerView extends FrameLayout implements LifecycleEventListener {

	private static final int SHOW_PROGRESS = 1;

	private ThemedReactContext mThemedReactContext;
    private RCTEventEmitter mEventEmitter;
    private PolyvVideoView mVideoView = null;
    private String mVid;

    private DanmakuContext mContext;
    private PolyvDanmakuManager danmakuManager;
    private IDanmakuView iDanmakuView = null;

    private static final int SEEKTOFITTIME = 12;
    private static final int PAUSE = 13;
    private boolean status_canauto_resume = true;
    private boolean status_pause_fromuser = true;
    private boolean status_pause;

    private long seekToTime = -1;
    private long updateTime = -1;
    private boolean isPrepare;
    private boolean isStart;

    private int autoplay = 1;
	private int progress = 0;

    private Handler handler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SHOW_PROGRESS:
				track();
				break;
				case SEEKTOFITTIME:
                    seekToFitTime();
                    break;
                case PAUSE:
                    if (status_pause)
                        pause();
                    break;
			}
		}
	};

	private IPolyvOnPreloadPlayListener mIPolyvOnPreloadPlayListener = new IPolyvOnPreloadPlayListener() {
		@Override
		public void onPlay() {
			start();
		}
	};

	private IPolyvOnPreparedListener2 mIPolyvOnPreparedListener2 = new IPolyvOnPreparedListener2() {

		@Override
		public void onPrepared() {

			//Log.i("gogoup-dev", String.format("progress value = %d", progress));

			if (autoplay == 0) {
				setPaused(true);
			} else {

				if (progress > 0) {
					int duration = (int)(mVideoView.getDuration() / 1000);

					//Log.i("gogoup-dev", String.format("duration value = %d", duration));
					int seek = (int)(duration * progress / 100);
					//Log.i("gogoup-dev", String.format("seek value = %d", seek));
					setSeek(seek);
				}
			}
			

			WritableMap event = Arguments.createMap();
			event.putInt("duration", mVideoView.getDuration() / 1000);
			mEventEmitter.receiveEvent(getId(), Events.LOADED.toString(), event);
		}
	};

	private IPolyvOnPlayPauseListener mIPolyvOnPlayPauseListener = new IPolyvOnPlayPauseListener() {

		@Override
		public void onPause() {
			handler.removeMessages(SHOW_PROGRESS);
			mEventEmitter.receiveEvent(getId(), Events.PAUSED.toString(), Arguments.createMap());
		}

		@Override
		public void onPlay() {
			WritableMap event = Arguments.createMap();
			event.putInt("current", mVideoView.getCurrentPosition() / 1000);
			mEventEmitter.receiveEvent(getId(), Events.PLAYING.toString(), event);

			handler.sendEmptyMessage(SHOW_PROGRESS);
		}

		@Override
		public void onCompletion() {
			handler.removeMessages(SHOW_PROGRESS);
		}
	};

	private IPolyvOnInfoListener2 mIPolyvOnInfoListener2 = new IPolyvOnInfoListener2() {
		@Override
		public boolean onInfo(int what, int extra) {
			 switch (what){
			 	case PolyvMediaInfoType.MEDIA_INFO_BUFFERING_START:
			 	pause(false);
			 	break;
			 	case PolyvMediaInfoType.MEDIA_INFO_BUFFERING_END:
			 	resume(false);
			 	break;
			 }

			 return true;
		}
	};

	private IPolyvOnVideoStatusListener mIPolyvOnVideoStatusListener = new IPolyvOnVideoStatusListener() {

		@Override
		public void onStatus(int status) {


			if (status < 60) {
				Log.i("polyv", "onErrorStatus");
			} else {
				Log.i("polyv", "onNormal");

			}
		}

	};

	private IPolyvOnCompletionListener2 mIPolyvOnCompletionListener2 = new IPolyvOnCompletionListener2() {

		 @Override
		 public void onCompletion() {
		 	Log.i("polyv", "onEndC");
		 	mEventEmitter.receiveEvent(getId(), Events.STOP.toString(), Arguments.createMap());
		 }
	};

	private IPolyvOnVideoPlayErrorListener2 mIPolyvOnVideoPlayErrorListener2 = new IPolyvOnVideoPlayErrorListener2() {

		@Override
		public boolean onVideoPlayError(@PolyvPlayErrorReason.PlayErrorReason int playErrorReason) {
			Log.i("polyv", "onError");
			return true;
		}
	};

	private IPolyvOnVideoSRTListener mIPolyvOnVideoSRTListener = new IPolyvOnVideoSRTListener() {

		@Override
        public void onVideoSRT(@Nullable PolyvSRTItemVO subTitleItem) {
        	String subtitle = "";

            if (subTitleItem != null) {
                subtitle = subTitleItem.getSubTitle();
            }

            WritableMap event = Arguments.createMap();
			event.putString("subtitle", subtitle);
			mEventEmitter.receiveEvent(getId(), Events.SRT.toString(), event);
        }
	};

	private IPolyvOnErrorListener2 mIPolyvOnErrorListener2 = new IPolyvOnErrorListener2() {

		@Override
		public boolean onError() {
			Log.i("polyv", "onError2");
			return true;
		}
	};

	private PolyvDanmakuManager.GetDanmakuListener getDanmakuListener = new PolyvDanmakuManager.GetDanmakuListener() {

        @Override
        public void fail(Throwable throwable) {
        }

        @Override
        public void success(BaseDanmakuParser baseDanmakuParser, PolyvDanmakuEntity entity) {
            iDanmakuView.prepare(baseDanmakuParser, mContext);
        }
    };

    private PolyvDanmakuManager.SendDanmakuListener sendDanmakuListener = new PolyvDanmakuManager.SendDanmakuListener() {

        @Override
        public void fail(Throwable throwable) {
        }

        @Override
        public void success(String s) {
            
        }
    };

    private DrawHandler.Callback callback = new DrawHandler.Callback() {
        @Override
        public void prepared() {
            iDanmakuView.start((long) mVideoView.getCurrentPosition());
            if (status_pause) {
            	handler.sendEmptyMessageDelayed(PAUSE, 30);
            }
                    
        }

        @Override
        public void updateTimer(DanmakuTimer danmakuTimer) {
        }

        @Override
        public void danmakuShown(BaseDanmaku baseDanmaku) {
        }

        @Override
        public void drawingFinished() {
        }
    };

	public enum Events {
		LOADING("onLoading"),
		LOADED("onLoaded"),
		SRT("onSRT"),
		PLAYING("onPlaying"),
		PAUSED("onPaused"),
		STOP("onStop"),
		ERROR("onError");

		private final String mName;

		Events(final String name) {
			mName = name;
		}

		@Override
		public String toString() {
			return mName;
		}
	}

	public ReactPlayerView(ThemedReactContext context) {
		super(context);
		mThemedReactContext = context;
		mEventEmitter = mThemedReactContext.getJSModule(RCTEventEmitter.class);
		mThemedReactContext.addLifecycleEventListener(this);

		init();
		//initDanmu();
	}

	public void setVid(String vid, int progress, int autoplay) {

		Log.i("polyv", String.format("autoplay value = %d", autoplay));
		handler.removeMessages(SHOW_PROGRESS);
		mVid = vid;

		mVideoView.pause();
		mVideoView.setVid(vid);

		this.autoplay = autoplay;
		this.progress = progress;

		//danmakuManager.getDanmaku(vid, -1, getDanmakuListener);
		//iDanmakuView.setCallback(callback);
	}

	public void setSpeed(float speed) {
		mVideoView.setSpeed(speed);
	}

	public void setMessage(String message) {
		BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BilibiliDanmakuTransfer.toBilibiliFontMode("roll"));
		if (danmaku == null || iDanmakuView == null) {
            return;
        }

        danmaku.text = message;
        danmaku.padding = 5;
        danmaku.priority = 1; // 0:可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = false;

        danmaku.setTime(iDanmakuView.getCurrentTime() + 100);
        danmaku.textSize = 16 * (mContext.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.WHITE;
        danmaku.textShadowColor = Color.BLACK;
        iDanmakuView.addDanmaku(danmaku);

        String time = "00:00:00";
        time = PolyvDanmakuTransfer.toPolyvDanmakuTime(mVideoView.getCurrentPosition());
        danmakuManager.sendDanmaku(new PolyvDanmakuInfo(mVid, message, time, "16", "roll", Color.WHITE), sendDanmakuListener);
	}

	public void setDanmu(boolean danmu) {
		if (danmu) {
			resume();
		} else {
			pause();
		}
	}

	public void setPaused(boolean paused) {
	 	if (paused) {
	 		mVideoView.pause();
	 		pause();
	 	} else {
	 		mVideoView.start();
	 		resume();
	 	}
	}

	public void setSeek(int seek) {
	 	mVideoView.pause();
	 	mVideoView.seekTo(seek * 1000);
	 	mVideoView.start();
	 	seekTo();
	}

	public void stop() {
		handler.removeMessages(SHOW_PROGRESS);
		mVideoView.pause();
		pause();
	}

	private void init() {
		inflate(mThemedReactContext.getApplicationContext(), R.layout.player, this);

		mVideoView = (PolyvVideoView)findViewById(R.id.polyv_video_view);
		mVideoView.setBackgroundColor(0);

		mVideoView.setOnPreparedListener(mIPolyvOnPreparedListener2);
		mVideoView.setOnPreloadPlayListener(mIPolyvOnPreloadPlayListener);
		mVideoView.setOnPlayPauseListener(mIPolyvOnPlayPauseListener);
		mVideoView.setOnInfoListener(mIPolyvOnInfoListener2);
		mVideoView.setOnVideoStatusListener(mIPolyvOnVideoStatusListener);
		mVideoView.setOnVideoPlayErrorListener(mIPolyvOnVideoPlayErrorListener2);
		mVideoView.setOnVideoSRTListener(mIPolyvOnVideoSRTListener);
		mVideoView.setOnErrorListener(mIPolyvOnErrorListener2);
		mVideoView.setOnCompletionListener(mIPolyvOnCompletionListener2);
	}

	private void initDanmu() {

		iDanmakuView = (IDanmakuView)findViewById(R.id.dv_danmaku);
		danmakuManager = new PolyvDanmakuManager(getContext());

		//-------------------仅对加载的弹幕有效-------------------//
        // 设置最大显示行数
		HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 5); // 滚动弹幕最大显示5行
        maxLinesPair.put(BaseDanmaku.TYPE_FIX_TOP, 2);
        maxLinesPair.put(BaseDanmaku.TYPE_FIX_BOTTOM, 2);

        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_BOTTOM, true);

        mContext = DanmakuContext.create();
        mContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 2).setDuplicateMergingEnabled(false).setScrollSpeedFactor(1.6f).setScaleTextSize(1.3f).setMaximumLines(maxLinesPair).preventOverlapping(overlappingEnablePair);
        iDanmakuView.showFPS(false);
        iDanmakuView.enableDanmakuDrawingCache(false);
	}

	private void track() {
		int position = mVideoView.getCurrentPosition();

		WritableMap event = Arguments.createMap();
		event.putInt("current", position / 1000);
		mEventEmitter.receiveEvent(getId(), Events.PLAYING.toString(), event);
		handler.sendMessageDelayed(handler.obtainMessage(SHOW_PROGRESS), 1000 - (position % 1000));
	}

	public void start() {
        if (iDanmakuView != null) {
            if (!iDanmakuView.isPrepared()) {
                iDanmakuView.setCallback(callback);
            } else {
                iDanmakuView.start((long) mVideoView.getCurrentPosition());
                if (status_pause)
                    handler.sendEmptyMessageDelayed(PAUSE, 30);
            }
        } else {
            isStart = true;
        }
    }

	//获取视频稳定后的时间再seekTo
    private void seekToFitTime() {
    	if (seekToTime == -1) {
    		seekToTime = mVideoView.getCurrentPosition();
    	}

		long currentTime = mVideoView.getCurrentPosition();
		if (currentTime < seekToTime || (updateTime != -1 && currentTime > updateTime)) {

			iDanmakuView.seekTo(currentTime);
			if (status_pause) {
				handler.sendEmptyMessageDelayed(PAUSE, 30);
			}

			seekToTime = -1;
            updateTime = -1;
		} else if (currentTime >= seekToTime) {
			updateTime = currentTime;
		}

		handler.removeMessages(SEEKTOFITTIME);
		handler.sendMessageDelayed(handler.obtainMessage(SEEKTOFITTIME), 300);
    }

    public void pause() {
        pause(true);
    }

    public void pause(boolean fromuser) {
        if (!fromuser)
            status_pause_fromuser = false;
        else
            status_canauto_resume = false;
        status_pause = true;
        if (iDanmakuView != null && iDanmakuView.isPrepared()) {
            iDanmakuView.pause();
        }
    }

    public void seekTo() {
        if (iDanmakuView != null) {
            seekToFitTime();
        }
    }

    public void resume() {
        resume(true);
    }

    public void resume(boolean fromuser) {
        if (status_pause_fromuser && fromuser || (!status_pause_fromuser && !fromuser)) {
            status_pause = false;
            if (iDanmakuView != null && iDanmakuView.isPrepared() && iDanmakuView.isPaused()) {
                if (!status_pause_fromuser) {
                    status_pause_fromuser = true;
                    seekTo();
                    if (status_canauto_resume)
                        iDanmakuView.resume();
                } else {
                    status_canauto_resume = true;
                    iDanmakuView.resume();
                }
            }
        }
    }

	@Override
	public void onHostResume() {
	 	mVideoView.resume();
	 	resume();
	}

	@Override
	public void onHostPause() {
	 	mVideoView.pause();
	 	pause();
	}

	@Override
	public void onHostDestroy() {
		mVideoView.stopPlayback();
		stop();

		if (mVideoView != null) {
			mVideoView.destroy();
		}
	}
}