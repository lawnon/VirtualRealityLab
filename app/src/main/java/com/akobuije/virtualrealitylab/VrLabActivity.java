package com.akobuije.virtualrealitylab;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.vr.ndk.base.DaydreamApi;

import com.akobuije.virtualrealitylab.rendering.Mesh;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VrLabActivity extends AppCompatActivity {
  /**
   * Declaration of Media Path and MonoscopicView
   */
  private static final int LOAD_VIDEO = 0;
  private static final int LOAD_IMAGE = 1;
  private static final int READ_EXTERNAL_STORAGE_PERMISSION_ID = 4;
  private static final String defaultPath = "android.resource://com.akobuije.virtualrealitylab/" + R.raw.default_vr_video;
  public static boolean MEDIA_ACTIVE = false;
  public static boolean LOOP_VID = true;

  private boolean _visible;
  private View _hiddenControls;
  private TextInputLayout _txUrlInput;
  private ImageButton _ibLoadUrlMedia;
  private FloatingActionButton _btShowMenu;
  private MenuItem _miLoadVideo;
  private MenuItem _miLoadPic;
  private MenuItem _miLoadUrl;
  private MenuItem _miHome;
  private Switch _miLoop;
  private Intent _vrIntent;
  private Intent _loadIntent;

  /**
   * Whether or not the system UI should be auto-hidden after
   * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
   */
  private static final boolean AUTO_HIDE = true;

  /**
   * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
   * user interaction before hiding the system UI.
   */
  private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

  /**
   * Some older devices needs a small delay between UI widget updates
   * and a change of the status and navigation bar.
   */
  private static final int UI_ANIMATION_DELAY = 300;
  private final Handler mHideHandler = new Handler();
  private MonoscopicView _monoView;
  private VideoUiView _monoViewUi;
  private final Runnable mHidePart2Runnable = new Runnable() {
    @SuppressLint("InlinedApi")
    @Override
    public void run() {
      // Delayed removal of status and navigation bar

      // Note that some of these constants are new as of API 16 (Jelly Bean)
      // and API 19 (KitKat). It is safe to use them, as they are inlined
      // at compile-time and do nothing on earlier devices.
      _monoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
          | View.SYSTEM_UI_FLAG_FULLSCREEN
          | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
  };

  private final Runnable mShowPart2Runnable = new Runnable() {
    @Override
    public void run() {
      // Delayed display of UI elements
      _hiddenControls.setVisibility(View.VISIBLE);
    }
  };

  private final Runnable mHideRunnable = new Runnable() {
    @Override
    public void run() {
      hide();
    }
  };
  /**
   * Touch listener to use for in-layout UI controls to delay hiding the
   * system UI. This is to prevent the jarring behavior of controls going away
   * while interacting with activity UI.
   */
  private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
      if (AUTO_HIDE) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
      }
      return false;
    }
  };

  private void loadDefaultIntent(){
    _vrIntent = new Intent()
        .setType("video/*")
        .setAction(Intent.ACTION_VIEW)
        .setData(Uri.parse(defaultPath));
    MediaLoader.LOCK_MEDIAPLAYER = true;
    _monoView.loadMedia(_vrIntent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.vrlab_activity);

    Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(myToolbar);

    _visible = true;
    _hiddenControls = findViewById(R.id.fullscreen_content_controls);
    _btShowMenu = findViewById(R.id.floating_control);
    _monoView = (MonoscopicView) findViewById(R.id.video_view);
    _monoViewUi = (VideoUiView) findViewById(R.id.video_ui_view);

    // Set up the user interaction to manually show or hide the system UI.
    _btShowMenu.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (MEDIA_ACTIVE)
          _monoViewUi.setVisibility(View.VISIBLE);
        else
          _monoViewUi.setVisibility(View.GONE);

        toggle();
      }
    });

    // Upon interacting with UI controls, delay any scheduled hide()
    // operations to prevent the jarring behavior of controls going away
    // while interacting with the UI.
    //findViewById(R.id.floating_control).setOnTouchListener(mDelayHideTouchListener);

    ActivityCompat.requestPermissions(
        VrLabActivity.this,
        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
        READ_EXTERNAL_STORAGE_PERMISSION_ID);

    //Load VrMedia
    _monoView.initialize(_monoViewUi);
  }

  // Menu icons are inflated just as they were with actionbar
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);

    _miLoadVideo = menu.findItem(R.id.load_video);
    _miLoadPic = menu.findItem(R.id.load_pic);
    _miLoadUrl = menu.findItem(R.id.load_url);
    _miHome = menu.findItem(R.id.home);
    _miLoop = (Switch) menu.findItem(R.id.switch_bar_loop).getActionView();

    _miLoadVideo.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        _loadIntent = new Intent()
            .setType("video/*")
            .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(
            Intent.createChooser(
                _loadIntent,
                item.getTitle()),
            LOAD_VIDEO);
        return false;
      }
    });

    _miLoadPic.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        _loadIntent = new Intent()
            .setType("image/*")
            .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(
            Intent.createChooser(
                _loadIntent,
                item.getTitle()),
            LOAD_IMAGE);
        return false;
      }
    });

    _miLoadUrl.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        _txUrlInput = (TextInputLayout) findViewById(R.id.url_input);
        _ibLoadUrlMedia = (ImageButton) findViewById(R.id.bt_get_url);

        _txUrlInput.setVisibility(View.VISIBLE);

        _ibLoadUrlMedia.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            _vrIntent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(_txUrlInput.getEditText().getText().toString()));

            MediaLoader.LOCK_MEDIAPLAYER = true;
            _monoView.loadMedia(_vrIntent);

            _txUrlInput.setVisibility(View.GONE);
          }
        });
        return false;
      }
    });

    _miHome.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        loadDefaultIntent();
        MEDIA_ACTIVE = false;
        return false;
      }
    });

    _miLoop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        LOOP_VID = isChecked;
      }
    });

    return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == LOAD_VIDEO && resultCode == RESULT_OK) {
      MediaLoader.LOCK_MEDIAPLAYER = true;
      _monoView.loadMedia(data);
    }
    if (requestCode == LOAD_IMAGE && resultCode == RESULT_OK) {
      MediaLoader.LOCK_MEDIAPLAYER = true;
      _monoView.loadOnlyMedia(data);
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    // Trigger the initial hide() shortly after the activity has been
    // created, to briefly hint to the user that UI controls
    // are available.
    delayedHide(100);

    loadDefaultIntent();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (_monoView != null) {
      _monoView.onResume();
    }
  }

  @Override
  protected void onPause() {
    // MonoscopicView is a GLSurfaceView so it needs to pause & resume rendering. It's also
    // important to pause MonoscopicView's sensors & the video player.
    if (_monoView != null) {
      _monoView.onPause();
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (_monoView != null) {
      _monoView.destroy();
    }
    super.onDestroy();
  }

  private void toggle() {
    if (_visible) {
      hide();
    } else {
      show();
    }
  }

  private void hide() {
    // Hide UI first
    _hiddenControls.setVisibility(View.GONE);
    _visible = false;

    // Schedule a runnable to remove the status and navigation bar after a delay
    mHideHandler.removeCallbacks(mShowPart2Runnable);
    mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
  }

  @SuppressLint("InlinedApi")
  private void show() {
    // Show the system bar
    _monoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    _visible = true;

    // Schedule a runnable to display UI elements after a delay
    mHideHandler.removeCallbacks(mHidePart2Runnable);
    mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
  }

  /**
   * Schedules a call to hide() in delay milliseconds, canceling any
   * previously scheduled calls.
   */
  private void delayedHide(int delayMillis) {
    mHideHandler.removeCallbacks(mHideRunnable);
    mHideHandler.postDelayed(mHideRunnable, delayMillis);
  }
}
