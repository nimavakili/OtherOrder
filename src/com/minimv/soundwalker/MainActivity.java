package com.minimv.soundwalker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.app.ActionBarImpl;
import com.actionbarsherlock.internal.app.ActionBarWrapper;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

//import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.minimv.soundwalker.R;
import com.bugsense.trace.BugSenseHandler;

public class MainActivity extends SherlockFragmentActivity
		implements ActionBar.TabListener {

    private AppSectionsPagerAdapter mAppSectionsPagerAdapter;
    private ViewPager mViewPager;
    private static MainSectionFragment mainFragment;
    private static MapSectionFragment mapFragment;
    //private Bundle previousInstance;
    private boolean wasChangingConfigurations = false;
    //public static int MAIN_FRAGMENT_INDEX = 0;
    //public static int MAP_FRAGMENT_INDEX = 1;
    //public static int ABOUT_FRAGMENT_INDEX = 2;

    private Context mContext;
	private final String TAG = "MainActiviy";
	private static GPSService gpsService;
	private final ServiceConnection gpsServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			gpsService = ((GPSService.LocationServiceBinder) service).getService();
			//Log.v(TAG, "GPS service is tracking: " + gpsService.isTracking());
			if (mainFragment != null) {
				if (gpsService.isTracking()) {
					//Log.v("TB", "wantsToSetChecked");
					mainFragment.tb.post(new Runnable() {
						@Override
						public void run() {
							mainFragment.tb.setChecked(true);
							//Log.v("TB", "setChecked");
						}
					});
				}
				mainFragment.searching.post(new Runnable() {
					@Override
					public void run() {
		            	mainFragment.accuracy.setText("---");
		            	mainFragment.lattitude.setText("---");
		            	mainFragment.longitude.setText("---");
		            	mainFragment.active.setText("0");
						if (gpsService.GPSDisabled()) {
							mainFragment.searching.setText(getStr(R.string.gps_disabled));
						}
						else if (gpsService.gotLock()) {
							mainFragment.searching.setText(getStr(R.string.lock));
						}
						else {
							mainFragment.searching.setText(getStr(R.string.searching));
						}
					}
				});
			}
			/*if (mapFragment != null) {
				mapFragment.rootView.post(new Runnable() {
					public void run() {
						//mapFragment.toggleDebug(mapFragment.debug);
						mapFragment.addNodes();
					}
				});
				mapFragment.rootView.postDelayed(new Runnable() {
					public void run() {
						//mapFragment.toggleDebug(mapFragment.debug);
						mapFragment.addNodes();
					}
				}, 2000);
			}*/
			if (!wasChangingConfigurations) {
				handler.post(GPSDialog);
			}
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			gpsService = null;
		}
	};
	private final Runnable GPSDialog = new Runnable() {
		@Override
		public void run() {
			if (gpsService != null) {
				if (gpsService.GPSDisabled()) {
	            	if (GPSAlert != null) {
	            		if (GPSAlert.isShowing()) {
	            			return;
	            		}
	            	}
	            	try {
						AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			            int currentApiVersion = android.os.Build.VERSION.SDK_INT;
			            String msg = "";
			            if (currentApiVersion >= 19) {
			            	msg = getStr(R.string.gps_disabled_prompt_kitkat);
			            }
			            else {
			            	msg = getStr(R.string.gps_disabled_prompt);
			            }
			            builder.setMessage(msg);
			            builder.setCancelable(true);
			            builder.setPositiveButton(getStr(R.string.enable_gps), new DialogInterface.OnClickListener() {
			                 public void onClick(DialogInterface dialog, int id) {
			                      Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			                      gpsOptionsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			                      startActivity(gpsOptionsIntent);
			                  }
			             });
			             builder.setNegativeButton(getStr(R.string.do_nothing), new DialogInterface.OnClickListener() {
			                  public void onClick(DialogInterface dialog, int id) {
			                	  dialog.dismiss();
			                  }
			             });
			             GPSAlert = builder.create();
			             GPSAlert.show();
	            	}
	            	catch (NullPointerException e) {
	            		//e.printStackTrace();
	            	}
	            }
				else {
					if (!gpsService.gotLock())
						//searching.setVisibility(View.VISIBLE);
						if (mainFragment != null) {
							mainFragment.searching.setText(getStr(R.string.searching));
						}
				}
			}
		}
	};
	/*private Runnable asyncInit = new Runnable() {
		@Override
		public void run() {
			// TODO
			setTabsWidth();
			if (gpsService != null) {
				if (gpsService.isTracking() && mainFragment != null) {
					mainFragment.tb.setChecked(true);
				}
			}
			if (mainFragment != null) {
				mainFragment.searching.setText(getStr(R.string.gps_disabled));
			}
		}
	};*/
	private final Handler handler = new Handler();
	private AlertDialog GPSAlert;
	private Intent gpsIntent;
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String messageLock = intent.getStringExtra(GPSService.messageLock);
            double[] messageLoc = intent.getDoubleArrayExtra(GPSService.messageLoc);
            int messageAct = intent.getIntExtra(GPSService.messageAct, -1000);
            int messageAll = intent.getIntExtra(GPSService.messageAll, -1000);
            if (messageLock != null) {
	            //Log.v("Receive", messageLock);
				if (mainFragment != null) {
		            if (messageLock.equals("No")) {
		            	if (mapFragment != null)
		            		mapFragment.noLock();
		            	//searching.setVisibility(View.VISIBLE);
		            	if (gpsService.GPSDisabled()) {
		            		mainFragment.searching.setText(getStr(R.string.gps_disabled));
		            	}
		            	else {
		            		mainFragment.searching.setText(getStr(R.string.searching));
		            	}
		            	mainFragment.accuracy.setText("---");
		            	mainFragment.lattitude.setText("---");
		            	mainFragment.longitude.setText("---");
		            	mainFragment.active.setText("0");
		            	//all.setText("---");
		            }
		            else if (messageLock.equals("Yes")) {
		            	//searching.setVisibility(View.INVISIBLE);
		            	mainFragment.searching.setText(getStr(R.string.lock));
		            }
				}
            }
            if (messageLoc != null) {
            	String lat = String.valueOf(messageLoc[0]);
            	String lon = String.valueOf(messageLoc[1]);
            	String acc = String.valueOf(messageLoc[2]);
	            //Log.v("Receive", acc);
				if (mainFragment != null) {
					mainFragment.lattitude.setText(lat.substring(0, Math.min(10, lat.length())));
	            	mainFragment.longitude.setText(lon.substring(0, Math.min(10, lon.length())));
	            	mainFragment.accuracy.setText(acc);
				}
            	//mapFragment.updateLocation(42.296335, -71.121215, 0);
            	if (mapFragment != null)
            		mapFragment.updateLocation(messageLoc[0], messageLoc[1], messageLoc[2]);
            }
            if (messageAct != -1000) {
            	String act = String.valueOf(messageAct);
	            //Log.v("Receive", act);
				if (mainFragment != null) {
					mainFragment.active.setText(act);
				}
        		if (mapFragment != null) {
        			if (mapFragment.debug) {
        				mapFragment.updateNodes();
        			}
        		}
            }
            if (messageAll != -1000) {
            	String allS = String.valueOf(messageAll);
	            //Log.v("Receive", allS);
				if (mainFragment != null) {
					mainFragment.all.setText(allS);
				}
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		mContext = this;
		BugSenseHandler.initAndStartSession(this, "41c5041a");
		setContentView(R.layout.activity_main);
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());
        final ActionBar actionBar = getSupportActionBar();
        //TODO: pre-ICS
        if (actionBar instanceof ActionBarImpl) {
            enableEmbeddedTabs(actionBar);
        //ICS and forward
        }
        else if (actionBar instanceof ActionBarWrapper) {
            try {
                Field actionBarField = actionBar.getClass().getDeclaredField("mActionBar");
                actionBarField.setAccessible(true);
                enableEmbeddedTabs(actionBarField.get(actionBar));
            } catch (Exception e) {
                Log.e(TAG, "Error enabling embedded tabs", e);
    	        BugSenseHandler.sendException(e);
            }
        }
        
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        //actionBar.setIcon(Color.TRANSPARENT);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        mViewPager.setOffscreenPageLimit(2);
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
        
        //This has to be after adding tabs to get rid of auto change to list mode
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //mViewPager.setCurrentItem(2);

        gpsIntent = new Intent(getApplicationContext(), GPSService.class);
		if (savedInstanceState != null) {
			wasChangingConfigurations = savedInstanceState.getBoolean("wasChangingConfigurations", false);
		}
	    //previousInstance = savedInstanceState;
	}
	
	@Override
	protected void onStart() {
		super.onStart();		
		//wasChangingConfigurations = false;
		//if (previousInstance != null) {
			//wasChangingConfigurations = previousInstance.getBoolean("wasChangingConfigurations", false);
		//}
		//MapSectionFragment mapFragment = (MapSectionFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		//if (mapFragment != null) {
		//	if (mapFragment.debug) {
		//		MainSectionFragment mainFragment = (MainSectionFragment) getSupportFragmentManager().findFragmentById(R.id.main);
		//		mainFragment.
		//	}
		//}
		registerReceiver(receiver, new IntentFilter(GPSService.messageAction));
		startService(gpsIntent);
		bindService(gpsIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
		
		setTabsWidth();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (isChangingConfigurations() && gpsService != null) {
			gpsService.noKill = true;
		}
		if (GPSAlert != null) {
			if (GPSAlert.isShowing()) {
				GPSAlert.dismiss();
			}
		}
		unbindService(gpsServiceConnection);
    	unregisterReceiver(receiver);
    	
    	wasChangingConfigurations = false;
    	//releaseResources();
	}
	
	//private void releaseResources() {
		//gpsService = null;
		//mainFragment = null;
		//mapFragment = null;
		//previousInstance = null;
	//}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	    super.onSaveInstanceState(savedInstanceState);
		try {
			//Log.v(TAG, "isChangingConfigurations: " + isChangingConfigurations());
			savedInstanceState.putBoolean("wasChangingConfigurations", isChangingConfigurations());
		}
		catch (NullPointerException e) {
			//e.printStackTrace();
		}
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_menu, menu);
		//if (previousInstance != null)
		if (mapFragment != null) {
			menu.getItem(0).setChecked(mapFragment.debug);
			if (mainFragment != null) {
				mainFragment.toggleDebug(mapFragment.debug);
			}
		}
	    return true;
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	BugSenseHandler.closeSession(this);
    }

    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
        	//Log.v(TAG, "AppSectionsPagerAdapter getItem");
            switch (i) {
                case 0:
                	mainFragment = new MainSectionFragment();
                    Bundle mainArgs = new Bundle();
                    if (gpsService != null)
                    	mainArgs.putBoolean("tb", gpsService.isTracking());
                    else
                    	mainArgs.putBoolean("tb", false);
                    return mainFragment;
                case 1:
                	mapFragment = new MapSectionFragment();
                    //Bundle mapArgs = new Bundle();
                    //if (gpsService != null)
                    //	mapArgs.putBoolean("tb", gpsService.isTracking());
                    //else
                    //	mapArgs.putBoolean("tb", false);
                    //mapF.setArguments(mapArgs);
                    return mapFragment;
                case 2:
                	return new AboutFragment();
                default:
                    Fragment fragment = new DummySectionFragment();
                    Bundle args = new Bundle();
                    args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
                    fragment.setArguments(args);
                    return fragment;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int i) {
            switch (i) {
            case 0:
                return "Tracking";
            case 1:
            	return "Map";
            case 2:
            	return "About";
            default:
            	return "Section " + (i + 1);
            }
        }
        
        /*public void destroyItem(View container, int position, Object object) {
            super.destroyItem(container, position, object);
            mPageReferenceMap.remove(position);
        }*/
         /**
         * After an orientation change, the fragments are saved in the adapter, and
         * I don't want to double save them: I will retrieve them and put them in my
         * list again here.
         */
        @Override
        public Object instantiateItem(ViewGroup container, int i) {
            switch (i) {
            case 0:
                mainFragment = (MainSectionFragment) super.instantiateItem(container, 0);
                return mainFragment;
            case 1:
                mapFragment = (MapSectionFragment) super.instantiateItem(container, 1);
                return mapFragment;
            default:
            	return super.instantiateItem(container, i);
            }
        }
    }

    public static class MainSectionFragment extends Fragment {

    	//public static Button bb; //, gb, ub, sb, stb;
    	//public static ToggleButton tb, rb, pb;
    	public ToggleButton tb;
    	//public static TextView tt, rt, gt, searching;
    	public TextView searching, accuracy, lattitude, longitude, active, all;
    	public RelativeLayout mainBg, stats;//about, parent, mapPins, help;
    	
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_new, container, false);
            //Bundle args = getArguments();
    		//gb = (Button) findViewById(R.id.goButton);
    		//bb = (Button) rootView.findViewById(R.id.backButton);
    		//sb = (Button) findViewById(R.id.saveButton);
    		//stb = (Button) findViewById(R.id.stopButton);
    		//pb = (ToggleButton) findViewById(R.id.playButton);
    		tb = (ToggleButton) rootView.findViewById(R.id.trackingButton);
    		/*try {
    			tb.setChecked(args.getBoolean("tb"));
    		}
    		catch (NullPointerException e) {
    			e.printStackTrace();
    		}*/
			//Log.v("TB", "created");
    		//rb = (ToggleButton) findViewById(R.id.recordingButton);
    		//tt = (TextView) findViewById(R.id.trackingText);
    		//rt = (TextView) findViewById(R.id.recordingText);
    		//gt = (TextView) findViewById(R.id.goText);
    		//center = (ImageView) findViewById(R.id.center);
    		//mapOver = (ImageView) findViewById(R.id.mapOver);
    		searching = (TextView) rootView.findViewById(R.id.searching);
    		accuracy = (TextView) rootView.findViewById(R.id.accuracy);
    		lattitude = (TextView) rootView.findViewById(R.id.latitude);
    		longitude = (TextView) rootView.findViewById(R.id.longitude);
    		active = (TextView) rootView.findViewById(R.id.active);
    		active.setText("0");
    		all = (TextView) rootView.findViewById(R.id.all);
    		mainBg = (RelativeLayout) rootView.findViewById(R.id.main_bg);
    		stats = (RelativeLayout) rootView.findViewById(R.id.stats);
    		//about = (RelativeLayout) rootView.findViewById(R.id.aboutFrame);
    		//help = (RelativeLayout) findViewById(R.id.helpFrame);
    		//gb.setEnabled(false);
    		//tb.setVisibility(View.INVISIBLE);
    		//rb.setVisibility(View.INVISIBLE);
    		//center.setVisibility(View.INVISIBLE);
    		//mapOver.setVisibility(View.INVISIBLE);
    		//searching.setVisibility(View.INVISIBLE);
    		//toggleDebug(args.getBoolean("debug"));
            return rootView;
        }

        public void toggleDebug(boolean debug) {
    		if (debug) {
    			stats.setVisibility(View.VISIBLE);
    			mainBg.setVisibility(View.INVISIBLE);
    		}
    		else {
    			stats.setVisibility(View.GONE);
    			mainBg.setVisibility(View.VISIBLE);
    		}
        }
        
    	/*@Override
    	public void onSaveInstanceState(Bundle savedInstanceState) {
    		try {
            	MapSectionFragment mapFragment = (MapSectionFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    			savedInstanceState.putBoolean("debug", mapFragment.debug);
    		}
    		catch (NullPointerException e) {
    			//e.printStackTrace();
    		}
    	    super.onSaveInstanceState(savedInstanceState);
    	}*/
    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     */
    public static class DummySectionFragment extends Fragment {

        public static final String ARG_SECTION_NUMBER = "section_number";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_section_dummy, container, false);
            Bundle args = getArguments();
            ((TextView) rootView.findViewById(android.R.id.text1)).setText(
                    getString(R.string.dummy_section_text, args.getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    public static class AboutFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_about, container, false);
            return rootView;
        }
    }

    /*private void showAboutDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		LayoutInflater inflater = getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_about, null);
		builder.setView(view);
		builder.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
        });
        builder.setCancelable(true);
        String version = getStr(R.string.version);
        try {
			version = mContext.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
        //builder.setTitle("About - v" + version);
        //builder.setCustomTitle(customTitleView);
        AlertDialog aboutDialog = builder.create();
        aboutDialog.show();
    }*/

    public static class MapSectionFragment extends SupportMapFragment {

    	private View rootView;
    	private GoogleMap mMap;
    	private GroundOverlay arnoldMap;
    	private boolean debug = false;
    	private LatLngBounds arnoldArboretum;
    	private Circle[] circleO;
    	private Circle[] circleI;
    	private Marker[] marker;
    	private Marker locationMarker;
    	//private NodeManager node[];
    	/*private final GoogleMap.OnCameraChangeListener zoomListener = new GoogleMap.OnCameraChangeListener() {
			@Override
			public void onCameraChange(CameraPosition cPos) {
				if (!dontListen) {
					checkZoom(200);
				}
				dontListen = false;
			}
		};*/
    	private float minZoom = 10, maxZoom = 10, lastZoom = 10;
    	private View zoomIn, zoomOut;
    	private CameraPosition curPos;
    	private boolean shouldReset = true;
    	private boolean dontListen = false;
    	//private MapFragmentListener mCallback;
    	
    	public MapSectionFragment() {
    		super();
    	}
    	/*@Override
    	public void onAttach(Activity activity) {
    		super.onAttach(activity);
    	}

    	@Override
    	public void onDetach() {
    		super.onDetach();
    	}*/

    	@Override
    	public void onLowMemory() {
    		super.onLowMemory();
    		BugSenseHandler.addCrashExtraData("Map", "onLowMemory");
    		Log.v("Map", "onLowMemory");
    	}
    	
    	/*@Override
        public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		Log.v("Map", "onCreate");
    		//if (savedInstanceState != null)
    			//Log.v("Map", "onCreate wasRetained");
    		shouldReset = true;
    	}*/

    	//public static interface MapFragmentListener {
        //    void onMapReady();
        //}

    	//public static MapSectionFragment newInstance() {
        //    return new MapSectionFragment();
        //}

    	/*@Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            try {
                mCallback = (MapFragmentListener) getActivity();
            } catch (ClassCastException e) {
                throw new ClassCastException(getActivity().getClass().getName() + " must implement OnGoogleMapFragmentListener");
            }
        }*/
    	
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    		rootView = super.onCreateView(inflater, container, savedInstanceState);

    		//Log.v("Map", "onCreateView");
    		/*try {
	    		if (savedInstanceState.getBoolean("debug")) {
	    			debug = savedInstanceState.getBoolean("debug");
	    		}
    		}
    		catch (NullPointerException e) {
    			//e.printStackTrace();
    		}*/
            //rootView = inflater.inflate(R.layout.fragment_map, container, false);
            //if (mCallback != null) {
            //    mCallback.onMapReady();
            //}

    		// TODO API14 --> API11
    		ArrayList<View> zOut = new ArrayList<View>(0);
	    	rootView.findViewsWithText(zOut, "Zoom Out", View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
	    	if (zOut.size() > 0) {
		    	zoomOut = zOut.get(0);
	    	}
    		ArrayList<View> zIn = new ArrayList<View>(0);
	    	rootView.findViewsWithText(zIn, "Zoom In", View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
	    	if (zIn.size() > 0) {
		    	zoomIn = zIn.get(0);
	    	}
	    	
    		setRetainInstance(true);
            return rootView;
        }
    	
    	@Override
    	public void onStart() {
    		super.onStart();
    		rootView.post(new Runnable() {
				@Override
				public void run() {
		            setUpMap();
	            	//toggleDebug(debug);
	                try {
	                	toggleDebug(debug);
	                }
	                catch (NullPointerException e) {
	                	e.printStackTrace();
	                	BugSenseHandler.sendException(e);
	                	if (mMap != null) {
		                	mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
		    					@Override
		    					public void onMapLoaded() {
		    						try {
		    							toggleDebug(debug);
		    						}
		    						catch (NullPointerException e) {
		    							e.printStackTrace();
		    							BugSenseHandler.sendException(e);
		    						}
		    		            	mMap.setOnMapLoadedCallback(null);
		    					}
		    				});
	                	}
	                	else {
	                		BugSenseHandler.addCrashExtraData("MapFragment", "mMap is null!");
	                	}
	                }
				}
			});
            //toggleDebug(debug);
    		//updateNodes();
    	}

    	@Override
    	public void onStop() {
    		super.onStop();
    		// TODO memory management, both of these are causing problems
    		removeNodes();
    		//mMap = null;
    	}

        private void setUpMap() {
            if (mMap == null) {
                mMap = getMap();
        		Log.v("Map", "setUpMap");
                if (mMap != null) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                    //mMap.getMyLocation();
                    //42.301361, -71.119089
                    //42.295713, -71.118213
                    //42.297721, -71.112168
                    //42.297724, -71.122265
                	//H: 0.005705
                	//W: 0.009615
                    LatLng arnoldArboretumSW = new LatLng(42.295670, -71.121990);
                    LatLng arnoldArboretumNE = new LatLng(42.301375, -71.112375);
                    //LatLng arnoldArboretumSW = new LatLng(42.296335, -71.121215);
                    //LatLng arnoldArboretumNE = new LatLng(42.300715, -71.113227);
                    arnoldArboretum = new LatLngBounds(arnoldArboretumSW, arnoldArboretumNE);
                    final GroundOverlayOptions arnoldMapOptions = new GroundOverlayOptions()
                    	.image(BitmapDescriptorFactory.fromResource(R.drawable.arnold_arboretum))
                    	.visible(false)
                    	.positionFromBounds(arnoldArboretum);
                    	//.bearing(0.4f);
        			final double px = 0.00001;
                    arnoldArboretumSW = new LatLng(42.295670 + px*3, -71.121990 + px*2);
                    arnoldArboretumNE = new LatLng(42.301375 - px*3, -71.112375 - px*2);
                    arnoldArboretum = new LatLngBounds(arnoldArboretumSW, arnoldArboretumNE);
                    arnoldMap = mMap.addGroundOverlay(arnoldMapOptions);
                    mMap.getUiSettings().setCompassEnabled(true);
                    //mMap.setLocationSource(null);
            		locationMarker = mMap.addMarker((new MarkerOptions())
            				.position(new LatLng(0, 0))
            				.alpha(0.75f)
        					.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker))
        					.anchor(0.5f, 0.5f)
        					.visible(false)
        			);
	            	mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
	        			@Override
	        			public void onCameraChange(CameraPosition cPos) {
	        				if (!dontListen) {
	        					checkZoom(200);
	        				}
	        				dontListen = false;
	        			}
	        		});
                }
            }
        }
        
        public void updateLocation(double lat, double lon, double acc) {
        	if (locationMarker != null) {
        		//Log.v("Map", "isZooming: " + isZooming());
        		// TODO 
	            LatLng latlng = new LatLng(lat, lon);
	        	locationMarker.setPosition(latlng);
	        	if (!locationMarker.isVisible()) {
	        		locationMarker.setVisible(true);
	        	}
        		if (!mMap.isMyLocationEnabled() && !isZooming() && arnoldMap.isVisible() && !dontListen) {
    	        	//int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
		        	//acc = Math.min(Math.max(acc, 0), 10)/10;
		        	//locationMarker.setAlpha(0.75f - (float)acc*0.5f);
		        	//mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
		        	LatLngBounds c = mMap.getProjection().getVisibleRegion().latLngBounds;
		        	double cCLat = mMap.getCameraPosition().target.latitude;
		        	double cCLon = mMap.getCameraPosition().target.longitude;
		        	double cN = c.northeast.latitude + lat - cCLat;
		        	double cS = c.southwest.latitude + lat - cCLat;
		        	double cE = c.northeast.longitude + lon - cCLon;
		        	double cW = c.southwest.longitude + lon - cCLon;
		        	double N = arnoldArboretum.northeast.latitude;
		        	double S = arnoldArboretum.southwest.latitude;
		        	double E = arnoldArboretum.northeast.longitude;
		        	double W = arnoldArboretum.southwest.longitude;
		        	double scrollX = 0;
		        	double scrollY = 0;
		        	//int pxX = 0;
		        	//int pxY = 0;
		        	if (cN > N) {
		    			scrollY = (N - cN);
		    		}
		        	else if (cS < S) {
		    			scrollY = (S - cS);
		    		}
		        	if (cE > E) {
		    			scrollX = (E - cE);
		    		}
		        	else if (cW < W) {
		    			scrollX = (W - cW);
		    		}
		        	lat += scrollY;
		        	lon += scrollX;
		            latlng = new LatLng(lat, lon);
		        	//mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
		           	//    @Override
		           	//    public void onCameraChange(CameraPosition cPos) {
		           	//        mMap.setOnCameraChangeListener(zoomListener);
		           	//    }
		           	//});
		            //mMap.setOnCameraChangeListener(null);
		            dontListen = true;
		        	mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng), 200, new GoogleMap.CancelableCallback() {
						@Override
						public void onFinish() {
				            dontListen = false;
						}
						@Override
						public void onCancel() {
				            dontListen = false;
							//Log.v("Map", "updateLocation cancelled!");
						}
					});
		            //mMap.setOnCameraChangeListener(zoomListener);
		    		//mMap.moveCamera(CameraUpdateFactory.scrollBy(scrollX, scrollY));
        		}
        	}
        }
        
        private boolean isZooming() {
        	//Log.v("Map", "isZooming: " + Math.round(mMap.getCameraPosition().zoom*100) + ", " + Math.round(lastZoom*100));
        	if (Math.round(mMap.getCameraPosition().zoom*100) != lastZoom*100) {
        		return true;
        	}
        	//lastZoom = mMap.getCameraPosition().zoom;
        	return false;
        }

        private void checkCamera(int duration) {
    		if (!mMap.isMyLocationEnabled()) {
    			if (locationMarker.isVisible()) {
    				dontListen = false;
    				//Log.v("Map", "HERE");
    	        	if (!arnoldMap.isVisible()) {
    					arnoldMap.setVisible(true);
    	        	}
    				updateLocation(locationMarker.getPosition().latitude, locationMarker.getPosition().longitude, 0);
    				return;
    			}
	        	LatLngBounds c = mMap.getProjection().getVisibleRegion().latLngBounds;
	        	double cCLat = mMap.getCameraPosition().target.latitude;
	        	double cCLon = mMap.getCameraPosition().target.longitude;
	        	double cN = c.northeast.latitude;
	        	double cS = c.southwest.latitude;
	        	double cE = c.northeast.longitude;
	        	double cW = c.southwest.longitude;
	        	double N = arnoldArboretum.northeast.latitude;
	        	double S = arnoldArboretum.southwest.latitude;
	        	double E = arnoldArboretum.northeast.longitude;
	        	double W = arnoldArboretum.southwest.longitude;
    			//if (N - S < cN - cS - 0.00005 || E - S < cE - cS - 0.00005) {
    			//	fitCamera(false);
    			//	return;
    			//}
	        	double scrollX = 0;
	        	double scrollY = 0;
	        	if (cN > N) {
	    			scrollY = (N - cN);
	    		}
	        	else if (cS < S) {
	    			scrollY = (S - cS);
	    		}
	        	if (cE > E) {
	    			scrollX = (E - cE);
	    		}
	        	else if (cW < W) {
	    			scrollX = (W - cW);
	    		}
	        	if (scrollX != 0 || scrollY != 0) {
		        	cCLat += scrollY;
		        	cCLon += scrollX;
		        	LatLng latlng = new LatLng(cCLat, cCLon);
		        	dontListen = true;
		        	mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng), duration, new GoogleMap.CancelableCallback() {
						@Override
						public void onFinish() {
							if (!arnoldMap.isVisible()) {
								arnoldMap.setVisible(true);
								System.gc();
							}
						}
						@Override
						public void onCancel() {
							Log.v("Map", "checkCamera cancelled!");
						}
					});
	        	}
	        	else if (!arnoldMap.isVisible()) {
					arnoldMap.setVisible(true);
	        	}
    		}
        }
        
        private void checkZoom(int duration) {
    		if (!mMap.isMyLocationEnabled()) {
	        	CameraPosition cPos = mMap.getCameraPosition();
	        	float zoom = 0;
				if (zoomOut != null) {
					zoomIn.setEnabled(Math.round(cPos.zoom*100) < maxZoom*100);
					zoomOut.setEnabled(Math.round(cPos.zoom*100) > minZoom*100);
				}
				if (Math.round(cPos.zoom*100) < minZoom*100) {
					zoom = minZoom;
				}
				else if (Math.round(cPos.zoom*100) > maxZoom*100) {
					zoom = maxZoom;
				}
				if (zoom != 0) {
		        	final int dur = duration;
		        	final float z = zoom;
		        	dontListen = true;
					mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom), duration, new GoogleMap.CancelableCallback() {
						@Override
						public void onFinish() {
							lastZoom = z;
	            			checkCamera(dur);
						}
						@Override
						public void onCancel() {
							Log.v("Map", "checkZoom cancelled!");
						}
					});
				}
				else {
					lastZoom = Math.round(cPos.zoom*100)/100.0f;
        			checkCamera(duration);
					if (!arnoldMap.isVisible()) {
						arnoldMap.setVisible(true);
					}
				}
    		}
        }
        
        public void noLock() {
        	if (locationMarker != null) {
        		if (locationMarker.isVisible()) {
        			locationMarker.setVisible(false);
        			//fitCamera();
        		}
        	}
        }
        
        public void toggleDebug(boolean dbg) {
        	System.gc();
    		//Log.v("Map", "toggleDebug");
        	debug = dbg;
            if (debug) {
            	//mMap.setOnCameraChangeListener(null);
            	//arnoldMap.setTransparency(0.5f);
            	if (arnoldMap.isVisible()) {
            		arnoldMap.setVisible(false);
            	}
            	if (!mMap.isMyLocationEnabled())
            		mMap.setMyLocationEnabled(true);
                if (mMap.getMapType() != GoogleMap.MAP_TYPE_NORMAL)
                	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                mMap.getUiSettings().setAllGesturesEnabled(true);
            	if (!mMap.getUiSettings().isMyLocationButtonEnabled())
            		mMap.getUiSettings().setMyLocationButtonEnabled(true);
                if (zoomOut != null) {
                	zoomOut.setEnabled(true);
                	//zoomOut.setAlpha(1);
                	zoomIn.setEnabled(true);
                	//zoomIn.setAlpha(1);
                }
                //addNodes();
        		updateNodes();
            }
            else {
                removeNodes();
                if (mMap.getMapType() != GoogleMap.MAP_TYPE_NONE)
                	mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
            	arnoldMap.setVisible(false);
            	/*if (rootView.getWidth() == 0) {
            		dontListen = true;
            		mMap.animateCamera(CameraUpdateFactory.scrollBy(1, 0), 1, new GoogleMap.CancelableCallback() {
						@Override
						public void onFinish() {
		            		fitCamera();
						}
						@Override
						public void onCancel() {
							Log.v("Map", "toogleDebug cancelled!");
						}
					});
            	}
            	else {*/
            		fitCamera();
            	//}
            	//arnoldMap.setTransparency(0.0f);
            	if (mMap.isMyLocationEnabled())
            		mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setAllGesturesEnabled(false);
            	//if (!mMap.getUiSettings().isZoomControlsEnabled())
            		//mMap.getUiSettings().setZoomControlsEnabled(true);
                //mMap.getUiSettings().setRotateGesturesEnabled(false);
                //mMap.getUiSettings().setScrollGesturesEnabled(false);
                //mMap.getUiSettings().setTiltGesturesEnabled(false);
            	if (mMap.getUiSettings().isMyLocationButtonEnabled())
            		mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        }
                
        public void fitCamera() {
	    	curPos = mMap.getCameraPosition();
	    	dontListen = true;
    		mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(arnoldArboretum, 0), 1, new GoogleMap.CancelableCallback() {
				@Override
				public void onFinish() {
            		double N = arnoldArboretum.northeast.latitude;
            		double S = arnoldArboretum.southwest.latitude;
            		double E = arnoldArboretum.northeast.longitude;
            		double W = arnoldArboretum.southwest.longitude;
            		double C = arnoldArboretum.getCenter().longitude;
            		Projection p = mMap.getProjection();
            		Point pointN = p.toScreenLocation(new LatLng(N, C));
            		Point pointS = p.toScreenLocation(new LatLng(S, C));
            		Point pointE = p.toScreenLocation(new LatLng(C, E));
            		Point pointW = p.toScreenLocation(new LatLng(C, W));
            		int padding = 0;
            		if (pointN.y > 1) {
        	    		float height = pointS.y - pointN.y;
        	    		float width = rootView.getWidth();
        	    		float realWidth = (rootView.getHeight()/height)*width;
        	    		padding = (int)(width - realWidth)/2;
            		}
            		else if (pointW.x > 1) {
        	    		float height = rootView.getHeight();
        	    		float width = pointE.x - pointW.x;
        	    		float realHeight = (rootView.getWidth()/width)*height;
        	    		padding = (int)(height - realHeight)/2;
            		}
        	    	dontListen = true;
            		mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(arnoldArboretum, padding), 1, new GoogleMap.CancelableCallback() {
						@Override
						public void onFinish() {
                	    	float preMinZoom = minZoom;
                    		minZoom = Math.round(mMap.getCameraPosition().zoom*100)/100.0f;
                    		//maxZoom = Math.max(minZoom + 2, maxZoom);
                    		maxZoom = minZoom + 2;
                    		if (!shouldReset && lastZoom != preMinZoom) {
								//Log.v("Map", "Shouldn't reset");
                    			lastZoom = Math.max(Math.min(curPos.zoom, maxZoom), minZoom);
                    	    	dontListen = true;
                    			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curPos.target, lastZoom), 1, new GoogleMap.CancelableCallback() {
									@Override
									public void onFinish() {
										System.gc();
										checkZoom(1);
									}
									
									@Override
									public void onCancel() {
										Log.v("Map", "fitCamera 2 cancelled!");
									}
								});
                    		}
                    		else {
                        		lastZoom = minZoom;
                        		if (zoomOut != null) {
                        			zoomOut.setEnabled(false);
                        		}
                        		System.gc();
                    	        //mMap.setOnCameraChangeListener(zoomListener);
                    		}
                    		shouldReset = false;
						}
						
						@Override
						public void onCancel() {
							Log.v("Map", "fitCamera 2 cancelled!");
						}
					});
				}
				@Override
				public void onCancel() {
					Log.v("Map", "fitCamera 1 cancelled!");
				}
			});
        }
        
        public void addNodes() {
        	if (debug && gpsService.node != null && marker == null) {
        		NodeManager node[] = gpsService.node;
        		circleO = new Circle[node.length];
        		circleI = new Circle[node.length];
        		marker = new Marker[node.length];
        		for (int i = 0; i < node.length; i++) {
        			LatLng latlon = new LatLng(node[i].getLat(), node[i].getLon());
        			circleO[i] = mMap.addCircle((new CircleOptions())
        					.center(latlon)
        					.radius(node[i].getRadO())
        					.strokeWidth(5)
        					.strokeColor(Color.argb(127, 0, 0, 0))
        					.zIndex(2)
        			);
        			if (node[i].getRadI() > 0) {
        				circleI[i] = mMap.addCircle((new CircleOptions())
	        					.center(latlon)
	        					.radius(node[i].getRadI())
	        					.strokeWidth(2)
	        					.strokeColor(Color.argb(63, 0, 0, 0))
	        					.zIndex(1)
	        			);
        			}
        			marker[i] = mMap.addMarker((new MarkerOptions())
        					.position(latlon)
        					.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker))
        					.anchor(0.5f, 0.5f)
        			);
        		}
        	}
        }

        public void removeNodes() {
        	if (marker != null) {
	    		for (int i = 0; i < marker.length; i++) {
					circleO[i].remove();
					if (circleI[i] != null)
						circleI[i].remove();
					marker[i].remove();
	    		}
        	}
        	circleO = null;
        	circleI = null;
        	marker = null;
        }
        
        public void updateNodes() {
        	if (debug && gpsService.node != null) {
        		NodeManager node[] = gpsService.node;
        		if (marker == null) {
        			addNodes();
        			//return;
        		}
        		else if (node.length != marker.length) {
	        		removeNodes();
	        		addNodes();
	        	}
	        	int color, colorS;
				int colorI = getResources().getColor(R.color.holo_blue_bright);
				int colorA = getResources().getColor(R.color.holo_red_bright);
	    		for (int i = 0; i < node.length; i++) {
	            	LatLng latlon = new LatLng(node[i].getLat(), node[i].getLon());
	    			if (node[i].isPlaying()) {
	    				color = Color.argb(63, Color.red(colorA), Color.green(colorA), Color.blue(colorA));
	    				colorS = Color.argb(127, Color.red(colorA), Color.green(colorA), Color.blue(colorA));
	    			}
	    			else {
	    				color = Color.argb(63, Color.red(colorI), Color.green(colorI), Color.blue(colorI));
	    				colorS = Color.argb(127, Color.red(colorI), Color.green(colorI), Color.blue(colorI));
	    			}
	    			if (!circleO[i].isVisible())
	    				circleO[i].setVisible(true);
	    			if (circleO[i].getCenter().latitude != latlon.latitude || circleO[i].getCenter().longitude != latlon.longitude)
	    				circleO[i].setCenter(latlon);
	    			if (circleO[i].getRadius() != node[i].getRadO())
	    				circleO[i].setRadius(node[i].getRadO());
	            	if (circleI[i] != null) {
	        			if (circleO[i].getFillColor() != color)
	        				circleO[i].setFillColor(color);
		    			if (!circleI[i].isVisible())
		    				circleI[i].setVisible(true);
		    			if (circleI[i].getCenter().latitude != latlon.latitude || circleI[i].getCenter().longitude != latlon.longitude)
	        				circleI[i].setCenter(latlon);
	        			if (circleI[i].getRadius() != node[i].getRadI())
	        				circleI[i].setRadius(node[i].getRadI());
	        			if (circleI[i].getFillColor() != color)
	        				circleI[i].setFillColor(color);
	            	}
	    			else {
	        			if (circleO[i].getFillColor() != colorS)
	        				circleO[i].setFillColor(colorS);
	    			}
	    			if (!marker[i].isVisible())
	    				marker[i].setVisible(true);
	            	if (marker[i].getPosition().latitude != latlon.latitude || marker[i].getPosition().longitude != latlon.longitude)
	            		marker[i].setPosition(latlon);
	        	}
        	}
        }
    }

    public void onTrackingToggled(View view) {
    	ToggleButton tb = (ToggleButton) view;
		boolean on = tb.isChecked();

		if (on) {
			//final Handler handler = new Handler();
			gpsService.startTracking();
			handler.post(GPSDialog);
		}
		else {
			gpsService.stopTracking();
		}
	}

	//public void onAbout(MenuItem item) {
		//showAboutDialog();
	//}

	public void onReset(MenuItem item) {
    	NodeManager[] node = gpsService.node;
		for (int i = 0; i < node.length; i++) {
			if (node[i].isPlaying())
				node[i].stop();
		}
		NodeManager.reset();
	}

	public void onDebug(MenuItem item) {
		if (mapFragment != null && mainFragment != null) {
			mapFragment.debug = !mapFragment.debug;
			mapFragment.toggleDebug(mapFragment.debug);
			mainFragment.toggleDebug(mapFragment.debug);
			item.setChecked(mapFragment.debug);
		}
	}

    private String getStr(int id) {
		return getResources().getString(id);
	}
    
    private void setTabsWidth() {
    	//try {
			//DisplayMetrics displaymetrics = new DisplayMetrics();
			//getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			//int screenWidth = displaymetrics.widthPixels;
			//final ActionBar actionBar = getSupportActionBar();
	    	Window window = getWindow();
		    final View rootView = window.getDecorView();
		    int resId = getResources().getIdentifier("action_bar", "id", "android");
		    final ViewGroup actionBarView = (ViewGroup) rootView.findViewById(resId);
		    actionBarView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
		    	@Override
				public boolean onPreDraw() {
		        	try {
			        	Log.v(TAG, "TabWidth");
				    	float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					    //ViewGroup tabScrollView = (ViewGroup) actionBarView.getChildAt(1);
					    HorizontalScrollView tabScrollView = (HorizontalScrollView) actionBarView.getChildAt(1);
					    //tabScrollView.setScrollContainer(false);
					    ViewGroup optionsView = (ViewGroup) actionBarView.getChildAt(2);
					    LinearLayout tabBarView = (LinearLayout) tabScrollView.getChildAt(0);
					    ViewGroup tabView = (ViewGroup) tabBarView.getChildAt(0);
					    //ViewGroup.LayoutParams oLayoutParams = (ViewGroup.LayoutParams) optionsView.getLayoutParams();
					    //oLayoutParams.setMargins(0, 0, 0, 0);
					    //optionsView.setLayoutParams(oLayoutParams);
					    android.app.ActionBar.LayoutParams tsLayoutParams = (android.app.ActionBar.LayoutParams) tabScrollView.getLayoutParams();
					    tsLayoutParams.width = rootView.getWidth() - optionsView.getWidth();
					    tsLayoutParams.setMargins(0, 0, 0, 0);
					    tabScrollView.setLayoutParams(tsLayoutParams);
					    FrameLayout.LayoutParams tbLayoutParams = (FrameLayout.LayoutParams) tabBarView.getLayoutParams();
					    tbLayoutParams.width = rootView.getWidth() - optionsView.getWidth();
					    tsLayoutParams.gravity = Gravity.FILL_HORIZONTAL;
					    LinearLayout.LayoutParams tabLayoutParams = (LinearLayout.LayoutParams) tabView.getLayoutParams();
					    tabLayoutParams.width = (rootView.getWidth() - optionsView.getWidth() - (int)px)/3;
					    tabView.setLayoutParams(tabLayoutParams);
					    tabView.setPadding(0, 0, 0, 0);
					    //tabLayoutParams.width = tabScrollView.getWidth();
					    //tabView.setLayoutParams(tabLayoutParams);
					    //final View tabView = actionBar.getTabAt(0).getCustomView();
						//Log.v("TAB", "" + actionBarView.getChildCount());
						//Log.v("TAB", "" + tabScrollView.toString());
						//Log.v("TAB", "" + tabScrollView.getWidth());
						//Log.v("TAB", "" + tsLayoutParams.gravity);
						//Log.v("TAB", "" + tabBarView.getWidth());
						//Log.v("TAB", "" + tabView.getWidth());
						//Log.v("TAB", "" + tabView.getMinimumWidth());
						//final View tabContainerView = (View) tabView.getParent();
						//final int tabPadding = tabContainerView.getPaddingLeft() + tabContainerView.getPaddingRight();
						//final int tabs = actionBar.getTabCount();
						//for(int i=0 ; i < tabs ; i++){
					    //View tab = actionBar.getTabAt(i).getCustomView();
					    //TextView text1 = (TextView) tab.findViewById(R.id.text1);
					    //text1.setMaxWidth(screenWidth/tabs-tabPadding-1);
			            // make sure it is not called anymore 
			        	actionBarView.getViewTreeObserver().removeOnPreDrawListener(this);
		        	}
		        	catch (NullPointerException e) {
		        		//e.printStackTrace();
						//BugSenseHandler.sendException(e);
		    		}
					catch (Exception e) {
			        	actionBarView.getViewTreeObserver().removeOnPreDrawListener(this);
						e.printStackTrace();
						BugSenseHandler.sendException(e);
					}
				    return true;
		        }
		    });
    	//}
		//catch (Exception e) {
		//	e.printStackTrace();
		//	BugSenseHandler.sendException(e);
		//}
	}

	@Override
	public void onTabSelected(Tab tab,
			android.support.v4.app.FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab,
			android.support.v4.app.FragmentTransaction ft) {
	}

	@Override
	public void onTabReselected(Tab tab,
			android.support.v4.app.FragmentTransaction ft) {
	}
	
	private void enableEmbeddedTabs(Object actionBar) {
	    try {
	        Method setHasEmbeddedTabsMethod = actionBar.getClass().getDeclaredMethod("setHasEmbeddedTabs", boolean.class);
	        setHasEmbeddedTabsMethod.setAccessible(true);
	        setHasEmbeddedTabsMethod.invoke(actionBar, true);
	    }
	    catch (Exception e) {
	        Log.e(TAG, "Error marking actionbar embedded", e);
	        BugSenseHandler.sendException(e);
	    }
	}
}