package com.minimv.soundwalker.otherorder;

import java.util.Locale;

import com.minimv.soundwalker.otherorder.R;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageSlider extends FragmentActivity {

    ImageSliderAdapter imageSliderAdapter;

    private ViewPager mViewPager;
    private static String[] plants;
    private static String[] landmarks;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_demo);

        int position = getIntent().getExtras().getInt("position");

        imageSliderAdapter = new ImageSliderAdapter(getSupportFragmentManager());

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        //actionBar.setDisplayShowTitleEnabled(false);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(imageSliderAdapter);
        mViewPager.setCurrentItem(position);
        mViewPager.setOffscreenPageLimit(2);
        plants = getResources().getStringArray(R.array.plants);
        landmarks = getResources().getStringArray(R.array.landmarks);
    }

    @SuppressWarnings("deprecation")
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed in the action bar.
                // Create a simple intent that starts the hierarchical parent activity and
                // use NavUtils in the Support Package to ensure proper handling of Up.
                Intent upIntent = new Intent(this, MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is not part of the application's task, so create a new task
                    // with a synthesized back stack.
                    TaskStackBuilder.from(this)
                            // If there are ancestor activities, they should be added here.
                            .addNextIntent(upIntent)
                            .startActivities();
                    finish();
                } else {
                    // This activity is part of the application's task, so simply
                    // navigate up to the hierarchical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ImageSliderAdapter extends FragmentStatePagerAdapter {

        public ImageSliderAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new ImageFragment();
            Bundle args = new Bundle();
            args.putInt("position", i);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            //return plants.length;
        	return 29 + 6;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	if (position >= 29) {
        		return landmarks[position - 29];
        	}
            return plants[position].replaceFirst(" \\d", "");
        }
    }

    public static class ImageFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_image_slider, container, false);
            int position = getArguments().getInt("position");
            String name = "";
        	if (position >= 29) {
        		name = landmarks[position - 29].toLowerCase(Locale.US).replace(' ', '_');
        	}
        	else {
        		name = plants[position].toLowerCase(Locale.US).replace(' ', '_');
        	}
            int id = getResources().getIdentifier(name, "drawable", "com.minimv.soundwalker.otherorder");
            if (id != 0) {
                ImageView image = (ImageView) rootView.findViewById(R.id.image);
            	image.setImageResource(id);
            }
            else {
            	((TextView) rootView.findViewById(android.R.id.text1)).setText(name);
            }
            return rootView;
        }
    }
}