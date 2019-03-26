//
//  RCTPlayer.m
//  RCTPolyv
//
//  Created by Fei Mo on 2017/11/23.
//  Copyright © 2017年 Fei Mo. All rights reserved.
//

#import "RCTPlayer.h"
#import <React/RCTBridgeModule.h>
#import <React/RCTEventDispatcher.h>
#import <React/UIView+React.h>

@implementation RCTPlayer
{
    RCTEventDispatcher *_eventDispatcher;
    PLVMoviePlayerController *player;
    UIView *maskView;
    NSTimer *playbackTimer;
    
    NSString *vid;
    NSNumber *autoplay;
    float progress;
    
    NSMutableDictionary *_parsedSrt;
    
    BOOL danmuEnabled;
    PVDanmuManager *danmuManager;
    PvDanmuSendView *danmuSendView;
    BOOL sendingDanmu;
}

- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher
{
    if ((self = [super init])) {
        _eventDispatcher = eventDispatcher;
    }
    
    return self;
}

- (void)applicationWillResignActive:(NSNotification *)notification {
    RCTLogInfo(@"active");
    
}


- (void)applicationWillEnterForeground:(NSNotification *)notification {
    RCTLogInfo(@"fg");
    [self setPaused:TRUE];
}

- (void)applicationDidEnterBackground:(NSNotification *)notification {
    RCTLogInfo(@"bg");
    [self setPaused:TRUE];
}

- (void) setSource:(NSDictionary *)source
{
    RCTLogInfo(@"init player");
    [self releasePlayer];
    
    vid = source[@"vid"];
    autoplay = source[@"autoplay"];
    progress = [source[@"progress"] floatValue];
    
    player = [[PLVMoviePlayerController alloc] initWithVid:vid level:0];
    
    player.delegate = self;
    
    [self setupUI];
    [self configObserver];
    
    if ([autoplay integerValue] == 0)
    {
        player.shouldAutoplay = FALSE;
    }
}

- (void) setDanmu:(BOOL)danmu {
    danmuEnabled = danmu;
    
    if (danmu) {
        
        [danmuManager resetDanmuWithFrame:self.reactContentFrame];
        [danmuManager initStart];
    }
}

- (void) setMessage:(NSString *)message
{
    [danmuManager pause];
    [danmuManager sendDanmu:vid msg:message time:[player currentPlaybackTime] fontSize:24 fontMode:@"roll" fontColor:@"0xFFFFFF"];
}

- (void) setSpeed:(float)speed {
    player.currentPlaybackRate = speed;
}

- (void)setPaused:(BOOL)paused {
    if (player) {
        if (paused) {
            [player pause];
        } else {
            [player play];
        }
        _paused = paused;
    }
}

- (void)setSeek:(float)seek {
    
    [player pause];
    [player setCurrentPlaybackTime:seek];
    [player play];
}

- (void)setupUI {
    
    UIView *playerView = player.view;
    [self addSubview: playerView];
    [playerView setTranslatesAutoresizingMaskIntoConstraints:NO];

    NSLayoutConstraint *centerX = [NSLayoutConstraint constraintWithItem:playerView attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeCenterX multiplier:1.0 constant:0];
    NSLayoutConstraint *centerY = [NSLayoutConstraint constraintWithItem:playerView attribute:NSLayoutAttributeCenterY relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeCenterY multiplier:1.0 constant:0];
    NSLayoutConstraint *width = [NSLayoutConstraint constraintWithItem:playerView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeWidth multiplier:1.0 constant:0];
    NSLayoutConstraint *height = [NSLayoutConstraint constraintWithItem:playerView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeHeight multiplier:1.0 constant:0];
    
    NSArray *constraints = [NSArray arrayWithObjects:centerX, centerY,width,height, nil];
    [self addConstraints: constraints];
    
    maskView = [[UIView alloc] init];
    [self addSubview: maskView];
    [maskView setTranslatesAutoresizingMaskIntoConstraints:NO];
    
    NSLayoutConstraint *tcenterX = [NSLayoutConstraint constraintWithItem:maskView attribute:NSLayoutAttributeCenterX relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeCenterX multiplier:1.0 constant:0];
    NSLayoutConstraint *tcenterY = [NSLayoutConstraint constraintWithItem:maskView attribute:NSLayoutAttributeCenterY relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeCenterY multiplier:1.0 constant:0];
    NSLayoutConstraint *twidth = [NSLayoutConstraint constraintWithItem:maskView attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeWidth multiplier:1.0 constant:0];
    NSLayoutConstraint *theight = [NSLayoutConstraint constraintWithItem:maskView attribute:NSLayoutAttributeHeight relatedBy:NSLayoutRelationEqual toItem:self attribute:NSLayoutAttributeHeight multiplier:1.0 constant:0];
    
    NSArray *tconstraints = [NSArray arrayWithObjects:tcenterX, tcenterY, twidth,theight, nil];
    [self addConstraints: tconstraints];
    
    
    
}

- (void)configObserver {
    NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
    // 播放状态改变，可配合playbakcState属性获取具体状态
    [notificationCenter addObserver:self selector:@selector(onMPMoviePlayerPlaybackStateDidChangeNotification)
                               name:MPMoviePlayerPlaybackStateDidChangeNotification object:nil];
    // 媒体网络加载状态改变
    [notificationCenter addObserver:self selector:@selector(onMPMoviePlayerLoadStateDidChangeNotification)
                               name:MPMoviePlayerLoadStateDidChangeNotification object:nil];
    
    // 播放时长可用
    [notificationCenter addObserver:self selector:@selector(onMPMovieDurationAvailableNotification)
                               name:MPMovieDurationAvailableNotification object:nil];
    // 媒体播放完成或用户手动退出, 具体原因通过MPMoviePlayerPlaybackDidFinishReasonUserInfoKey key值确定
    [notificationCenter addObserver:self selector:@selector(onMPMoviePlayerPlaybackDidFinishNotification:)
                               name:MPMoviePlayerPlaybackDidFinishNotification object:nil];
    // 视频就绪状态改变
    [notificationCenter addObserver:self selector:@selector(onMediaPlaybackIsPreparedToPlayDidChangeNotification)
                               name:MPMediaPlaybackIsPreparedToPlayDidChangeNotification object:nil];
    
    // 播放资源变化
    [notificationCenter addObserver:self selector:@selector(onMPMoviePlayerNowPlayingMovieDidChangeNotification)
                               name:MPMoviePlayerNowPlayingMovieDidChangeNotification object:nil];
    
    //[notificationCenter addObserver:self selector:@selector(applicationWillResignActive:) name:UIApplicationWillResignActiveNotification object:nil];
    
    //[notificationCenter addObserver:self selector:@selector(applicationWillEnterForeground:) name:UIApplicationWillEnterForegroundNotification object:nil];
    
    //[notificationCenter addObserver:self selector:@selector(applicationDidEnterBackground:) name:UIApplicationDidEnterBackgroundNotification object:nil];
}

#pragma Movie Player delegate
- (void)moviePlayer:(PLVMoviePlayerController *)player didLoadVideoInfo:(PvVideo *)video {
    RCTLogInfo(@"didLoadVideoInfo");
    
    _parsedSrt = nil;
    
    [self parseSubRip];
}

- (void)parseSubRip {
    _parsedSrt = [NSMutableDictionary new];
    
    NSString *val = nil;
    NSArray *values = [player.video.videoSrts allValues];
    
    RCTLogInfo(@"srts length %lu", [values count]);
    
    if ([values count] != 0) {
        //暂时只选择第一条字幕
        val = [values objectAtIndex:0];
    }
    if (!val) {
        return;
    }
    NSString *string = [NSString stringWithContentsOfURL:[NSURL URLWithString:val] encoding:NSUTF8StringEncoding error:NULL];
    if (string == nil) {
        return;
    }
    
    string = [string stringByReplacingOccurrencesOfString:@"\n\r\n" withString:@"\n\n"];
    string = [string stringByReplacingOccurrencesOfString:@"\n\n\n" withString:@"\n\n"];
    
    NSScanner *scanner = [NSScanner scannerWithString:string];
    
    while (![scanner isAtEnd])
    {
        @autoreleasepool
        {
            NSString *indexString;
            (void) [scanner scanUpToCharactersFromSet:[NSCharacterSet newlineCharacterSet] intoString:&indexString];
            
            NSString *startString;
            (void) [scanner scanUpToString:@" --> " intoString:&startString];
            NSScanner *aScanner = [NSScanner scannerWithString:startString];
            
            NSTimeInterval h =  0.0;
            NSTimeInterval m =  0.0;
            NSTimeInterval s =  0.0;
            NSTimeInterval c =  0.0;
            
            [aScanner scanDouble:&h];
            [aScanner scanString:@":" intoString:NULL];
            [aScanner scanDouble:&m];
            [aScanner scanString:@":" intoString:NULL];
            
            [aScanner scanDouble:&s];
            [aScanner scanString:@"," intoString:NULL];
            [aScanner scanDouble:&c];
            double fromTime = (h * 3600.0) + (m * 60.0) + s + (c / 1000.0);
            
            
            (void) [scanner scanString:@"-->" intoString:NULL];
            
            NSString *endString;
            (void) [scanner scanUpToCharactersFromSet:[NSCharacterSet newlineCharacterSet] intoString:&endString];
            aScanner = [NSScanner scannerWithString:endString];
            [aScanner scanDouble:&h];
            [aScanner scanString:@":" intoString:NULL];
            [aScanner scanDouble:&m];
            [aScanner scanString:@":" intoString:NULL];
            
            [aScanner scanDouble:&s];
            [aScanner scanString:@"," intoString:NULL];
            [aScanner scanDouble:&c];
            double endTime = (h * 3600.0) + (m * 60.0) + s + (c / 1000.0);
            
            NSString *textString = @"";
            // BEGIN EDIT
            (void) [scanner scanUpToString:@"\n\n" intoString:&textString];
            
            textString = [textString stringByReplacingOccurrencesOfString:@"\r\n" withString:@" "];
            textString = [textString stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
            // END EDIT
            
            NSMutableDictionary *dictionary = [NSMutableDictionary new];
            [dictionary setObject:[NSNumber numberWithDouble:fromTime] forKey:@"from"];
            [dictionary setObject:[NSNumber numberWithDouble:endTime] forKey:@"to"];
            [dictionary setObject:textString forKey:@"text"];
            
            [_parsedSrt setObject:dictionary forKey:indexString];
        }
    }
}

// 播放状态改变
- (void)onMPMoviePlayerPlaybackStateDidChangeNotification {
    
    if (player.playbackState == MPMoviePlaybackStatePlaying) {
        RCTLogInfo(@"playing");
        
        if (self.onPlaying) {
            self.onPlaying(@{ @"target": self.reactTag,
                              @"current" : [NSNumber numberWithDouble:player.currentPlaybackTime],
                              @"duration": [NSNumber numberWithDouble:player.duration]});
        }
        _paused = NO;
        [self startPlaybackTimer];
    } else if (player.playbackState == MPMoviePlaybackStatePaused) {
        RCTLogInfo(@"pause");
        if (self.onPaused) {
            self.onPaused(@{ @"target": self.reactTag});
        }
        _paused = YES;
        [self releaseTimer];
    } else if (player.playbackState == MPMoviePlaybackStateStopped) {
        RCTLogInfo(@"backstop");
    } else {
        RCTLogInfo(@"unkown");
    }
}

// 网络加载状态改变
- (void)onMPMoviePlayerLoadStateDidChangeNotification {
    
    RCTLogInfo(@"load status");
    if (player.loadState & MPMovieLoadStateStalled) {
        if (self.onLoading) {
            self.onLoading(@{ @"target": self.reactTag});
        }
        
    } else if (player.loadState & MPMovieLoadStatePlaythroughOK) {
        RCTLogInfo(@"can play");
    } else if (player.loadState & MPMovieLoadStatePlayable) {
        RCTLogInfo(@"playable");
    } else {
        if (self.onError) {
            self.onError(@{ @"target": self.reactTag});
        }
    }
}

// 成功获取视频时长
- (void)onMPMovieDurationAvailableNotification {
    
    if (self.onLoaded) {
        RCTLogInfo(@"progress %f", progress);
        RCTLogInfo(@"seek");
        
        if (progress > 0) {
            
            [self setSeek: progress * player.duration / 100.0];
            progress = 0;
        }
        
        self.onLoaded(@{ @"target": self.reactTag,
                         @"duration": [NSNumber numberWithDouble:player.duration]});
        
        //danmuEnabled = TRUE;
        //CGRect dmFrame = self.reactContentFrame;
        //danmuManager = [[PVDanmuManager alloc] initWithFrame:dmFrame withVid:vid inView:self underView:maskView durationTime:1];
        
        //[danmuManager resetDanmuWithFrame:self.frame];
        //[danmuManager initStart];
    }
}

// 播放完成或退出
- (void)onMPMoviePlayerPlaybackDidFinishNotification:(NSNotification *)notification {
    MPMovieFinishReason finishReason = [notification.userInfo[MPMoviePlayerPlaybackDidFinishReasonUserInfoKey] integerValue];
    RCTLogInfo(@"stop");
    [self releaseTimer];
    
    if (self.onStop) {
        self.onStop(@{ @"target": self.reactTag});
    }
}

// 做好播放准备后
- (void)onMediaPlaybackIsPreparedToPlayDidChangeNotification {
    RCTLogInfo(@"prepare");
}

// 播放资源变化
- (void)onMPMoviePlayerNowPlayingMovieDidChangeNotification {
    RCTLogInfo(@"change");
}

- (void)startPlaybackTimer {
    if (!playbackTimer) {
        playbackTimer = [NSTimer scheduledTimerWithTimeInterval:0.5 target:self selector:@selector(monitorVideoPlayback) userInfo:nil repeats:YES];
        [[NSRunLoop currentRunLoop] addTimer:playbackTimer forMode:NSDefaultRunLoopMode];
    }
}

- (void)monitorVideoPlayback {
    
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"%K <= %f AND %K >= %f", @"from", player.currentPlaybackTime, @"to", player.currentPlaybackTime];
    
    NSString *subtitle = @"";
    NSArray *values = [_parsedSrt allValues];
    
    if ([values count] > 0) {
        NSArray *search = [values filteredArrayUsingPredicate:predicate];
        if ([search count] > 0) {
            NSDictionary *result =  [search objectAtIndex:0];
            subtitle = [result objectForKey:@"text"];
        } else {
            subtitle = @"";
        }
    }
    
    if (self.onSRT) {
        self.onSRT(@{ @"subtitle": subtitle,});
    }
    
    if (self.onPlaying) {
        self.onPlaying(@{ @"target": self.reactTag,
                          @"current" : [NSNumber numberWithDouble:player.currentPlaybackTime],
                          @"duration": [NSNumber numberWithDouble:player.duration]});
    }
    
    //if (danmuEnabled) {
    //    [danmuManager rollDanmu:player.currentPlaybackTime];
    //}
}

- (void)willMoveToWindow:(UIWindow *)newWindow {
    
    RCTLogInfo(@"willmovetowindow");
    
    if(!newWindow) {
        [self releasePlayer];
    }
}

- (void)willMoveToSuperview:(UIView *)newSuperview {
    
    RCTLogInfo(@"willmovetoview");
    
    if(!newSuperview) {
        [self releasePlayer];
    }
}

- (void)removeFromSuperview
{
    RCTLogInfo(@"remove");
    [self releasePlayer];
    
    [super removeFromSuperview];
}

- (void)dealloc
{
    RCTLogInfo(@"release");
    [self releasePlayer];
}

- (void)releaseTimer {
    if (playbackTimer) {
        [playbackTimer invalidate];
        playbackTimer = nil;
    }
}

- (void)releasePlayer {
    if(player) {
        [self releaseTimer];
        [player stop];
        player = nil;
    }
    
    RCTLogInfo(@"release");
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end
