package ss.beadmaze.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.SweepGradient;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager; 
import android.view.Display; 

import ss.beadmaze.*;
import ss.beadmaze.Maze.Direction;
import ss.beadmaze.Maze.Orientation;

/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 * 
 * Has a mode which RUNNING, PAUSED, etc. Has a x, y, dx, dy, ... capturing the
 * current ship physics. All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 */
class AndroidMazeView extends SurfaceView implements SurfaceHolder.Callback { 
	public enum ThreadState {
		STATE_INIT,
		STATE_PAUSE,
		STATE_READY,
		STATE_RESET,
		STATE_RUNNING,
		STATE_LEVEL_OVER
	}
	
	/** The mThread that actually draws the animation */
	private AndroidMazeThread mThread = null;
	private ToneGenerator mToneGen = null;
	/** Message handler used by mThread to hint SurfaceView to show AlertDialog*/
	private Handler mMessageChannel = null;
	private final int mMessageChannelLevelOverId = -1;
	private final int mMessageChannelGameOverId = -2;
	private final int mPathStride = 10;
	private final int mBeadRadius0 = mPathStride;
	private final int mBeadRadius1 = (8 * mBeadRadius0) / 10;
	private final int mSpotlightClipRadius = 8 * mBeadRadius1;
	
	private final int mMazeEnd = Color.rgb(200, 35, 35);
	private final int mMazeStart = Color.rgb(50, 200, 50);
	private final int mBeadInner = Color.rgb(255, 188, 0);
	private final int mBeadOuter = Color.rgb(255, 122, 0);
	
	private int mCurrentSpeed = 0;
	private static final int mFixedSpeed = 3;
	final static int mSpeedMax = 10;
	private static final int mLevelMax = 11;
	private int mBackGroundColorIndex = 0;
	private final int mColors[] = {Color.rgb(160, 50, 50), Color.rgb(50, 160, 50), Color.rgb(50, 50, 160)};
	// Various keys for the save/restore bundle
	private static final String currentDirKey = "currentDirKey";
	private static final String mStateKey = "mStateKey";
	private static final String mLevelKey = "mLevelKey";
	private static final String mRunningTotalTimeKey = "mRunningTotalTimeKey";
	private static final String mBlindKey = "mBlindKey";
	private static final String mBackGroundColorIndexKey = "mBackGroundColorIndexKey";
	private static final String mazeDumpFile = "mazedump.xml";
	private long mRunningTotalTime = 0;
	private long mLastTimeStamp = 0;
	
	private Maze mMaze = null;
	private Bitmap mBackgroundImage = null;
	private boolean mSpotlightMode = false;
	private Direction mPreviousDir = Direction.ERROR;
	private Direction mCurrentDir = Direction.ERROR;
	/** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
	private ThreadState mState = ThreadState.STATE_INIT;
	/** Indicate whether the surface has been created & is ready to draw */
	private boolean mRun = true;
	private int mLevel = 0;
	
	class AndroidMazeThread extends Thread {
        /** Handle to the surface manager object we interact with */
		private SurfaceHolder mSurfaceHolder;
		public AndroidMazeThread(SurfaceHolder surfaceHolder) {
			// get handles to some important objects
			mSurfaceHolder = surfaceHolder;
		}

		/**
		 * Starts the game, setting parameters for the current difficulty.
		 */
		private void doStart() {
			synchronized (mSurfaceHolder) {
				setState(ThreadState.STATE_RUNNING);
				mSurfaceHolder.notifyAll();
			}                        
		}

		/**
		 * Pauses the physics update & animation.
		 */
		public void pause() {
			synchronized (mSurfaceHolder) {
				// Only need to pause the run() mThread loop if we are running
				if (mState == ThreadState.STATE_RUNNING) {
					setState(ThreadState.STATE_PAUSE);
				}
				mSurfaceHolder.notifyAll();
			}
		}

		/**
		 * End the game mThread i.e. AndroidMazeThread. The game mThread should be waiting on mSurfaceHolder.
		 */
		public void end() {
			boolean retry = true; 
			synchronized (mSurfaceHolder) {
				setRunning(false);
				mSurfaceHolder.notifyAll();
			}
			while (retry) {
				try {
					mThread.join();
					retry = false;
				} catch (InterruptedException e) {
				}
			}
		}
		
		@Override 
		public void run() {
			// If we were previously suspended in STATE_LEVEL_OVER then recreate the level over
			// dialog box
			boolean sendLevelOverMessage = (mState == ThreadState.STATE_LEVEL_OVER ? true : false);
			while (mRun) {	
				try {
					synchronized (mSurfaceHolder) {
						switch (mState) {
						case STATE_RUNNING:
						{
							int result = updatePhysics(); 
							if (result == 1) {
								doDraw();
							}
							else if (result == -1) {
								setState(ThreadState.STATE_LEVEL_OVER);
								doDraw();
								sendLevelOverMessage = true;
							}
							else {
								doDraw();
								// No update to bead position
								mSurfaceHolder.wait();
							}
							break;
						}
						case STATE_RESET:
						{ 
							mLevel++;
							if (!buildMazeAuto()) {
								mRun = false;
								mMessageChannel.sendEmptyMessage(mMessageChannelGameOverId);
								break;
							}
							createStaticMazeImageNew(null);
							mMaze.resetBead();
							doDraw();
							mSurfaceHolder.wait();
							setState(ThreadState.STATE_READY);
							break;
						}
						case STATE_READY:
						{ 
							mMaze.resetBead();
							doDraw();
							sendLevelOverMessage = false;
							mSurfaceHolder.wait();
							break;
						} 
						case STATE_LEVEL_OVER: 
						{
							doDraw();
							if (sendLevelOverMessage) {
								// Intimate the View that a level has finished
								// Guard this with a boolean so that if we 
								// cycle in this state many times for the same
								// level, we do not end up showing the level over
								// dialog more than once 
								mMessageChannel.sendEmptyMessage(mMessageChannelLevelOverId);
								sendLevelOverMessage = false;
							}
							// Do not burn CPU cycles by spinning in the 
							// the big while loop waiting for mState to change
							mSurfaceHolder.wait();
							break;
						}
						case STATE_PAUSE: 
						{
							doDraw();
							// Do not burn CPU cycles by spinning in the 
							// the big while loop waiting for mState to change
							mSurfaceHolder.wait();
							break;
						}
						} // switch
						mSurfaceHolder.notifyAll();
					}   // synchronized 
				} // try
				catch (InterruptedException e) {
					mRun = false;
					mMessageChannel.sendEmptyMessage(mMessageChannelGameOverId);
				}
			} // while
		}

		/**
		 * Dump game state to the provided Bundle. Typically called when the
		 * Activity is being suspended.
		 * 
		 * @return Bundle with this view's state
		 */
		public boolean saveState(Bundle savedInstanceState) {
			synchronized (mSurfaceHolder) {
				FileOutputStream stream;
				try {
					stream = AndroidMazeView.this.getContext().openFileOutput(mazeDumpFile, 
													                          android.content.Context.MODE_PRIVATE);
				} catch (FileNotFoundException e) {
					return false;
				}
				if (!mMaze.saveState(savedInstanceState, stream)) {
					return false;
				}
				savedInstanceState.putString(mStateKey, mState.toString());
				savedInstanceState.putString(currentDirKey, mCurrentDir.toString());
				mRunningTotalTime += (System.currentTimeMillis() - mLastTimeStamp);
				mLastTimeStamp = System.currentTimeMillis();
				savedInstanceState.putLong(mRunningTotalTimeKey, mRunningTotalTime);
				savedInstanceState.putBoolean(mBlindKey, mSpotlightMode);
				savedInstanceState.putInt(mLevelKey, mLevel);
				savedInstanceState.putInt(mBackGroundColorIndexKey, mBackGroundColorIndex);
				mSurfaceHolder.notifyAll();
				return true;
			}
		}


		/**
		 * Used to signal the mThread whether it should be running or not.
		 * Passing true allows the mThread to run; passing false will shut it
		 * down if it's already running. Calling start() after this was most
		 * recently called with false will result in an immediate shutdown.
		 * 
		 * @param b true to run, false to shut down
		 */
		public void setRunning(boolean b) {
			mRun = b;
		}

		/**
		 * Sets the game mode. That is, whether we are running, paused, in the
		 * failure state, in the victory state, etc.
		 * 
		 * @see #setState(int, CharSequence)
		 * @param stateRunning one of the STATE_* constants
		 */
		public void setState(ThreadState stateRunning) {
			synchronized (mSurfaceHolder) {
				mState = stateRunning;
				switch (mState) {
				case STATE_INIT:
					mRunningTotalTime = 0;
					break;
				case STATE_PAUSE:
					mRunningTotalTime += (System.currentTimeMillis() - mLastTimeStamp);
					mLastTimeStamp = System.currentTimeMillis();
					break;
				case STATE_READY:
					mRunningTotalTime = 0;
					break;
				case STATE_RESET:
					mRunningTotalTime = 0;
					break;
				case STATE_RUNNING:
					mLastTimeStamp = System.currentTimeMillis();
					break;
				case STATE_LEVEL_OVER:
					mRunningTotalTime += (System.currentTimeMillis() - mLastTimeStamp);
					mLastTimeStamp = System.currentTimeMillis();
					break;
				}
				mSurfaceHolder.notifyAll();
			}
		}

		/* Callback invoked when the surface dimensions change. */
		public void setSurfaceSize(int width, int height) { 
			// synchronized to make sure these all change atomically
			synchronized (mSurfaceHolder) {
				mSurfaceHolder.notifyAll();
			}
		}

		/**
		 * Resumes from a pause.
		 */
		public void unpause() {
			// Move the real time clock up to now
			synchronized (mSurfaceHolder) {
				// Only if state is paused change to running. Look at
				// pause() logic to see why.
				if (mState == ThreadState.STATE_PAUSE) {
					mLastTimeStamp = System.currentTimeMillis();
					setState(ThreadState.STATE_RUNNING);
				}
				mSurfaceHolder.notifyAll();
			}
		}

		boolean computeFrame(Canvas can) {
			drawMaze(can);
			drawBead(can);   
			if (mSpotlightMode) 
				drawOverlay(can);	
			return true;
		}

		private void drawMaze(Canvas can) {
			can.drawBitmap(mBackgroundImage, 0, 0, null);
		}

		private void drawBead(Canvas can) {
			// Draw the filled liquid
			Paint g = new Paint();
			g.setAntiAlias(true);
			g.setColor(mBeadInner); //Color.rgb(92, 51, 23));
			can.drawCircle(mMaze.getBeadLocation().getX(), mMaze.getBeadLocation().getY(), 
					mBeadRadius1, g);
			can.drawCircle(mMaze.getBeadLocation().getX(), mMaze.getBeadLocation().getY(), 
					mBeadRadius1 - 1, g);
			g.setColor(mBeadOuter);
			g.setStyle(Paint.Style.FILL);
			can.drawCircle(mMaze.getBeadLocation().getX(), mMaze.getBeadLocation().getY(), 
					mBeadRadius1 - 2, g);
		}

		private void drawOverlay(Canvas can) {
			Path path = new Path();
			path.addCircle(mMaze.getBeadLocation().getX(), mMaze.getBeadLocation().getY(), 
					       mSpotlightClipRadius, Path.Direction.CW);
			can.clipPath(path, Region.Op.XOR);
			Paint g = new Paint();
			g.setAntiAlias(true);
			g.setAlpha(220);
			can.drawRect(0, 0, mMaze.getWidth(), mMaze.getHeight(), g);
			can.clipRect(0, 0, mMaze.getWidth(), mMaze.getHeight(), Region.Op.REPLACE);
			g.setStyle(Paint.Style.STROKE);
			g.setAlpha(0);
			g.setColor(Color.WHITE);
			can.drawCircle(mMaze.getBeadLocation().getX(), mMaze.getBeadLocation().getY(), 
						   mSpotlightClipRadius, g);
		}
		
		/**
		 * Draws the ship, fuel/mCurrentSpeed bars, and mBackgroundImage to the provided
		 * Canvas.
		 */
		private boolean doDraw() {
			boolean result = false;
			Canvas c = mSurfaceHolder.lockCanvas(null);
			if (c != null) 	{
				result = computeFrame(c);
				mSurfaceHolder.unlockCanvasAndPost(c);
			}
			return result;
		}

		/**
		 * Figures the lander state (x, y, fuel, ...) based on the passage of
		 * real-time. Does not invalidate(). Called at the start of draw().
		 * Detects the end-of-game and sets the UI to the next state.
		 */
		private int updatePhysics() {
			int distance = mCurrentSpeed;
			Orientation orientation = Orientation.ERROR;
			switch (mCurrentDir) {
			case ERROR:
			case NONE:
				return 0;
			case EAST:
				orientation = Orientation.X_AXIS;
				break;
			case SOUTH:
				orientation = Orientation.Y_AXIS;
				break;
			case WEST:
				distance *= -1;
				orientation = Orientation.X_AXIS;
				break;
			case NORTH:
				distance *= -1;
				orientation = Orientation.Y_AXIS;
				break;
			default:
				assert(false);
				break;	
			}
			int result = mMaze.moveBead(distance, orientation);
			if (mCurrentDir == mPreviousDir)
				return result;
			if (result == 0)
				return result;
			mToneGen.startTone(ToneGenerator.TONE_PROP_BEEP);
			mPreviousDir = mCurrentDir;
			return result;
		}

		boolean doKeyDown(int keyCode, float speed) {
			synchronized (mSurfaceHolder) {
				boolean okStart = false;
				if (keyCode == KeyEvent.KEYCODE_DPAD_UP) okStart = true;
				if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) okStart = true;
				if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) okStart = true;
				if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) okStart = true;
				if (keyCode == KeyEvent.KEYCODE_S) okStart = true;

				if (mState == ThreadState.STATE_LEVEL_OVER) {
					mSurfaceHolder.notifyAll();
					return true;
				}
				
				boolean result = false;
				if (okStart && (mState == ThreadState.STATE_READY)) {
					// ready-to-start -> start
					doStart();
					result = true;
				} else if (mState == ThreadState.STATE_PAUSE && okStart) {
					// paused -> running
					unpause();
					result = true;
				} else if (mState == ThreadState.STATE_RUNNING) {
					// center/space -> fire
					if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
							|| keyCode == KeyEvent.KEYCODE_SPACE) {

						result = true;
						// left/q -> left
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
						mCurrentDir = Direction.WEST;
						result = true;
						// right/w -> right
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
						mCurrentDir = Direction.EAST;
						result = true;
						// up -> pause
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
						mCurrentDir = Direction.NORTH;
						result = true;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
						mCurrentDir = Direction.SOUTH;
						result = true;
					}
				}
				mCurrentSpeed = (int)(speed * mFixedSpeed);
				mSurfaceHolder.notifyAll();
				return result;	
			}
		}

		/**
		 * Handles a key-up event.
		 * 
		 * @param keyCode the key that was pressed
		 * @param speed the original event object
		 * @return true if the key was handled and consumed, or else false
		 */
		boolean doKeyUp(int keyCode, float speed) {
			synchronized (mSurfaceHolder) {
				boolean handled = false;
				if (mState == ThreadState.STATE_LEVEL_OVER) {				
					mSurfaceHolder.notifyAll();
					return true;
				}
				
				boolean stopBead = false;
				if (mState == ThreadState.STATE_RUNNING) {
					if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT) && 
							   (mCurrentDir == Direction.WEST)) {
						stopBead = true;
						handled = true;
					}
					else if ((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && 
						     (mCurrentDir == Direction.EAST)) {
						stopBead = true;
						handled = true;
					}
					else if ((keyCode == KeyEvent.KEYCODE_DPAD_UP) && 
							(mCurrentDir == Direction.NORTH)) {
						stopBead = true;
						handled = true;
					}
					else if ((keyCode == KeyEvent.KEYCODE_DPAD_DOWN) && 
							(mCurrentDir == Direction.SOUTH)) {
						stopBead = true;
						handled = true;
					}
					
					if (stopBead) {
						mCurrentDir = Direction.NONE;
						mCurrentSpeed = mFixedSpeed;
					}
				}
				mSurfaceHolder.notifyAll();
				return handled; 
			}
		}
	}

	
	public AndroidMazeView(Context context, AttributeSet attrs, Bundle savedInstanceState) {
		super(context);
		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		mMessageChannel = new Handler() {
			@Override
            public void handleMessage(Message m) {
				if (m.what == mMessageChannelLevelOverId)
					showLevelOverDialog();
				else if (m.what == mMessageChannelGameOverId)
					((Activity)getContext()).finish();
            }
		};	
		mToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC,ToneGenerator.MAX_VOLUME/2);
		init(savedInstanceState);
		//init(null);
		setFocusable(true); // make sure we get key events
	}
	
	/**
	 * Standard override for key-up. We actually care about these, so we can
	 * turn off the engine or stop rotating.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		return onKeyDown(keyCode, 1.0f);
	}

	boolean onKeyDown(int keyCode, float speed) {
		if (mThread == null) return false;
		return mThread.doKeyDown(keyCode, speed);
	}
	
	/**
	 * Standard override for key-up. We actually care about these, so we can
	 * turn off the engine or stop rotating.
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent msg) {
		return onKeyUp(keyCode, 1.0f);
	}

	boolean onKeyUp(int keyCode, float speed) {
		if (mThread == null) return false;
		return mThread.doKeyUp(keyCode, speed);
	}
	
	boolean onSensorEvent(float speedX, float speedY) {
		if (mThread == null) return false;
		return false;
	}
	
	void toggleSpotlightMode() {
		mSpotlightMode = !mSpotlightMode;
	}
	
	private void showLevelOverDialog()
	{
		AlertDialog.Builder adb = new AlertDialog.Builder(this.getContext());
        adb.setTitle("Info");
        long time = mRunningTotalTime/1000;
        String msg = "Level " + mLevel + " completed in " + time + " secs";       
        if (mLevel == mLevelMax-1)
        	msg += "\nCongratulations! Game over!!";
        adb.setMessage(msg);
        adb.setNegativeButton("Quit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,
                    int which) {
                dialog.cancel();
                mLevel = mLevelMax;
                mThread.setState(ThreadState.STATE_RESET);
            }
        });
        if (mLevel < mLevelMax -1) { 
        adb.setPositiveButton("Next Level",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int which) {
                        dialog.cancel();
                        mThread.setState(ThreadState.STATE_RESET);
                    }
                });
        }
        AlertDialog ad = adb.create();
        ad.show();			
	}

	/**
	 * Standard window-focus override. Notice focus lost so we can pause on
	 * focus lost. e.g. user switches to take a call.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus && (mThread != null)) 
			mThread.pause();
	}


	/* Callback invoked when the surface dimensions change. */
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mThread == null)
			return;
		mThread.setSurfaceSize(width, height);
	}

	/*
	 * Callback invoked when the Surface has been created and is ready to be
	 * used.
	 * NOTE: This method also gets called when Activity is RESUMED
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		// This should really be called when every thing has been setup, viz mState, bead's location, etc
		mThread = new AndroidMazeThread(holder);
		mThread.setRunning(true);
		if (!mThread.isAlive()) {
			mThread.start();
		}
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 * NOTE: This method also gets called when Activity is PAUSED
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		// We have to tell mThread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		mThread.end();
		mThread = null;
	}

	public void pause() {
		if (mThread == null)
			return;
		mThread.pause();
	}

	private void init(Bundle savedInstanceState) {
		if (savedInstanceState == null) { 
			mLevel = 1;
			mState = ThreadState.STATE_READY;
			mRunningTotalTime = 0;
			buildMazeAuto();
			mMaze.resetBead();
		}
		else {
			mLevel = savedInstanceState.getInt(mLevelKey);
			mState = ThreadState.valueOf(savedInstanceState.getString(mStateKey));
			mCurrentDir = Direction.valueOf(savedInstanceState.getString(currentDirKey));
			mRunningTotalTime = savedInstanceState.getLong(mRunningTotalTimeKey);
			mSpotlightMode = savedInstanceState.getBoolean(mBlindKey);
			mLastTimeStamp = System.currentTimeMillis();
			restoreMaze();
			mMaze.restoreState(savedInstanceState);
			mLastTimeStamp = System.currentTimeMillis();
		}
		createStaticMazeImageNew(savedInstanceState);
	}
	
	public void saveState(Bundle savedInstanceState) {
		if (mThread == null)
			return;
		mThread.saveState(savedInstanceState);
	}

	private void printLinks(Vertex junction, Direction dir, Paint brush, Canvas backgroundCanvas) {
		Vertex v = mMaze.findVertex(junction, dir);	
		if (v == null) {
			return;
		}	
		int x0 = junction.getLocation().getX();
		int y0 = junction.getLocation().getY();
		int x1, y1;
	
		if (dir == Direction.EAST) {
			x0 -= mPathStride;
			x1 = v.getLocation().getX() + mPathStride;
			y1 = y0;
		}
		else {
			y0 -= mPathStride;
			y1 = v.getLocation().getY() + mPathStride;
			x1 = x0;
			assert(dir == Direction.SOUTH);
		}
		backgroundCanvas.drawLine(x0, y0, x1, y1, brush); 		
	}

	private void printLinks(Vertex junction, Paint brush, Canvas backgroundCanvas) {
		printLinks(junction, Direction.EAST, brush, backgroundCanvas);
		printLinks(junction, Direction.SOUTH, brush, backgroundCanvas);
	}
	
	private int scaleColor(int c) {
		return Color.rgb(Color.red(c) + (50 * mLevel) / (mLevelMax - 1), 
				         Color.green(c) + (50 * mLevel) / (mLevelMax - 1), 
				         Color.blue(c) + (50 * mLevel) / (mLevelMax - 1));
	}
	
	private void createStaticMazeImageNew(Bundle savedInstanceState)
	{   
		mBackgroundImage = Bitmap.createBitmap(mMaze.getWidth(), mMaze.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas backgroundCanvas = new Canvas(mBackgroundImage);
		if (savedInstanceState != null)
			mBackGroundColorIndex = savedInstanceState.getInt(mBackGroundColorIndexKey);
		else {
			mBackGroundColorIndex++;	
			if (mBackGroundColorIndex == mColors.length) {
				mBackGroundColorIndex = 0;
			}			
		}
		
		int colors[] = new int[mColors.length + 1];
		for (int i = mBackGroundColorIndex; i < mColors.length; i++) {
			// Brighten the color as the level increases so that screen remains bright
			// as the maze walls thin
				colors[i - mBackGroundColorIndex] = scaleColor(mColors[i]);
		}
		
		for (int i = 0; i < mBackGroundColorIndex; i++) {
			// Brighten the color as the level increases so that screen remains bright
			// as the maze walls thin
			colors[mColors.length - mBackGroundColorIndex + i] = scaleColor(mColors[i]);
		}
		
		colors[mColors.length] = colors[0];
		
		SweepGradient bg = new SweepGradient(mMaze.getEndVertex().getLocation().getX(), mMaze.getEndVertex().getLocation().getY(), colors, null);
		
		Paint brush = new Paint();
		brush.setShader(bg);
		backgroundCanvas.drawRect(0, 0, mMaze.getWidth(), mMaze.getHeight(), brush);
		
		brush = new Paint();
		brush.setColor(Color.BLACK);
		brush.setStyle(Paint.Style.FILL);
		brush.setStrokeWidth(mPathStride * 2);
		brush.setAntiAlias(true);
		int vertexCount = mMaze.getVertexCount();
		for (int i = 0; i< vertexCount; i++) {
			printLinks(mMaze.getVertex(i), brush, backgroundCanvas);
		} 
		
		Rect endRect = new Rect((mMaze.getEndVertex().getLocation().getX() - mPathStride), 
				                (mMaze.getEndVertex().getLocation().getY() - mPathStride), 
				                (mMaze.getEndVertex().getLocation().getX() + mPathStride), 
				                (mMaze.getEndVertex().getLocation().getY() + mPathStride));
		
		Rect startRect = new Rect((mMaze.getStartVertex().getLocation().getX() - mPathStride), 
		 		                  (mMaze.getStartVertex().getLocation().getY() - mPathStride), 
                                  (mMaze.getStartVertex().getLocation().getX() + mPathStride), 
                                  (mMaze.getStartVertex().getLocation().getY() + mPathStride));

		brush.setColor(mMazeEnd);
		backgroundCanvas.drawRect(endRect, brush);
		
		brush.setColor(mMazeStart);
		backgroundCanvas.drawRect(startRect, brush);
		
		brush.setStrokeWidth(1);
		brush.setStyle(Paint.Style.STROKE);
		brush.setColor(Color.WHITE);
		backgroundCanvas.drawRect(endRect, brush);
		backgroundCanvas.drawRect(startRect, brush);
	}

	
	
	private boolean restoreMaze() {
		DisplayMetrics dm = new DisplayMetrics();
        Display display = ((WindowManager)this.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(dm);
        MazeBuilder builder = new MazeBuilder(dm.widthPixels, dm.heightPixels, mPathStride);
        FileInputStream stream = null;
		try {
			stream = AndroidMazeView.this.getContext().openFileInput(mazeDumpFile);
		} catch (FileNotFoundException e) {
			return false;
		}
        mMaze = builder.build(stream);
		return true;
	}
	
	private boolean buildMazeAuto() {
		if (mLevel >= mLevelMax) 
			return false;
		
		InputStream s = getResources().openRawResource(R.raw.levels);
		DisplayMetrics dm = new DisplayMetrics();
        Display display = ((WindowManager)this.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(dm);
        AutoMazeBuilder builder = new AutoMazeBuilder(dm.widthPixels, dm.heightPixels, mPathStride);
        mMaze = builder.build(s, mLevel);
    
        try {
			s.close();
		} catch (IOException e) {
			return false;
		}
		
		if (mMaze == null)
			return false;
	    /*
	     * For debugging
		XMLView xv = new XMLView("/sdcard/out." + mLevel + ".xml", mMaze);
		xv.print();
		*/
		return true;
	}
}
