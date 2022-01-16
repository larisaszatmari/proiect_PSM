package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SongChangeListener {

    private final List<MusicList> musicLists = new ArrayList<>();
    private RecyclerView musicRecyclerView;
    private MediaPlayer mediaPlayer;
    private TextView endTime, startTime;
    private boolean isPlaying = false;
    private SeekBar playerSeekBar;
    private ImageView playPauseImg;
    private Timer timer;
    //private int currentSongListPosition = 0;
    private MusicList currentSong;
    private MusicAdapter musicAdapter;

    private boolean adapterPrepared = false;
    private boolean autoPlay = true;

    private SearchView searchView = null;

    private Thread songThread = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decodeView = getWindow().getDecorView();
        int options = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decodeView.setSystemUiVisibility(options);
        setContentView(R.layout.activity_main);


        //final LinearLayout searchBtn = findViewById(R.id.searchBtn);
        //inal LinearLayout menuBtn = findViewById(R.id.menuBtn);
        musicRecyclerView = findViewById(R.id.musicReciclerView);
        final CardView playPauseCard = findViewById(R.id.playPauseCard);
        playPauseImg = findViewById(R.id.playPauseImg);
        final ImageView nextBtn = findViewById(R.id.nextBtn);
        final ImageView previousBtn = findViewById(R.id.previousBtn);

        playerSeekBar = findViewById(R.id.playerSeekBar);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        musicRecyclerView.setHasFixedSize(true);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mediaPlayer = new MediaPlayer();



        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            //getMusicFiles(musicFilter);
            getMusicFiles();
        }else
            {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 11);
                }
                else{
                    //getMusicFiles(musicFilter);
                    getMusicFiles();
                }
            }


                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onChanged(getNextSong(currentSong));

                        nextBtn.setEnabled(false);
                        Timer delayTimer = new Timer();
                        delayTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        nextBtn.setEnabled(true);
                                    }
                                });
                            }
                        }, 1000, 500);
                    }
                });

                previousBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onChanged(getPrevSong(currentSong));

                        previousBtn.setEnabled(false);
                        Timer delayTimer = new Timer();
                        delayTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        previousBtn.setEnabled(true);
                                    }
                                });
                            }
                        }, 1000, 500);

                    }
                });

                playPauseCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (isPlaying){
                            isPlaying = false;

                            mediaPlayer.pause();
                            playPauseImg.setImageResource(R.drawable.play_icon);
                        }else{
                            isPlaying = true;
                            mediaPlayer.start();
                            playPauseImg.setImageResource(R.drawable.pause_btn);
                        }

                    }
                });

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    if(progress >= seekBar.getMax()) progress-=1;
                    mediaPlayer.seekTo(progress);
                    /*if(isPlaying){
                        mediaPlayer.seekTo(progress);
                    }
                    else{
                        mediaPlayer.seekTo(0);
                    }*/
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    private void getMusicFiles(){

        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, MediaStore.Audio.Media.DATA+" LIKE?",
                new String[]{"%.mp3%"}, null);

        if(cursor == null){
            Toast.makeText(this, "Something went wrong!!!", Toast.LENGTH_SHORT).show();
        }
        else if(!cursor.moveToNext()){
            Toast.makeText(this, "No music Found", Toast.LENGTH_SHORT).show();
        }
        else{
            while(cursor.moveToNext()){
                final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                final String getArtistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId );
                String getDuration = "00:00";

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){

                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                }

                final MusicList musicList = new MusicList(getMusicFileName, getArtistName, getDuration, false, musicFileUri);
                musicLists.add(musicList);
            }

            musicAdapter = new MusicAdapter(musicLists, MainActivity.this);
            musicRecyclerView.setAdapter(musicAdapter);
        }
        cursor.close();

    }

    private boolean songExists(MusicList song) {
        return musicLists.contains((song));
    }

    private MusicList getNextSong(MusicList song) {
        return musicLists.get((musicLists.indexOf(song)+1)%musicLists.size());
    }

    private MusicList getPrevSong(MusicList song) {
        if(musicLists.indexOf(song)==0) return musicLists.get(musicLists.size()-1);
        return musicLists.get((musicLists.indexOf(song)-1)%musicLists.size());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                musicAdapter.getFilter().filter(newText);
                return false;
            }
        });
        /*searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                View title = findViewById(R.id.titleText);
                if(hasFocus) {
                    title.setVisibility(View.INVISIBLE);
                }
                else
                {
                    title.setVisibility(View.VISIBLE);
                    searchView.setQuery("", false);
                }
            }
        });*/

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getMusicFiles();
        } else {
            Toast.makeText(this, "Permission Declined By User", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus){
            View decodeView = getWindow().getDecorView();
            int options = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decodeView.setSystemUiVisibility(options);
        }
    }

    @Override
    public void onChanged(MusicList song) {

        //currentSongListPosition = position;
        if(currentSong!=null) currentSong.setPlaying(false);
        currentSong = song;
        musicAdapter.updateList(musicLists);
        musicRecyclerView.scrollToPosition(musicLists.indexOf(currentSong));
        currentSong.setPlaying(true);
        //if(mediaPlayer.isPlaying()){
        mediaPlayer.pause();
        mediaPlayer.reset();
        adapterPrepared = false;
        //}

        searchView.setQuery("", false);
        searchView.clearFocus();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        if(songThread != null) {
            songThread.interrupt();
        }
        songThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer.setDataSource(MainActivity.this, currentSong.getMusicFile());
                    mediaPlayer.prepare();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,"Unable to play track", Toast.LENGTH_SHORT).show();
                }

                if(Thread.interrupted()) {
                    mediaPlayer.reset();
                }
            }
        });

        songThread.start();


        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                final int getTotalDuration = mp.getDuration();

                String generateDuration = String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(getTotalDuration)%60,
                        TimeUnit.MILLISECONDS.toSeconds(getTotalDuration)%60);

                endTime.setText(generateDuration);
                isPlaying = true;
                mp.start();
                playerSeekBar.setMax(getTotalDuration);
                playPauseImg.setImageResource(R.drawable.pause_btn);

                adapterPrepared = true;
            }
        });

        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!adapterPrepared) return;
                        final int getCurrentDuration = mediaPlayer.getCurrentPosition();

                        String generateDuration = String.format(Locale.getDefault(), "%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration)%60,
                                TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration)%60);

                        playerSeekBar.setProgress(getCurrentDuration);
                        startTime.setText(generateDuration);
                    }
                });

            }
        }, 1000, 1000);


        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.reset();
                timer.purge();
                timer.cancel();
                isPlaying = false;
                playPauseImg.setImageResource(R.drawable.play_icon);
                playerSeekBar.setProgress(0);
                if(autoPlay) {
                    onChanged(getNextSong(currentSong));
                }
            }
        });

    }
}