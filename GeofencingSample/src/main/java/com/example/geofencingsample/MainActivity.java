package com.example.geofencingsample;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import static com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import static com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;

public class MainActivity extends Activity
        implements ConnectionCallbacks,
        OnConnectionFailedListener,
        OnAddGeofencesResultListener,
        LocationClient.OnRemoveGeofencesResultListener {
    // 東京スカイツリーの緯度・経度が中心。
    // 自分の所在地の緯度・経度を入れると試験しやすい。
    private static final double FENCE_LATITUDE = 35.710057714926265;
    private static final double FENCE_LONGITUDE = 139.81071829999996;

    // 半径200m
    private final static float FENCE_RADIUS = 200.0f;

    // 設置するジオフェンスのID
    private final static String SKYTREE = "tokyo_skytree";

    // スカイツリーの公式ウェブサイトURL
    private final static String SKYTREE_URL = "http://www.tokyo-skytree.jp/";

    private final static int ADD_FENCE = 0;
    private final static int REMOVE_FENCE = 1;

    private MainActivity self = this;
    private LocationClient mLocationClient;
    private boolean mInProgress;
    private int mRequestType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInProgress = false;

        Button startButton = (Button) findViewById(R.id.start_service_btn);
        Button stopButton = (Button) findViewById(R.id.stop_service_btn);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRequestType = ADD_FENCE;
                mLocationClient = new LocationClient(self, self, self);
                if (!mInProgress) {
                    mInProgress = true;
                    mLocationClient.connect();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRequestType = REMOVE_FENCE;
                mLocationClient = new LocationClient(self, self, self);
                if (!mInProgress) {
                    mInProgress = true;
                    mLocationClient.connect();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Google Play Services への接続チェック
    private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else {
            // Google Playに接続できない時のエラー処理(省略)
            return false;
        }
    }

    // ジオフェンスを登録する
    private void addFence(double latitude, double longitude, float radius,
                          String requestId, String broadcastUri) {
        if (!servicesConnected()) {
            return;
        }

        Geofence.Builder builder = new Geofence.Builder();
        builder.setRequestId(requestId);
        builder.setCircularRegion(latitude, longitude, radius);
        builder.setExpirationDuration(Geofence.NEVER_EXPIRE); // 無期限
        builder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER);

        // フェンスのListを作成する。
        List<Geofence> fenceList = new ArrayList<Geofence>();
        fenceList.add(builder.build());

        // フェンス内に入った時に、指定のURIを表示するインテントを投げるようにする。
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(broadcastUri));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mLocationClient.addGeofences(fenceList, pendingIntent, this);
    }

    // 指定されたIDのジオフェンスを削除
    private void removeFence(String requestId) {
        if (!servicesConnected()) {
            return;
        }

        List<String> fenceIdList = new ArrayList<String>();
        fenceIdList.add(requestId);
        mLocationClient.removeGeofences(fenceIdList, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        switch(mRequestType) {
        case ADD_FENCE:
            addFence(FENCE_LATITUDE,
                    FENCE_LONGITUDE,
                    FENCE_RADIUS,
                    SKYTREE,
                    SKYTREE_URL);
            break;
        case REMOVE_FENCE:
            removeFence(SKYTREE);
            break;
        default:
            break;
        }
    }

    @Override
    public void onDisconnected() {
        mInProgress = false;
        mLocationClient = null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;

        // Googleアカウント接続エラーなどの対処(省略)
    }

    @Override
    public void onAddGeofencesResult(int i, String[] strings) {
        mInProgress = false;
        mLocationClient.disconnect();
    }

    @Override
    public void onRemoveGeofencesByRequestIdsResult(int i,
                                                    String[] strings) {
        mInProgress = false;
        mLocationClient.disconnect();
    }

    @Override
    public void onRemoveGeofencesByPendingIntentResult(
            int i,
            PendingIntent pendingIntent) {
        mInProgress = false;
        mLocationClient.disconnect();
    }
}
