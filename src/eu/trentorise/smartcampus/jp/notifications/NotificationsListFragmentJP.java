package eu.trentorise.smartcampus.jp.notifications;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;

import eu.trentorise.smartcampus.android.common.SCAsyncTask;
import eu.trentorise.smartcampus.android.feedback.utils.FeedbackFragmentInflater;
import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.communicator.model.NotificationFilter;
import eu.trentorise.smartcampus.communicator.model.NotificationsConstants.ORDERING;
import eu.trentorise.smartcampus.jp.Config;
import eu.trentorise.smartcampus.jp.MyItineraryFragment;
import eu.trentorise.smartcampus.jp.MyRecurItineraryFragment;
import eu.trentorise.smartcampus.jp.R;
import eu.trentorise.smartcampus.jp.custom.AbstractAsyncTaskProcessor;
import eu.trentorise.smartcampus.jp.helper.JPHelper;
import eu.trentorise.smartcampus.jp.helper.JPParamsHelper;
import eu.trentorise.smartcampus.mobilityservice.model.BasicItinerary;
import eu.trentorise.smartcampus.mobilityservice.model.BasicRecurrentJourney;
import eu.trentorise.smartcampus.notifications.NotificationsHelper;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.ConnectionException;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.SecurityException;

public class NotificationsListFragmentJP extends SherlockListFragment {

	private NotificationsListAdapterJP adapter;
	private final static String CORE_MOBILITY = "core.mobility";
	private static final int MAX_MSG = 50;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle bundle) {
		return inflater.inflate(R.layout.notifications_list_jp, container);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		FeedbackFragmentInflater.inflateHandleButton(getSherlockActivity(),
				getView());

		adapter = new NotificationsListAdapterJP(getActivity(),
				R.layout.notifications_row_jp);
		setListAdapter(adapter);
		adapter.clear();

		// instantiate again JPHelper if needed
		if (!JPHelper.isInitialized()) {
			JPHelper.init(getSherlockActivity());
		}

		if (!NotificationsHelper.isInstantiated()) {
			try {
				NotificationsHelper.init(getSherlockActivity(), JPParamsHelper.getAppToken(), null, CORE_MOBILITY, MAX_MSG);
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				Toast.makeText(getActivity().getApplicationContext(),
						getString(R.string.app_failure_operation),
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
			}
		}

		new SCAsyncTask<Void, Void, List<Notification>>(getSherlockActivity(), new NotificationsLoader(getSherlockActivity())).execute();

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (adapter.getItem(position).getEntities() != null
				&& !adapter.getItem(position).getEntities().isEmpty()) {
			String objectId = adapter.getItem(position).getEntities().get(0)
					.getId();
			SCAsyncTask<String, Void, Object> viewDetailsTask = new SCAsyncTask<String, Void, Object>(
					getSherlockActivity(),
					new NotificationsAsyncTaskProcessorJP(getSherlockActivity()));
			viewDetailsTask.execute(objectId);
		}
	}

	@Override
	public void onDestroy() {
		try {
			NotificationsHelper.markAllAsRead(getNotificationFilter());
		} catch (Exception e) {
			Log.e(this.getClass().getName(), e.getMessage());
		}

		super.onDestroy();
	}

	public static NotificationFilter getNotificationFilter() {
		NotificationFilter filter = new NotificationFilter();
		filter.setOrdering(ORDERING.ORDER_BY_ARRIVAL);
		return filter;
	}

	/*
	 * AsyncTask
	 */
	private class NotificationsAsyncTaskProcessorJP extends
			AbstractAsyncTaskProcessor<String, Object> {

		private Context ctx;

		public NotificationsAsyncTaskProcessorJP(Activity activity) {
			super(activity);
			ctx = activity.getApplicationContext();
		}

		@Override
		public Object performAction(String... params) throws SecurityException,
				ConnectionException, Exception {
			String id = params[0];

			return JPHelper.getItineraryObject(id, JPHelper.getAuthToken(ctx));
		}

		@Override
		public void handleResult(Object result) {
			if (result == null)
				return;

			Fragment fragment = null;
			FragmentTransaction fragmentTransaction = getSherlockActivity()
					.getSupportFragmentManager().beginTransaction();
			fragmentTransaction
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

			if (result instanceof BasicItinerary) {
				fragment = MyItineraryFragment
						.newInstance((BasicItinerary) result);
				fragmentTransaction.replace(Config.mainlayout, fragment,
						Config.MY_JOURNEYS_FRAGMENT_TAG);
			} else if (result instanceof BasicRecurrentJourney) {
				fragment = new MyRecurItineraryFragment();
				Bundle b = new Bundle();
				b.putSerializable(MyRecurItineraryFragment.PARAMS,
						(BasicRecurrentJourney) result);
				b.putBoolean(MyRecurItineraryFragment.PARAM_EDITING, false);
				fragment.setArguments(b);
				fragmentTransaction.replace(Config.mainlayout, fragment,
						Config.MY_RECUR_JOURNEYS_FRAGMENT_TAG);
			}

			if (fragment != null) {
				fragmentTransaction.addToBackStack(fragment.getTag());
				fragmentTransaction.commit();
			}
		}

	}

	private class NotificationsLoader extends
			AbstractAsyncTaskProcessor<Void, List<Notification>> {

		public NotificationsLoader(Activity activity) {
			super(activity);
		}

		@Override
		public List<Notification> performAction(Void... params)
				throws SecurityException, ConnectionException, Exception {
			return NotificationsHelper.getNotifications(
					getNotificationFilter(), 0, -1, 0);
		}

		@Override
		public void handleResult(List<Notification> notificationsList) {
			TextView listEmptyTextView = (TextView) getView().findViewById(
					R.id.jp_list_text_empty);

			if (!notificationsList.isEmpty()) {
				adapter.clear();
				for (Notification n : notificationsList) {
					adapter.add(n);
				}
				listEmptyTextView.setVisibility(View.GONE);
				adapter.notifyDataSetChanged();
			} else if (adapter.isEmpty()) {
				listEmptyTextView.setVisibility(View.VISIBLE);
			}
		}
	}

}
