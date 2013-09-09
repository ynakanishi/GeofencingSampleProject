package com.example.geofencingsample;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import java.util.ArrayList;
import java.util.List;

public class MyGeofenceService extends Service {
    // 東京スカイツリーの緯度・経度が中心。
    // 自分の所在地の緯度・経度を入れると試験しやすい。
    private static final double FENCE_LATITUDE = 35.710057714926265;
    private static final double FENCE_LONGITUDE = 139.81071829999996;

    // 半径200m
    private static final float FENCE_RADIUS = 200.0f;

    // 設置するジオフェンスのID
    private static final String SKYTREE = "tokyo_skytree";

    // スカイツリーの公式ウェブサイトURL
    private static final String SKYTREE_URL = "http://www.tokyo-skytree.jp/";

    private LocationClient mLocationClient;


    @Override
    public void onCreate() {
        super.onCreate();

        mLocationClient = new LocationClient(this,
                mLocationCallback,
                mOnConnectionFailedListener);
        mLocationClient.connect();
    }

    @Override
    public void onDestroy() {
        mLocationClient.disconnect();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 指定の位置、指定の半径の円をカバーするフェンスを作る。
     * フェンスに入ったらトリガがかかる。
     *
     * @param latitude 緯度
     * @param longitude 経度
     * @param radius 半径
     * @param requestId ジオフェンスのID
     * @param broadcastUri ジオフェンス内に入ったら表示されるURI
     */
    private void addGeofence(double latitude, double longitude, float radius,
                             String requestId, String broadcastUri) {
        Geofence.Builder builder = new Geofence.Builder();
        builder.setRequestId(requestId);
        builder.setCircularRegion(latitude, longitude, radius);
        builder.setExpirationDuration(Geofence.NEVER_EXPIRE); // 無期限
        builder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER);

        // フェンスのListを作成する。
        List<Geofence> geofenceList = new ArrayList<Geofence>();
        geofenceList.add(builder.build());

        // フェンス内に入った時に、指定のURIを表示するインテントを投げるようにする。
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(broadcastUri));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mLocationClient.addGeofences(geofenceList, pendingIntent,
                new LocationClient.OnAddGeofencesResultListener() {
            @Override
            public void onAddGeofencesResult(int i, String[] strings) {
                // 完了およびエラー処理(省略)
            }
        });
    }

    private GooglePlayServicesClient.ConnectionCallbacks mLocationCallback =
            new GooglePlayServicesClient.ConnectionCallbacks() {
                AddGetFenceLocationTask mGetFenceLocationTask;


                @Override
                public void onConnected(Bundle bundle) {
                    // GooglePlay Serviceに接続したら、ジオフェンスを設置する。
                    // ネットワークからジオフェンスの位置情報をなどを取ってくる場合も
                    // 想定して、AsyncTaskで処理するようにしておく。
                    mGetFenceLocationTask = new AddGetFenceLocationTask();
                    mGetFenceLocationTask.execute();
                }

                @Override
                public void onDisconnected() {
                }
            };

    private GooglePlayServicesClient.OnConnectionFailedListener mOnConnectionFailedListener
            = new GooglePlayServicesClient.OnConnectionFailedListener() {

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            // エラー処理(省略)
        }
    };

    // ジオフェンスを設置する。
    // このサンプルでは、固定位置一箇所だけに設置している。
    private class AddGetFenceLocationTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (isCancelled()) {
                return null;
            }

            // このサンプルでは固定位置(東京スカイツリー)にフェンスを置く
            addGeofence(FENCE_LATITUDE, FENCE_LONGITUDE, FENCE_RADIUS, SKYTREE, SKYTREE_URL);

            return null;
        }
    }
}
