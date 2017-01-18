package net.eastpole;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import java.text.DateFormat;
import java.util.Date;
import java.util.EnumMap;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GPSUpdatesFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GPSUpdatesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GPSUpdatesFragment extends Fragment {
//    // TODO: Rename parameter arguments, choose names that match
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;

    private MainActivity mMainActivity;

    protected enum WigetK {LATITUDE, LONGITUDE, ACCURACY, PROVIDER, ALTITUDE, SPEED, BEARING, TIME, DELTA0, DELTA1, DELTA2, DELTA3, DELTA4}

    ;

    //
    private interface GetLocationProp {
        public String get(MainActivity me);
    }   // Until we can use real lambdas!

    private static class GetDeltaTimeProp implements GetLocationProp {
        GetDeltaTimeProp(int mIdx) {
            this.mIdx = mIdx;
        }

        public String get(MainActivity me) {
            return me.mUpdateTimeDeltas[mIdx] >= 0 ? String.format("%.3f s", me.mUpdateTimeDeltas[mIdx] / 1000.0) : "";
        }

        private int mIdx;
    }

    private static class ResIdSpec {
        ResIdSpec(WigetK mWigetK, int mLabelId, int mViewId, GetLocationProp mGetLocationProp) {
            this.mWigetK = mWigetK;
            this.mLabelId = mLabelId;
            this.mViewId = mViewId;
            this.mGetLocationProp = mGetLocationProp;
        }

        public final WigetK mWigetK;
        public final int mViewId;
        public final int mLabelId;
        public final GetLocationProp mGetLocationProp;
    }

    private static final ResIdSpec mResIdSpecs[] = {
            new ResIdSpec(WigetK.LATITUDE, R.string.latitude_label, R.id.latitude_text
                    , new GetLocationProp() {
                public String get(MainActivity me) {
                    return String.valueOf(me.mCurrentLocation.getLatitude());
                }
            }),
            new ResIdSpec(WigetK.LONGITUDE, R.string.longitude_label, R.id.longitude_text
                    , new GetLocationProp() {
                public String get(MainActivity me) {
                    return String.valueOf(me.mCurrentLocation.getLongitude());
                }
            }),
            new ResIdSpec(WigetK.ACCURACY, R.string.accuracy_label, R.id.accuracy_text
                    , new GetLocationProp() {
                public String get(MainActivity me) {
                    return me.mCurrentLocation.hasAccuracy() ? String.valueOf(me.mCurrentLocation.getAccuracy()) : "";
                }
            }),
            new ResIdSpec(WigetK.PROVIDER, R.string.provider_label, R.id.provider_text
                    , new GetLocationProp() {
                public String get(MainActivity me) {
                    return me.mCurrentLocation.getProvider();
                }
            }),
            new ResIdSpec(WigetK.ALTITUDE, R.string.altitude_label, R.id.altitude_text
                    , new GetLocationProp() {
                public String get(MainActivity me) {
                    return me.mCurrentLocation.hasAltitude() ? String.valueOf(me.mCurrentLocation.getAltitude()) : "";
                }
            }),
            new ResIdSpec(WigetK.SPEED, R.string.speed_label, R.id.speed_text
                    , new GetLocationProp() {
                public String get(MainActivity me) {
                    return me.mCurrentLocation.hasSpeed() ? String.valueOf(me.mCurrentLocation.getSpeed()) : "";
                }
            }),
            new ResIdSpec(WigetK.BEARING, R.string.bearing_label, R.id.bearing_text
                    , new GetLocationProp() {
                public String get(MainActivity me) {
                    return me.mCurrentLocation.hasBearing() ? String.valueOf(me.mCurrentLocation.getBearing()) : "";
                }
            }),

            new ResIdSpec(WigetK.TIME, R.string.last_update_time_label, R.id.last_update_time_text
                    , new GetLocationProp() {
                public String get(MainActivity me) {
                    return String.valueOf(DateFormat.getTimeInstance().format(new Date(me.mLastUpdateTime)));
                }
            }),
            new ResIdSpec(WigetK.DELTA0, R.string.prev_update_time_label, R.id.last_delta_time0_text, new GetDeltaTimeProp(0)),
            new ResIdSpec(WigetK.DELTA1, R.string.prev_update_time_label, R.id.last_delta_time1_text, new GetDeltaTimeProp(1)),
            new ResIdSpec(WigetK.DELTA2, R.string.prev_update_time_label, R.id.last_delta_time2_text, new GetDeltaTimeProp(2)),
            new ResIdSpec(WigetK.DELTA3, R.string.prev_update_time_label, R.id.last_delta_time3_text, new GetDeltaTimeProp(3)),
            new ResIdSpec(WigetK.DELTA4, R.string.prev_update_time_label, R.id.last_delta_time4_text, new GetDeltaTimeProp(4)),
    };

    protected static class WidgetD {
        WidgetD(String mLabelText, TextView mTextView, GetLocationProp mGetLocationProp) {
            this.mLabelText = mLabelText;
            this.mTextView = mTextView;
            this.mGetLocationProp = mGetLocationProp;
        }

        public final String mLabelText;
        public TextView mTextView;
        public final GetLocationProp mGetLocationProp;
    }

    //
    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;
    protected EnumMap<WigetK, WidgetD> mWidgetMap;


    public GPSUpdatesFragment() {
        // Required empty public constructor
    }

    // Factory method - do we need this?!
    public static GPSUpdatesFragment newInstance() {
        GPSUpdatesFragment fragment = new GPSUpdatesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWidgetMap = new EnumMap<WigetK, WidgetD>(WigetK.class);
        for (ResIdSpec resIdSpec : mResIdSpecs) {
            mWidgetMap.put(resIdSpec.mWigetK, new WidgetD(
                    getResources().getString(resIdSpec.mLabelId),
                    null,
                    resIdSpec.mGetLocationProp));
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gpsupdates, container, false);

        mStartUpdatesButton = (Button) view.findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) view.findViewById(R.id.stop_updates_button);

        mStartUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View unused) {
                startUpdatesButtonHandler();
            }
        });
        mStopUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View unused) {
                stopUpdatesButtonHandler();
            }
        });

        for (ResIdSpec resIdSpec : mResIdSpecs) {
            WidgetD widgetD = mWidgetMap.get(resIdSpec.mWigetK);
            widgetD.mTextView = (TextView) view.findViewById(resIdSpec.mViewId);
        }

        TextView tv = (TextView) view.findViewById(R.id.latitude_text);
        tv.setText(R.string.latitude_label);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setButtonsEnabledState();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mMainActivity = (MainActivity) context;
        } else {
            throw new RuntimeException(context.toString() + " must be a MainActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMainActivity = null;
    }

    public void startUpdatesButtonHandler() {
        if (mMainActivity.mRequestingLocationUpdates)
            return;
        mMainActivity.mRequestingLocationUpdates = true;
        setButtonsEnabledState();
        mMainActivity.startLocationUpdates();
    }

    public void stopUpdatesButtonHandler() {
        if (!mMainActivity.mRequestingLocationUpdates)
            return;
        mMainActivity.mRequestingLocationUpdates = false;
        setButtonsEnabledState();
        mMainActivity.stopLocationUpdates();
    }

    private void setButtonsEnabledState() {
        if (mMainActivity.mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    protected void updateUI() {
        for (EnumMap.Entry<WigetK, WidgetD> entry : mWidgetMap.entrySet()) {
            WidgetD wd = entry.getValue();
            if (wd.mGetLocationProp == null)
                continue;

            wd.mTextView.setText(String.format("%s: %s", wd.mLabelText, wd.mGetLocationProp.get(mMainActivity)));
        }
    }
}
