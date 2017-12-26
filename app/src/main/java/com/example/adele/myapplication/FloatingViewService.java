package com.example.adele.myapplication;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;


/**
 * Created by admin on 2017/12/21.
 */

public class FloatingViewService extends Service implements
        SpotifyPlayer.NotificationCallback,ConnectionStateCallback,Player.AudioFlushCallback{
    private WindowManager mWindowManager;
    private View mFloatingView;
    private static String accessToken;
    private static final String CLIENT_ID = "2c63b0fb6bdc4b628544a62e7f1c6ca3";
    private SpotifyPlayer mPlayer;
    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            Log.d("spotify", "success");
        }

        @Override
        public void onError(Error error) {
            Log.d("spotify", error.toString());
        }
    };
    private static String tempAlbum="spotify:album:3T4tUhGYeRNVUGevb0wThu";  //Edsheeran Deluxe
    private boolean isPause=false;
    @Override
    public void onCreate() {
        super.onCreate();
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        //The root element of the collapsed view layout
        final View collapsedView = mFloatingView.findViewById(R.id.collapse_view);
//The root element of the expanded view layout
        final View expandedView = mFloatingView.findViewById(R.id.expanded_container);

        //Set the close button
        ImageView closeButtonCollapsed = (ImageView) mFloatingView.findViewById(R.id.close_btn);
        closeButtonCollapsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //close the service and remove the from from the window
                stopSelf();
            }
        });
        //Set the close button
        ImageView closeButton = (ImageView) mFloatingView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
            }
        });

        //Open the application on thi button click
        ImageView openButton = (ImageView) mFloatingView.findViewById(R.id.open_button);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Open the application  click.
                Intent intent = new Intent(FloatingViewService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);


                //close the service and remove view from the view hierarchy
                stopSelf();
            }
        });

        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:


                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;


                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);


                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 10 && Ydiff < 10) {
                            if (isViewCollapsed()) {
                                //When user clicks on the image view of the collapsed layout,
                                //visibility of the collapsed layout will be changed to "View.GONE"
                                //and expanded view will become visible.
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }
    private boolean isViewCollapsed() {
        return mFloatingView == null || mFloatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        accessToken = intent.getExtras().getString("accessToken");
        createPlayer();
        //Set the view while floating view is expanded.
        //Set the play button.
        final ImageView playButton = (ImageView) mFloatingView.findViewById(R.id.play_btn);
        final ImageView pauseButton = (ImageView) mFloatingView.findViewById(R.id.pause_btn);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyPlay();

            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyPause();

            }
        });

        //Set the next button.
        ImageView nextButton = (ImageView) mFloatingView.findViewById(R.id.next_btn);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyNext();

            }
        });

        //Set the previous button.
        ImageView prevButton = (ImageView) mFloatingView.findViewById(R.id.prev_btn);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyPrev();

            }
        });


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void createPlayer(){
        if (mPlayer == null) {
            Config playerConfig = new Config(this, accessToken, CLIENT_ID);
            Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer spotifyPlayer) {
                    mPlayer = spotifyPlayer;
                    mPlayer.addConnectionStateCallback(FloatingViewService.this);
                    mPlayer.addNotificationCallback(FloatingViewService.this);
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("Service", "Could not initialize player: " + throwable.getMessage());
                }
            });
        }else{
            mPlayer.login(accessToken);
        }
//        mPlayer.setRepeat(mOperationCallback,true);
    }
    private void spotifyPlay(){
        if(isPause){
            mPlayer.resume(mOperationCallback);
        }else{
            mPlayer.playUri(mOperationCallback,tempAlbum,0,0);
        }
        isPause=false;
        Toast.makeText(FloatingViewService.this,"播放音樂", Toast.LENGTH_LONG).show();
    }
    private void spotifyPause(){
        mPlayer.pause(mOperationCallback);
        isPause=true;
        Toast.makeText(FloatingViewService.this,"暫停音樂", Toast.LENGTH_LONG).show();
    }
    private void spotifyPrev(){
        mPlayer.skipToPrevious(mOperationCallback);
        Toast.makeText(FloatingViewService.this,"上一首", Toast.LENGTH_LONG).show();
    }
    private void spotifyNext(){
        mPlayer.skipToNext(mOperationCallback);
        Toast.makeText(FloatingViewService.this,"下一首", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onLoggedIn() {
        Log.d("spotify", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("spotify", "User logged out");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.d("spotify", "Login failed:"+error);
    }


    @Override
    public void onTemporaryError() {
        Log.d("spotify", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String s) {
        Log.d("spotify", "Received connection message: " + s);
    }
    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("spotify", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("spotify", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onAudioFlush() {
        Toast.makeText(FloatingViewService.this, mPlayer.getMetadata().currentTrack.artistName+"-"+mPlayer.getMetadata().currentTrack.name, Toast.LENGTH_LONG).show();
    }
}
