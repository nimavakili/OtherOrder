package com.minimv.soundwalker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
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
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.SupportMapFragment;
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

public class MainActivity extends SherlockFragmentActivity implements ActionBar.TabListener {

    private AppSectionsPagerAdapter mAppSectionsPagerAdapter;
    private ViewPager mViewPager;
    private static MapSectionFragment mapFragment;
    //private static Bundle previousInstance;

    private Context mContext;
	private static String TAG = "GPSActiviy";
	private GPSService gpsService;
	private ServiceConnection gpsServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			gpsService = ((GPSService.LocationServiceBinder) service).getService();
			Log.v(TAG, "GPS service is tracking: " + gpsService.isTracking());
			if (gpsService.isTracking()) {
				tb.setChecked(true);
			}
			handler.post(GPSDialog);
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			gpsService = null;
		}
	};
	float scale;
	int width;
	//public static Button bb; //, gb, ub, sb, stb;
	//public static ToggleButton tb, rb, pb;
	public static ToggleButton tb;
	//public static TextView tt, rt, gt, searching;
	public static TextView searching, accuracy, lattitude, longitude, active, all;
	public static RelativeLayout mainBg, stats;//about, parent, mapPins, help;
	private Runnable GPSDialog;
	private Handler handler;
	public static AlertDialog GPSAlert;
	private Intent gpsIntent;
	private BroadcastReceiver receiver;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());
        final ActionBar actionBar = getSupportActionBar();
        //pre-ICS
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
        
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //mViewPager.setCurrentItem(2);

		//scale = getResources().getDisplayMetrics().widthPixels/320.0f;
		//width = getResources().getDisplayMetrics().widthPixels;
		gpsIntent = new Intent(getApplicationContext(), GPSService.class);
		mContext = this;
		handler = new Handler();
		GPSDialog = new Runnable() {
			@Override
			public void run() {
				setTabsWidth();
				if (gpsService != null) {
					if (gpsService.GPSDisabled()) {
						//searching.setVisibility(View.INVISIBLE);
						searching.setText(getStr(R.string.gps_disabled));
		            	if (GPSAlert != null) {
		            		if (GPSAlert.isShowing()) {
		            			return;
		            		}
		            	}
						AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			            builder.setMessage(getStr(R.string.gps_disabled_prompt));
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
					else {
						if (!gpsService.gotLock())
							//searching.setVisibility(View.VISIBLE);
							searching.setText(getStr(R.string.searching));
					}
				}
			}
		};
	    receiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	            String messageLock = intent.getStringExtra(GPSService.messageLock);
	            double[] messageLoc = intent.getDoubleArrayExtra(GPSService.messageLoc);
	            int messageAct = intent.getIntExtra(GPSService.messageAct, -1000);
	            int messageAll = intent.getIntExtra(GPSService.messageAll, -1000);
	            if (messageLock != null) {
		            //Log.v("Receive", messageLock);
		            if (messageLock.equals("No")) {
		            	mapFragment.noLock();
		            	//mapFragment.updateLocation(42.296335, -71.121215, 0);
		            	//searching.setVisibility(View.VISIBLE);
		            	if (gpsService.GPSDisabled()) {
		            		searching.setText(getStr(R.string.gps_disabled));
		            	}
		            	else {
		            		searching.setText(getStr(R.string.searching));
		            	}
		            	accuracy.setText("---");
		            	lattitude.setText("---");
		            	longitude.setText("---");
		            	active.setText("0");
		            	//all.setText("---");
		            }
		            else if (messageLock.equals("Yes")) {
		            	//searching.setVisibility(View.INVISIBLE);
		            	searching.setText(getStr(R.string.lock));
		            }
	            }
	            if (messageLoc != null) {
	            	String lat = String.valueOf(messageLoc[0]);
	            	String lon = String.valueOf(messageLoc[1]);
	            	String acc = String.valueOf(messageLoc[2]);
		            //Log.v("Receive", acc);
	            	lattitude.setText(lat);
	            	longitude.setText(lon);
	            	accuracy.setText(acc);
	            	//mapFragment.updateLocation(42.296335, -71.121215, 0);
	            	mapFragment.updateLocation(messageLoc[0], messageLoc[1], messageLoc[2]);
	            }
	            if (messageAct != -1000) {
	            	String act = String.valueOf(messageAct);
		            //Log.v("Receive", act);
	            	active.setText(act);
	        		if (mapFragment != null) {
	        			if (mapFragment.debug) {
	        				mapFragment.updateNodes();
	        			}
	        		}
	            }
	            if (messageAll != -1000) {
	            	String allS = String.valueOf(messageAll);
		            //Log.v("Receive", allS);
	            	all.setText(allS);
	            }
	        }
	    };
	    
	    //previousInstance = savedInstanceState;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		//wasChangingConf = !previousInstance.getBoolean("wasChangingConfigurations", true);
		//if (mapFragment != null) {
		//	mapFragment.toggleDebug(mapFragment.debug);
		//}
		startService(gpsIntent);
		bindService(gpsIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
		registerReceiver(receiver, new IntentFilter(GPSService.messageAction));
	}

	@Override
	protected void onPause() {
		super.onPause();
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
	}
	
	/*@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		try {
			savedInstanceState.putBoolean("wasChangingConfigurations", isChangingConfigurations());
		}
		catch (NullPointerException e) {
			//e.printStackTrace();
		}
	    super.onSaveInstanceState(savedInstanceState);
	}*/
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_menu, menu);
		//if (previousInstance != null)
		if (mapFragment != null) {
			menu.getItem(0).setChecked(mapFragment.debug);
		}
	    return true;
	}
    
    //@Override
    //public void onDestroy() {
    //	super.onDestroy();
    //}

    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new MainSectionFragment();
                case 1:
                	mapFragment = new MapSectionFragment();
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
    }

    public static class MainSectionFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_new, container, false);
    		//gb = (Button) findViewById(R.id.goButton);
    		//bb = (Button) rootView.findViewById(R.id.backButton);
    		//sb = (Button) findViewById(R.id.saveButton);
    		//stb = (Button) findViewById(R.id.stopButton);
    		//pb = (ToggleButton) findViewById(R.id.playButton);
    		tb = (ToggleButton) rootView.findViewById(R.id.trackingButton);
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
    		try {
	    		if (savedInstanceState.getBoolean("debug")) {
	    			stats.setVisibility(View.VISIBLE);
	    			mainBg.setVisibility(View.INVISIBLE);
	    		}
    		}
    		catch (NullPointerException e) {
    			//e.printStackTrace();
    		}
            return rootView;
        }
        
    	@Override
    	public void onSaveInstanceState(Bundle savedInstanceState) {
    		try {
    			savedInstanceState.putBoolean("debug", mapFragment.debug);
    		}
    		catch (NullPointerException e) {
    			//e.printStackTrace();
    		}
    	    super.onSaveInstanceState(savedInstanceState);
    	}
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

    private void showAboutDialog() {
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
        /*String version = getStr(R.string.version);
        try {
			version = mContext.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}*/
        //builder.setTitle("About - v" + version);
        //builder.setCustomTitle(customTitleView);
        AlertDialog aboutDialog = builder.create();
        aboutDialog.show();
    }

    public static class MapSectionFragment extends Fragment {

    	private View rootView;
    	private GoogleMap mMap;
    	private GroundOverlay mapOverlay;
    	public boolean debug = false;
    	private LatLngBounds arnoldArboretum;
    	private Circle[] circleO;
    	private Circle[] circleI;
    	private Marker[] marker;
    	private Marker locationMarker;
    	private NodeManager node[];
    	GoogleMap.OnCameraChangeListener zoomListener;
    	float minZoom = 10, maxZoom = 10, lastZoom = 10;
    	private View zoomIn, zoomOut;
    	private CameraPosition curPos;
    	private boolean shouldReset = true;
    	
    	/*@Override
    	public void onAttach(Activity activity) {
    		super.onAttach(activity);
    	}

    	@Override
    	public void onDetach() {
    		super.onDetach();
    	}*/

    	@Override
        public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		Log.v("Map", "onCreate");
    		//if (savedInstanceState != null)
    			//Log.v("Map", "onCreate wasRetained");
    		shouldReset = true;
    	}
    	
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    		Log.v("Map", "onCreateView");
    		//if (savedInstanceState != null)
    			//Log.v("Map", "onCreateView wasRetained");
    		setRetainInstance(true);
            rootView = inflater.inflate(R.layout.fragment_map, container, false);
            setUpMap();
            toggleDebug(debug);
            return rootView;
        }

        private void setUpMap() {
            if (mMap == null) {
            	SupportMapFragment mapFragment = (SupportMapFragment) getFragmentManager().findFragmentById(R.id.map);
            	mapFragment.setRetainInstance(true);
                mMap = mapFragment.getMap();
                if (mMap != null) {
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
                    GroundOverlayOptions arnoldMap = new GroundOverlayOptions()
                    	.image(BitmapDescriptorFactory.fromResource(R.drawable.arnold_arboretum))
                    	.positionFromBounds(arnoldArboretum);
                    	//.bearing(0.4f);
        			double px = 0.00001;
                    arnoldArboretumSW = new LatLng(42.295670 + px*3, -71.121990 + px*2);
                    arnoldArboretumNE = new LatLng(42.301375 - px*3, -71.112375 - px*2);
                    arnoldArboretum = new LatLngBounds(arnoldArboretumSW, arnoldArboretumNE);
                    mapOverlay = mMap.addGroundOverlay(arnoldMap);
                    mMap.getUiSettings().setCompassEnabled(true);
                    //mMap.setLocationSource(null);
            		locationMarker = mMap.addMarker((new MarkerOptions())
            				.position(new LatLng(0, 0))
            				.alpha(0.75f)
        					.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker))
        					.anchor(0.5f, 0.5f)
        					.visible(false)
        			);
            		zoomListener = new GoogleMap.OnCameraChangeListener() {
						@Override
						public void onCameraChange(CameraPosition cPos) {
		           	    	Log.v("Map", "onCameraChange");
							if (Math.round(cPos.zoom*100) < minZoom*100 || Math.round(cPos.zoom*100) > maxZoom*100) {
					        	mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
					           	    @Override
					           	    public void onCameraChange(CameraPosition cPos) {
										lastZoom = Math.round(cPos.zoom*100)/100.0f;
					           			checkCamera();
					           	        mMap.setOnCameraChangeListener(zoomListener);
					           	    }
					           	});
							}
							else {
								lastZoom = Math.round(cPos.zoom*100)/100.0f;
			           			checkCamera();
							}
							if (zoomOut != null) {
								zoomIn.setEnabled(Math.round(cPos.zoom*100) < maxZoom*100);
								zoomOut.setEnabled(Math.round(cPos.zoom*100) > minZoom*100);
							}
							if (Math.round(cPos.zoom*100) < minZoom*100) {
								mMap.animateCamera(CameraUpdateFactory.zoomTo(minZoom), 200, null);
							}
							else if (Math.round(cPos.zoom*100) > maxZoom*100) {
								mMap.animateCamera(CameraUpdateFactory.zoomTo(maxZoom), 200, null);
							}
						}
					};
                }
            }
        }
        
        public void updateLocation(double lat, double lon, double acc) {
        	if (locationMarker != null) {
        		//Log.v("Map", "isZooming: " + isZooming());
        		if (!mMap.isMyLocationEnabled() && !isZooming()) {
    	        	//int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
		            LatLng latlng = new LatLng(lat, lon);
		        	locationMarker.setPosition(latlng);
		        	acc = Math.min(Math.max(acc, 0), 10)/10;
		        	locationMarker.setAlpha(0.75f - (float)acc*0.5f);
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
		            mMap.setOnCameraChangeListener(null);
		        	mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
		            mMap.setOnCameraChangeListener(zoomListener);
		    		//mMap.moveCamera(CameraUpdateFactory.scrollBy(scrollX, scrollY));
		        	if (!locationMarker.isVisible()) {
		        		locationMarker.setVisible(true);
		        	}
        		}
        	}
        }
        
        private boolean isZooming() {
        	//Log.v("Map", "isZooming: " + Math.round(mMap.getCameraPosition().zoom*100) + ", " + Math.round(lastZoom*100));
        	if (Math.round(mMap.getCameraPosition().zoom*100) != Math.round(lastZoom*100)) {
        		return true;
        	}
        	//lastZoom = mMap.getCameraPosition().zoom;
        	return false;
        }

        private void checkCamera() {
        		if (!mMap.isMyLocationEnabled()) {
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
			            //mMap.setOnCameraChangeListener(null);
			        	mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng), 200, null);
			            //mMap.setOnCameraChangeListener(zoomListener);
		        	}
        		}
        }
        
        public void noLock() {
        	if (locationMarker != null) {
        		if (locationMarker.isVisible()) {
        			locationMarker.setVisible(false);
        			fitCamera();
        		}
        	}
        }
        
        public void toggleDebug(boolean dbg) {
    		Log.v("Map", "toggleDebug");
        	debug = dbg;
            if (debug) {
            	mMap.setOnCameraChangeListener(null);
            	mapOverlay.setTransparency(0.5f);
                mMap.setMyLocationEnabled(true);
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                mMap.getUiSettings().setAllGesturesEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
			    zoomOut.setEnabled(true);
			    zoomOut.setAlpha(1);
			    zoomIn.setEnabled(true);
			    zoomIn.setAlpha(1);
                addNodes();
        		updateNodes();
            }
            else {
            	mMap.setOnCameraChangeListener(zoomListener);
            	if (rootView.getWidth() == 0) {
            		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
	            	    @Override
	            	    public void onCameraChange(CameraPosition arg) {
	            	    	//if (!debug) {
	            	    	fitCamera();
	            	    	//}
	            	        //mMap.setOnCameraChangeListener(zoomListener);
	            	    }
	            	});
            		mMap.moveCamera(CameraUpdateFactory.scrollBy(1, 0));
            	}
            	else {
            		fitCamera();
            		//checkCamera();
            	}
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(arnoldArboretum.getCenter(), 17.0f));
            	mapOverlay.setTransparency(0.0f);
                mMap.setMyLocationEnabled(false);
                mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                mMap.getUiSettings().setAllGesturesEnabled(false);
                mMap.getUiSettings().setZoomControlsEnabled(true);
                //mMap.getUiSettings().setRotateGesturesEnabled(false);
                //mMap.getUiSettings().setScrollGesturesEnabled(false);
                //mMap.getUiSettings().setTiltGesturesEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                removeNodes();
            }
        }
        
        public void fitCamera() {
	    	curPos = mMap.getCameraPosition();
    		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
        	    @Override
        	    public void onCameraChange(CameraPosition arg) {
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
            		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                	    @Override
                	    public void onCameraChange(CameraPosition arg) {
                	    	float preMinZoom = minZoom;
                    		minZoom = Math.round(mMap.getCameraPosition().zoom*100)/100.0f;
                    		//maxZoom = Math.max(minZoom + 2, maxZoom);
                    		maxZoom = minZoom + 2;
                    		if (!shouldReset) {
                    			if (lastZoom == preMinZoom) {
                    				lastZoom = minZoom;
                    			}
                    			else {
                    				lastZoom = Math.max(Math.min(curPos.zoom, maxZoom), minZoom);
                    			}
                        		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                            	    @Override
                            	    public void onCameraChange(CameraPosition arg) {
                            			checkCamera();
                            	        mMap.setOnCameraChangeListener(zoomListener);
                            	    }
                            	});
                    			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curPos.target, lastZoom));
                    		}
                    		else {
                        		lastZoom = minZoom;
							    zoomOut.setEnabled(false);
                    	        mMap.setOnCameraChangeListener(zoomListener);
                    		}
                    		shouldReset = false;
                	    }
                	});
            		mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(arnoldArboretum, padding));
        	    }
        	});
    		mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(arnoldArboretum, 0));
        }
        
        public void addNodes() {
        	if (GPSService.node != null) {
        		node = GPSService.node;
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
        					.visible(false)
        			);
        			if (node[i].getRadI() > 0) {
        				circleI[i] = mMap.addCircle((new CircleOptions())
	        					.center(latlon)
	        					.radius(node[i].getRadI())
	        					.strokeWidth(2)
	        					.strokeColor(Color.argb(63, 0, 0, 0))
	        					.zIndex(1)
	        					.visible(false)
	        			);
        			}
        			marker[i] = mMap.addMarker((new MarkerOptions())
        					.position(latlon)
        					.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker))
        					.anchor(0.5f, 0.5f)
        					.visible(false)
        			);
        		}
        	}
        }

        public void removeNodes() {
        	if (marker != null) {
	    		for (int i = 0; i < marker.length; i++) {
					circleO[i].remove();
					circleI[i].remove();
					marker[i].remove();
	    		}
        	}
        	circleO = null;
        	circleI = null;
        	marker = null;
        }
        
        public void updateNodes() {
        	if (debug && marker != null) {
	        	int color, colorS;
				int colorI = getResources().getColor(R.color.holo_blue_bright);
				int colorA = getResources().getColor(R.color.holo_red_bright);
	        	node = GPSService.node;
	        	if (node.length != marker.length) {
	        		removeNodes();
	        		addNodes();
	        	}
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
		boolean on = tb.isChecked();

		if (on) {
			//final Handler handler = new Handler();
			handler.post(GPSDialog);
			gpsService.startTracking();
		}
		else {
			gpsService.stopTracking();
		}
	}

	public void onAbout(MenuItem item) {
		showAboutDialog();
	}

	public void onReset(MenuItem item) {
		NodeManager.reset();
    	NodeManager[] node = GPSService.node;
		for (int i = 0; i < node.length; i++) {
			if (node[i].isPlaying())
				node[i].stopReset();
		}
	}

	public void onDebug(MenuItem item) {
		mapFragment.debug = !mapFragment.debug;
		mapFragment.toggleDebug(mapFragment.debug);
		item.setChecked(mapFragment.debug);
		if (mapFragment.debug) {
			stats.setVisibility(View.VISIBLE);
			mainBg.setVisibility(View.INVISIBLE);
		}
		else {
			stats.setVisibility(View.INVISIBLE);
			mainBg.setVisibility(View.VISIBLE);
		}
	}

    private String getStr(int id) {
		return getResources().getString(id);
	}
    
    private void setTabsWidth() {
    	try {
			//DisplayMetrics displaymetrics = new DisplayMetrics();
			//getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			//int screenWidth = displaymetrics.widthPixels;
			//final ActionBar actionBar = getSupportActionBar();
	    	float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
	    	Window window = getWindow();
		    View rootView = window.getDecorView();
		    int resId = getResources().getIdentifier("action_bar", "id", "android");
		    ViewGroup actionBarView = (ViewGroup) rootView.findViewById(resId);
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
    	}
		catch (Exception e) {
			e.printStackTrace();
		}
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
	    }
	}
}