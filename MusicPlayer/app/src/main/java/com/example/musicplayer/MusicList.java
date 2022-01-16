package com.example.musicplayer;

import android.net.Uri;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicList {

    private String title, artist, duration;
    private boolean isPlaying;
    private Uri musicFile;

    public MusicList(String title, String artist, String duration, boolean isPlaying, Uri musicFile) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.isPlaying = isPlaying;
        this.musicFile = musicFile;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getDuration() {
        return duration;
    }

    public String getDurationAsString() {
        int durationAsNumber = Integer.parseInt(duration);
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(durationAsNumber)%60,
                TimeUnit.MILLISECONDS.toSeconds(durationAsNumber)%60);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Uri getMusicFile() {
        return musicFile;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
}
