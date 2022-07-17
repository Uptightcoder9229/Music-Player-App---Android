package com.example.musicplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.palette.graphics.Palette;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import static com.example.musicplayer.AlbumDetailsAdapter.albumFiles;
import static com.example.musicplayer.ApplicationClass.ACTION_NEXT;
import static com.example.musicplayer.ApplicationClass.ACTION_PLAY;
import static com.example.musicplayer.ApplicationClass.ACTION_PREVIOUS;
import static com.example.musicplayer.ApplicationClass.CHANNEL_ID_1;
import static com.example.musicplayer.ApplicationClass.CHANNEL_ID_2;
import static com.example.musicplayer.MainActivity.musicFiles;
import static com.example.musicplayer.MainActivity.repeatBoolean;
import static com.example.musicplayer.MainActivity.shuffleBoolean;
import static com.example.musicplayer.MusicAdapter.mFiles;

public class PlayerActivity extends AppCompatActivity
        implements ActionPlaying, ServiceConnection {

    TextView song_name, artist_name, duration_played, duration_total, album;
    ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn;
    FloatingActionButton playPauseBtn;
    SeekBar seekBar;
    int position = -1;
    Queue<Integer> prevpostion = new LinkedList<>();
    static ArrayList<MusicFiles> listSongs = new ArrayList<>();
    static Uri uri;
   // static MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Thread playThread, prevThread, nextThread;
    MusicService musicService;


    private void musicData(){
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());
        album.setText(listSongs.get(position).getAlbum());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        setContentView(R.layout.activity_player);
        getSupportActionBar().hide();

       //TODO Added Media Session. set active

        initViews();
        getIntenMethod();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser){
                    musicService.seekTo(progress*1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null)
                {
                    int mCurrentPostion = musicService.getCurrentPosition() /1000;
                    seekBar.setProgress(mCurrentPostion);
                    duration_played.setText(formattedTime(mCurrentPostion));
                }

                handler.postDelayed(this, 1000);
            }
        });
        shuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(shuffleBoolean){
                    shuffleBoolean = false;
                    shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
                }else{
                    shuffleBoolean = true;
                    shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                }
            }
        });
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(repeatBoolean){
                    repeatBoolean = false;
                    repeatBtn.setImageResource(R.drawable.ic_repeat_off);
                }else{
                    repeatBoolean = true;
                    repeatBtn.setImageResource(R.drawable.ic_repeat_on);
                }
            }
        });
    }

    void setFullScreen(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void prevThreadBtn() {
        prevThread = new Thread(){
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();
    }

    public void prevBtnClicked() {

        if(musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                if(prevpostion.peek()!= null && prevpostion.peek()!= position) {
                    position = prevpostion.peek();
                    prevpostion.remove();
                }else
                    Toast.makeText(this, "No previous Songs", Toast.LENGTH_SHORT);

            }else if(!shuffleBoolean && !repeatBoolean)
            {
                position = ((position - 1 < 0) ? ( listSongs.size() - 1): (position - 1 ));
            }//else position will remain same.

            uri = Uri.parse(listSongs.get(position).getPath());
            //mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
            musicService.createMediaPlayer(position);
            metaData(uri);
            musicData();
            seekBar.setMax(musicService.getDuration() /1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPostion = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPostion);
                    }

                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_pause_circle);
            playPauseBtn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        }else{
                musicService.stop();
                musicService.release();
                position =((position - 1 < 0) ? ( listSongs.size() - 1): (position - 1));
                uri = Uri.parse(listSongs.get(position).getPath());
                //mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
                musicService.createMediaPlayer(position);
                metaData(uri);
                musicData();
                seekBar.setMax(musicService.getDuration() / 1000);
                PlayerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (musicService != null) {
                            int mCurrentPostion = musicService.getCurrentPosition() / 1000;
                            seekBar.setProgress(mCurrentPostion);
                        }

                        handler.postDelayed(this, 1000);
                    }
                });
                musicService.OnCompleted();
                musicService.showNotification(R.drawable.ic_play_circle);
                playPauseBtn.setBackgroundResource(R.drawable.ic_play);
        }
    }

    private void nextThreadBtn() {

        nextThread = new Thread(){
            @Override
            public void run() {
                super.run();
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked() {

        if(musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                int i = position;
                do {
                    position = getRandom(listSongs.size() - 1);
                }while (i == position);

            }else if(!shuffleBoolean && !repeatBoolean)
            {
                position = ++position % listSongs.size();
            }//else position will remain same.

            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position); //replaced mediaPlayer with musicService
            //mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
            metaData(uri);
            musicData();
            seekBar.setMax(musicService.getDuration() /1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPostion = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPostion);
                    }

                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_pause_circle);
            playPauseBtn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        }else{
                musicService.stop();
                musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                int i = position;
                do {
                    position = getRandom(listSongs.size() - 1);
                }while (i == position);

            }else if(!shuffleBoolean && !repeatBoolean)
            {
                position = ++position % listSongs.size();
            }//else position will remain same.
                uri = Uri.parse(listSongs.get(position).getPath());
                //mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
                musicService.start();
                metaData(uri);
                musicData();
                seekBar.setMax(musicService.getDuration() / 1000);
                PlayerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (musicService != null) {
                            int mCurrentPostion = musicService.getCurrentPosition() / 1000;
                            seekBar.setProgress(mCurrentPostion);
                        }

                        handler.postDelayed(this, 1000);
                    }
                });
                musicService.OnCompleted();
                musicService.showNotification(R.drawable.ic_play_circle);
                playPauseBtn.setBackgroundResource(R.drawable.ic_play);
        }
    }

    private int getRandom(int i) {
        Random random = new Random();
        return random.nextInt(i+1);
    }

    private void playThreadBtn() {
        playThread = new Thread(){
            @Override
            public void run() {
                super.run();
                playPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });
            }
        };
        playThread.start();
    }

    public void playPauseBtnClicked() {
        if(musicService.isPlaying())
        {
            playPauseBtn.setImageResource(R.drawable.ic_play);
            musicService.showNotification(R.drawable.ic_play_circle);
            musicService.pause();
            seekBar.setMax(musicService.getDuration() / 1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPostion = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPostion);
                    }

                    handler.postDelayed(this, 1000);
                }
            });
        }else{
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            musicService.showNotification(R.drawable.ic_pause_circle);
            musicService.start();
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPostion = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPostion);
                    }

                    handler.postDelayed(this, 1000);
                }
            });

        }
    }

    private String formattedTime(int mCurrentPostion) {
        String totalout = "";
        String totalNew = "";
        String seconds = String.valueOf(mCurrentPostion%60);
        String minutes = String.valueOf(mCurrentPostion /60);
        totalout = minutes + ":" + seconds;
        totalNew = minutes + ":" + "0" + seconds;

        if (seconds.length() == 1){
            return totalNew;
        }else
            return totalout;
    }

    private void getIntenMethod() {
        position = getIntent().getIntExtra("position", -1);

        String sender = getIntent().getStringExtra("sender");

        if(sender != null && sender.equals( "albumDetails")){
            listSongs = albumFiles;
        }else{
            listSongs = mFiles;
        }
        if (listSongs != null){
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            uri = Uri.parse(listSongs.get(position).getPath());
        }
        //TODO
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition", position);
        startService(intent);


    }

    private void initViews() {

        song_name = findViewById(R.id.song_name);
        artist_name = findViewById(R.id.song_artist);
        album = findViewById(R.id.song_album);
        duration_played = findViewById(R.id.durationPlayed);
        duration_total = findViewById(R.id.durationTotal);
        cover_art = findViewById(R.id.cover_art);
        nextBtn = findViewById(R.id.id_next);
        prevBtn = findViewById(R.id.id_prev);
        backBtn =findViewById(R.id.back_btn);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
    }

    public void metaData(Uri uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
        duration_total.setText(formattedTime(durationTotal));
        try{
        if(prevpostion.peek() != position) {
            prevpostion.add(position);
        }
        else{
            prevpostion.remove();
        }}catch (NullPointerException e){
            prevpostion.add(position);
        }
        byte[] ablum_art = retriever.getEmbeddedPicture();
        Bitmap bitmap;
        if (ablum_art != null){
            bitmap = BitmapFactory.decodeByteArray(ablum_art, 0 ,ablum_art.length);
            ImageAnimation(this, cover_art, bitmap);
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@Nullable Palette palette) {
                    Palette.Swatch swatch = palette.getDominantSwatch();

                    if(swatch != null){
                        ImageView gradient = findViewById(R.id.imageViewGradient);
                        RelativeLayout mContainer = findViewById(R.id.mContainer);
                        gradient.setBackgroundResource(R.drawable.gradient_bg);
                        mContainer.setBackgroundResource(R.drawable.main_bg);
                        GradientDrawable gradientDrawable = new GradientDrawable(
                                GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(), 0x00000000});
                        gradient.setBackground(gradientDrawable);

                        GradientDrawable gradientDrawableBg = new GradientDrawable(
                                GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(), swatch.getRgb()});
                        gradient.setBackground(gradientDrawableBg);
                        song_name.setTextColor(swatch.getTitleTextColor());
                        artist_name.setTextColor(swatch.getBodyTextColor());
                    }else
                        {
                            ImageView gradient = findViewById(R.id.imageViewGradient);
                            RelativeLayout mContainer = findViewById(R.id.mContainer);
                            gradient.setBackgroundResource(R.drawable.gradient_bg);
                            mContainer.setBackgroundResource(R.drawable.main_bg);
                            GradientDrawable gradientDrawable = new GradientDrawable(
                                    GradientDrawable.Orientation.BOTTOM_TOP,
                                    new int[]{0xff000000, 0x00000000});
                            gradient.setBackground(gradientDrawable);

                            GradientDrawable gradientDrawableBg = new GradientDrawable(
                                    GradientDrawable.Orientation.BOTTOM_TOP,
                                    new int[]{0xff000000, 0x00000000});
                            gradient.setBackground(gradientDrawableBg);
                            song_name.setTextColor(Color.WHITE);
                            artist_name.setTextColor(Color.DKGRAY);
                    }
                }
            });
        }else{
            try {
                Glide.with(this)
                        .asBitmap()
                        .load(R.drawable.ic_action_name)
                        .into(cover_art);
                ImageView gradient = findViewById(R.id.imageViewGradient);
                RelativeLayout mContainer = findViewById(R.id.mContainer);
                gradient.setBackgroundResource(R.drawable.gradient_bg);
                mContainer.setBackgroundResource(R.drawable.main_bg);
                song_name.setTextColor(Color.WHITE);
                artist_name.setTextColor(Color.DKGRAY);
            }catch(Error ignored)
            {}

        }
    }

    public void ImageAnimation(Context context, ImageView imageView, Bitmap bitmap){
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);
    }


//Service for notification

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService(); // Function in MusicService.java
        musicService.setCallBack(this); // This is used so all the functions in this method acticity is called
        Toast.makeText(this,"Connected" + musicService,Toast.LENGTH_SHORT).show();
        metaData(uri);
        musicData();
        seekBar.setMax(musicService.getDuration() / 1000);
        musicService.OnCompleted();
        musicService.showNotification(R.drawable.ic_pause_circle);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;

    }




}