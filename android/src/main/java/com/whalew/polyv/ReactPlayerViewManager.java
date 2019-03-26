package com.whalew.polyv;

import java.util.Map;
import javax.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import com.whalew.polyv.ReactPlayerView.Events;


public class ReactPlayerViewManager extends SimpleViewManager<ReactPlayerView> {

	public static final String REACT_CLASS = "RCTPlayer";

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
	protected ReactPlayerView createViewInstance(ThemedReactContext reactContext) {

		return new ReactPlayerView(reactContext);
	}

	@Override
    public void onDropViewInstance(ReactPlayerView mVideoView) {
		super.onDropViewInstance(mVideoView);

		mVideoView.stop();
    }


	@ReactProp(name = "source")
	public void setSource(ReactPlayerView mVideoView, ReadableMap source) {
		String vid = source.getString("vid");
		int progress = source.getInt("progress");
		int autoplay = source.getInt("autoplay");

		mVideoView.setVid(vid, progress, autoplay);
	}

	@ReactProp(name = "speed")
	public void setSpeed(ReactPlayerView mVideoView, float speed) {
		mVideoView.setSpeed(speed);
	}

	@ReactProp(name = "message")
	public void setMessage(ReactPlayerView mVideoView, String message) {
		mVideoView.setMessage(message);
	}

	@ReactProp(name = "danmu")
	public void setDanmu(ReactPlayerView mVideoView,  boolean danmu) {
	 	mVideoView.setDanmu(danmu);
	}

	@ReactProp(name = "paused")
	public void setPaused(ReactPlayerView mVideoView,  boolean paused) {
	 	mVideoView.setPaused(paused);
	}

	@ReactProp(name = "seek")
	public void setSeek(ReactPlayerView mVideoView,  int seek) {
	 	mVideoView.setSeek(seek);
	}
}