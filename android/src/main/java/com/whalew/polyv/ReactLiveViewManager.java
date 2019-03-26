package com.whalew.polyv;

import java.util.Map;
import javax.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import com.whalew.polyv.ReactLiveView.Events;

public class ReactLiveViewManager extends SimpleViewManager<ReactLiveView>{

	public static final String REACT_CLASS = "RCTLive";

	@Override
	public String getName() {
		return REACT_CLASS;
	}

	@Override
	@Nullable
	public Map getExportedCustomDirectEventTypeConstants() {
		MapBuilder.Builder builder = MapBuilder.builder();
		for (Events event : Events.values()) {
			builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
		}

		return builder.build();
	}

	@Override
	protected ReactLiveView createViewInstance(ThemedReactContext reactContext) {
		return new ReactLiveView(reactContext);
	}

	@Override
    public void onDropViewInstance(ReactLiveView mVideoView) {
		super.onDropViewInstance(mVideoView);

		mVideoView.stop();
    }

	@ReactProp(name = "source")
	public void setSource(ReactLiveView mVideoView, ReadableMap source) {

		String channelId = source.getString("channel");
		String nickName = source.getString("nickname");
		String avatar = source.getString("avatar");
		
		mVideoView.join(channelId, nickName, avatar);
	}

	@ReactProp(name = "message")
	public void setMessage(ReactLiveView mVideoView, String message) {
		mVideoView.setMessage(message);
	}

	@ReactProp(name = "paused")
	public void setPaused(ReactLiveView mVideoView,  boolean paused) {
	 	mVideoView.setPaused(paused);
	}

	@ReactProp(name = "danmu")
	public void setDanmu(ReactLiveView mVideoView,  boolean danmu) {
	 	mVideoView.setDanmu(danmu);
	}
}