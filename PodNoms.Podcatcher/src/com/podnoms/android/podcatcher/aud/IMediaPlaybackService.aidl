package com.podnoms.android.podcatcher.aud;

import android.graphics.Bitmap;

interface IMediaPlaybackService {
    void openFile(String path);
    boolean isPlaying();
    void stop();
    void pause();
    void play();
    long duration();
    long position();
    long seek(long pos);
    void jumpForward(long positionOverride, int milliseconds);
    void jumpBackward(long positionOverride, int milliseconds);
    void setTrackTitle(String title);
    void setTrackDescription(String description);
    long getAudioId();
    void setAudioId(long audioId);
    void setAudioSessionId(int audioSessionId);
    int getAudioSessionId();
    String getTrackTitle();
    String getTrackDescription();
    String getPath();
    int getMediaMountedCount();
}

