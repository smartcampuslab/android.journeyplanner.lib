/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either   express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.jp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import eu.trentorise.smartcampus.ac.AACException;
import eu.trentorise.smartcampus.android.feedback.activity.FeedbackFragmentActivity;
import eu.trentorise.smartcampus.android.feedback.utils.FeedbackFragmentInflater;
import eu.trentorise.smartcampus.jp.custom.map.ParkingObjectMapItemTapListener;
import eu.trentorise.smartcampus.jp.custom.map.ParkingsInfoDialog;
import eu.trentorise.smartcampus.jp.custom.map.ParkingsItemizedOverlay;
import eu.trentorise.smartcampus.jp.helper.JPHelper;
import eu.trentorise.smartcampus.jp.helper.JPParamsHelper;
import eu.trentorise.smartcampus.jp.model.ParkingSerial;

public class ParkingMapActivity extends BaseActivity implements ParkingObjectMapItemTapListener {

	public final static String ARG_PARKINGS = "parkings";
	public final static String ARG_PARKING_FOCUSED = "parking_focused";
	public final static int REQUEST_CODE = 1986;

	private final static int FOCUSED_ZOOM = 18;

	private MapView mapView = null;
	private MyLocationOverlay mMyLocationOverlay = null;
	ParkingsItemizedOverlay mItemizedoverlay = null;

	private ArrayList<ParkingSerial> parkingsList;
	private ParkingSerial focusedParking;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.mapcontainer_jp);

		if (getIntent().getSerializableExtra(ARG_PARKINGS) != null) {
			parkingsList = (ArrayList<ParkingSerial>) getIntent().getSerializableExtra(ARG_PARKINGS);
		}

		if (getIntent().getSerializableExtra(ARG_PARKING_FOCUSED) != null) {
			focusedParking = (ParkingSerial) getIntent().getSerializableExtra(ARG_PARKING_FOCUSED);
		}

		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true); // back arrow
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD); // tabs
		actionBar.setTitle(R.string.title_smart_check);

		setContent();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		MenuItem item = menu.add(Menu.CATEGORY_SYSTEM, R.id.menu_item_list, 1, R.string.menu_item_parking_list);
		item.setIcon(R.drawable.ic_list);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		} else if (item.getItemId() == R.id.menu_item_list) {
			finish();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void setContent() {

		FeedbackFragmentInflater.inflateHandleButtonInRelativeLayout(this,
				(RelativeLayout) findViewById(R.id.mapcontainer_relativelayout_jp));

		mapView = new MapView(this, getResources().getString(R.string.maps_api_key));
		// mapView = MapManager.getMapView();
		// setContentView(R.layout.mapcontainer);

		ViewGroup view = (ViewGroup) findViewById(R.id.mapcontainer);
		view.removeAllViews();
		view.addView(mapView);

		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		mapView.getController().setZoom(15);

		List<Overlay> listOfOverlays = mapView.getOverlays();
		mItemizedoverlay = new ParkingsItemizedOverlay(this, mapView);
		mItemizedoverlay.setMapItemTapListener(this);
		listOfOverlays.add(mItemizedoverlay);

		mMyLocationOverlay = new MyLocationOverlay(getApplicationContext(), mapView) {
			@Override
			protected void drawMyLocation(Canvas canvas, MapView mapView, Location lastFix, GeoPoint myLocation, long when) {
				Projection p = mapView.getProjection();
				float accuracy = p.metersToEquatorPixels(lastFix.getAccuracy());
				Point loc = p.toPixels(myLocation, null);
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				// paint.setColor(Color.BLUE);
				paint.setColor(Color.parseColor(getApplicationContext().getResources().getString(R.color.jpappcolor)));

				if (accuracy > 10.0f) {
					paint.setAlpha(50);
					canvas.drawCircle(loc.x, loc.y, accuracy, paint);
					// border
					paint.setAlpha(200);
					paint.setStyle(Paint.Style.STROKE);
					canvas.drawCircle(loc.x, loc.y, accuracy, paint);
				}

				Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.me).copy(
						Bitmap.Config.ARGB_8888, true);
				canvas.drawBitmap(bitmap, loc.x - (bitmap.getWidth() / 2), loc.y - bitmap.getHeight(), null);
			}
		};

		listOfOverlays.add(mMyLocationOverlay);

		// // move map to my location at first fix
		// mMyLocationOverlay.runOnFirstFix(new Runnable() {
		// public void run() {
		// mapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
		// // load with radius? Not for now.
		// }
		// });

		// LOAD
		for (ParkingSerial o : parkingsList) {
			mItemizedoverlay.addOverlay(o);
		}
		mItemizedoverlay.populateAll();
		mapView.invalidate();

		if (focusedParking != null) {
			int lat = (int) (focusedParking.getPosition()[0] * 1E6);
			int lon = (int) (focusedParking.getPosition()[1] * 1E6);
			GeoPoint focus = new GeoPoint(lat, lon);
			mapView.getController().animateTo(focus);
			mapView.getController().setZoom(FOCUSED_ZOOM);
		}
	}

	@Override
	public void onResume() {
		// centeredOnMe = false;
		mMyLocationOverlay.enableMyLocation();
		super.onResume();
	}

	@Override
	public void onPause() {
		mMyLocationOverlay.disableMyLocation();
		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onParkingObjectTap(ParkingSerial parking) {
		ParkingsInfoDialog parkingsInfoDialog = new ParkingsInfoDialog();
		Bundle args = new Bundle();
		args.putSerializable(ParkingsInfoDialog.ARG_PARKING, parking);
		parkingsInfoDialog.setArguments(args);
		parkingsInfoDialog.show(getSupportFragmentManager(), "parking_selected");
	}

	@Override
	public void onParkingObjectsTap(List<ParkingSerial> parkingsList) {
		ParkingsInfoDialog parkingsInfoDialog = new ParkingsInfoDialog();
		Bundle args = new Bundle();
		args.putSerializable(ParkingsInfoDialog.ARG_PARKINGS, (Serializable) parkingsList);
		parkingsInfoDialog.setArguments(args);
		parkingsInfoDialog.show(getSupportFragmentManager(), "parking_selected");
	}

	@Override
	public String getAppToken() {
		return JPParamsHelper.getAppToken();
	}

	@Override
	public String getAuthToken() {
		try {
			return JPHelper.getAuthToken(this);
		} catch (AACException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
