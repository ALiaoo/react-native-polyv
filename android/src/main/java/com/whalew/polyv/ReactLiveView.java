package com.whalew.polyv;

import java.util.HashMap;

import android.os.Build;
import android.graphics.Color;
import android.util.Log;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.LifecycleEventListener;

import com.easefun.polyvsdk.live.video.PolyvLiveVideoView;
import com.easefun.polyvsdk.live.video.PolyvLiveVideoViewListener;
import com.easefun.polyvsdk.live.video.PolyvLivePlayErrorReason;
import com.easefun.polyvsdk.live.chat.PolyvChatManager;
import com.easefun.polyvsdk.live.chat.PolyvChatMessage;
import com.easefun.polyvsdk.live.chat.playback.api.PolyvLive_Status;
import com.easefun.polyvsdk.live.chat.playback.api.listener.PolyvLive_StatusNorListener;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public class ReactLiveView extends FrameLayout implements LifecycleEventListener {

	private ThemedReactContext mThemedReactContext;
	private RCTEventEmitter mEventEmitter;
	private PolyvLiveVideoView mVideoView = null;

	private PolyvLive_Status live_status;
	private PolyvChatManager chatManager;
	private String userId = "e70e0pin2a";
	private String channelId;

	private static boolean status_canauto_resume = true;
    private static boolean status_pause_fromuser = true;

    private IDanmakuView mDanmakuView;
    private BaseDanmakuParser mParser;
    private DanmakuContext mContext;


	private PolyvChatManager.ChatManagerListener mChatManagerListener = new PolyvChatManager.ChatManagerListener() {

		@Override
		public void connectStatus(PolyvChatManager.ConnectStatus connect_status) {
		
		}

		@Override
		public void receiveChatMessage(PolyvChatMessage chatMessage) {

			if (chatMessage.getChatType() == PolyvChatMessage.CHATTYPE_RECEIVE) {
				sendDanmaku(chatMessage.getValues()[0]);
			}

			WritableMap event = Arguments.createMap();
			event.putString("messageType", "PLVChatMessageTypeSpeak");
			mEventEmitter.receiveEvent(getId(), Events.RECEIVED.toString(), event);
		}
	};

	private PolyvLiveVideoViewListener.OnPreparedListener mOnPreparedListener = new PolyvLiveVideoViewListener.OnPreparedListener() {

		@Override
		public void onPrepared() {
			Log.i("polyv", "LOADED");
			mEventEmitter.receiveEvent(getId(), Events.LOADED.toString(), Arguments.createMap());
		}
	};

	private PolyvLiveVideoViewListener.OnVideoPlayErrorListener mOnVideoPlayErrorListener = new PolyvLiveVideoViewListener.OnVideoPlayErrorListener() {

		@Override
		public void onVideoPlayError(@NonNull PolyvLivePlayErrorReason errorReason) {
			Log.i("polyv", "ERROR1");
			mEventEmitter.receiveEvent(getId(), Events.ERROR.toString(), Arguments.createMap());
		}
	};

	private PolyvLiveVideoViewListener.OnErrorListener mOnErrorListener = new PolyvLiveVideoViewListener.OnErrorListener() {

		@Override
		public void onError() {
			Log.i("polyv", "ERROR");
			mEventEmitter.receiveEvent(getId(), Events.ERROR.toString(), Arguments.createMap());
		}
	};

	private PolyvLiveVideoViewListener.OnWillPlayWaittingListener mOnWillPlayWaittingListener = new PolyvLiveVideoViewListener.OnWillPlayWaittingListener() {

		 @Override
		 public void onWillPlayWaitting(boolean isCoverImage) {

		 	Log.i("polyv", "NO LIVE");
		 	mEventEmitter.receiveEvent(getId(), Events.STOP.toString(), Arguments.createMap());
		 	checkStatus();
		 }
	};

	private PolyvLiveVideoViewListener.OnNoLiveAtPresentListener mOnNoLiveAtPresentListener = new PolyvLiveVideoViewListener.OnNoLiveAtPresentListener() {

		@Override
		public void onNoLiveAtPresent() {
			Log.i("polyv", "NO LIVE1");
			mEventEmitter.receiveEvent(getId(), Events.STOP.toString(), Arguments.createMap());
			checkStatus();
		}
	};

	public enum Events {
		LOADED("onLoaded"),
		PLAYING("onPlaying"),
		RECEIVED("onReceiveMessage"),
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

	public ReactLiveView(ThemedReactContext context) {

		super(context);
		mThemedReactContext = context;
		mEventEmitter = mThemedReactContext.getJSModule(RCTEventEmitter.class);
		mThemedReactContext.addLifecycleEventListener(this);

		init();
		initDanmu();
	}

	private void init() {

		inflate(mThemedReactContext.getApplicationContext(), R.layout.live, this);
		mVideoView = (PolyvLiveVideoView)findViewById(R.id.polyv_video_view);

		mVideoView.setOpenWait(true);
        mVideoView.setOpenPreload(true, 2);

		mVideoView.setOnPreparedListener(mOnPreparedListener);
		mVideoView.setOnVideoPlayErrorListener(mOnVideoPlayErrorListener);
		mVideoView.setOnErrorListener(mOnErrorListener);
		mVideoView.setOnWillPlayWaittingListener(mOnWillPlayWaittingListener);
		mVideoView.setOnNoLiveAtPresentListener(mOnNoLiveAtPresentListener);

		chatManager = new PolyvChatManager();
		chatManager.setOnChatManagerListener(mChatManagerListener);
	}

	private void initDanmu() {

		mDanmakuView = (IDanmakuView)findViewById(R.id.dv_danmaku);

		//-------------------仅对加载的弹幕有效-------------------//
        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 2); // 滚动弹幕最大显示5行
        maxLinesPair.put(BaseDanmaku.TYPE_FIX_TOP, 2);
        maxLinesPair.put(BaseDanmaku.TYPE_FIX_BOTTOM, 2);

        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_BOTTOM, true);
        //--------------------------------------------------------//

        mContext = DanmakuContext.create();
        mContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3).setDuplicateMergingEnabled(false).setScrollSpeedFactor(1.2f).setScaleTextSize(1.0f).setMaximumLines(maxLinesPair).preventOverlapping(overlappingEnablePair);
        mDanmakuView.showFPS(false);
        mDanmakuView.enableDanmakuDrawingCache(false);

        mDanmakuView.setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                mDanmakuView.start();
            }

            @Override
            public void updateTimer(DanmakuTimer timer) {
            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {
            }

            @Override
            public void drawingFinished() {
            }
        });

        mDanmakuView.prepare(mParser = new BaseDanmakuParser() {
            @Override
            protected IDanmakus parse() {
                return new Danmakus();
            }
        }, mContext);
	}

	private void checkStatus() {
		Log.i("polyv", "INIT CHECK");
		if (live_status == null) live_status = new PolyvLive_Status();
		live_status.shutdownSchedule();

		live_status.getLive_Status(channelId, 6000, 4000, new PolyvLive_StatusNorListener() {

			@Override
			public void success(boolean isLiving, final boolean isPPTLive) {
				Log.i("polyv", "LIVE");
				if (isLiving) {
					live_status.shutdownSchedule();
					mEventEmitter.receiveEvent(getId(), Events.PLAYING.toString(), Arguments.createMap());
					mVideoView.setLivePlay(userId, channelId, false);
				}
			}

			@Override
			public void fail(String failTips, int code) {
				Log.i("polyv", "ERROR");
			}
		});
	}

	public void join(String channelId, String nickName, String avatar) {
		channelId = channelId;
		
		String chatUserId = Build.SERIAL;

		chatManager.login(chatUserId, channelId, nickName, avatar);
		mVideoView.setLivePlay(userId, channelId, false);
	}

	public void setMessage(String message) {
		final PolyvChatMessage msg = new PolyvChatMessage(message);
		chatManager.sendChatMessage(msg);
		sendDanmaku(msg.getValues()[0]);
	}

	public void setPaused(boolean paused) {

		if (paused) {
			mVideoView.pause();
		} else {
			mVideoView.resume();
		}
	}

	public void setDanmu(boolean danmu) {
		if (danmu) {
			show();
		} else {
			hide();
		}
	}

	//隐藏
    public void hide() {
        if (mDanmakuView != null) {
            mDanmakuView.hide();
        }
    }

    //显示
    public void show() {
        if (mDanmakuView != null) {
            mDanmakuView.show();
        }
    }

    //暂停
    public void pause() {
        pause(true);
    }

    public void pause(boolean fromuser) {
        if (!fromuser)
            status_pause_fromuser = false;
        else
            status_canauto_resume = false;
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();
        }
    }

    //恢复
    public void resume() {
        resume(true);
    }

    public void resume(boolean fromuser) {
        if (status_pause_fromuser && fromuser || (!status_pause_fromuser && !fromuser)) {
            if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
                if (!status_pause_fromuser) {
                    status_pause_fromuser = true;
                    if (status_canauto_resume)
                        mDanmakuView.resume();
                } else {
                    status_canauto_resume = true;
                    mDanmakuView.resume();
                }
            }
        }
    }

    //发送
    public void sendDanmaku(CharSequence message) {
        BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        danmaku.text = message;
        danmaku.padding = 0;
        danmaku.priority = 1; // 一定会显示, 一般用于本机发送的弹幕
        danmaku.setTime(mDanmakuView.getCurrentTime() + 100);
        danmaku.textSize = 16 * (mContext.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.WHITE;
        mDanmakuView.addDanmaku(danmaku);
    }

    //释放
    private void release() {
        if (mDanmakuView != null) {
            mDanmakuView.release();
            mDanmakuView = null;
        }
    }

	public void stop() {

		if (live_status != null) {
			live_status.shutdownSchedule();
		}

		chatManager.disconnect();
		
		if (mVideoView != null) {
			mVideoView.destroy();
		}
	}

	@Override
	public void onHostResume() {

	}

	@Override
	public void onHostPause() {

	}

	@Override
	public void onHostDestroy() {
		if (mVideoView != null) {
			mVideoView.destroy();
		}
	}
}