import React, { Component } from "react";

import { requireNativeComponent } from "react-native";

const PropTypes = require("prop-types");

class Player extends Component {
  constructor(props) {
    super(props);

    this.paused = this.paused.bind(this);
    this.seek = this.seek.bind(this);
    this._assignRoot = this._assignRoot.bind(this);
    this._onLoading = this._onLoading.bind(this);
    this._onLoaded = this._onLoaded.bind(this);
    this._onPaused = this._onPaused.bind(this);
    this._onStop = this._onStop.bind(this);
    this._onError = this._onError.bind(this);
    this._onPlaying = this._onPlaying.bind(this);
    this._onSRT = this._onSRT.bind(this);
  }

  _assignRoot(component) {
    this._root = component;
  }

  setNativeProps(nativeProps) {
    this._root.setNativeProps(nativeProps);
  }

  speed(speed) {
    this.setNativeProps({ speed: speed });
  }

  paused(paused) {
    this.setNativeProps({ paused: paused });
  }

  seek(seek) {
    this.setNativeProps({ seek: seek });
  }

  danmu(danmu) {
    this.setNativeProps({ danmu: danmu });
  }

  message(message) {
    this.setNativeProps({ message: message });
  }

  _onLoading(event) {
    this.props.onLoading && this.props.onLoading(event.nativeEvent);
  }

  _onLoaded(event) {
    this.props.onLoaded && this.props.onLoaded(event.nativeEvent);
  }

  _onPlaying(event) {
    this.props.onPlaying && this.props.onPlaying(event.nativeEvent);
  }

  _onSRT(event) {
    this.props.onSRT && this.props.onSRT(event.nativeEvent);
  }

  _onPaused(event) {
    this.props.onPaused && this.props.onPaused(event.nativeEvent);
  }

  _onStop(event) {
    this.props.onStop && this.props.onStop(event.nativeEvent);
  }

  _onError(event) {
    this.props.onError && this.props.onError(event.nativeEvent);
  }

  render() {
    const nativeProps = Object.assign({}, this.props);

    Object.assign(nativeProps, {
      onLoading: this._onLoading,
      onLoaded: this._onLoaded,
      onPaused: this._onPaused,
      onStop: this._onStop,
      onError: this._onError,
      onPlaying: this._onPlaying,
      onSRT: this._onSRT
    });

    return <RCTPlayer ref={this._assignRoot} {...nativeProps} />;
  }
}

Player.propTypes = {
  source: PropTypes.shape({
    vid: PropTypes.string.isRequired,
    autoplay: PropTypes.number.isRequired,
    progress: PropTypes.number.isRequired
  }),
  speed: PropTypes.number,
  paused: PropTypes.bool,
  seek: PropTypes.number,
  danmu: PropTypes.bool,
  message: PropTypes.string,
  onLoading: PropTypes.func,
  onLoaded: PropTypes.func,
  onPaused: PropTypes.func,
  onStop: PropTypes.func,
  onError: PropTypes.func,
  onPlaying: PropTypes.func,
  onSRT: PropTypes.func
};

const RCTPlayer = requireNativeComponent("RCTPlayer", Player, {
  nativeOnly: {
    testID: true,
    accessibilityComponentType: true,
    renderToHardwareTextureAndroid: true,
    accessibilityLabel: true,
    accessibilityLiveRegion: true,
    importantForAccessibility: true,
    onLayout: true,
    nativeID: true
  }
});

module.exports = Player;
