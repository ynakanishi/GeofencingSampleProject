package com.example.geofencingsample;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity
        implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationClient.OnAddGeofencesResultListener,
        LocationClient.OnRemoveGeofencesResultListener {
    private static final String TAG = MainActivity.class.getSimpleName();

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

    // フェンスの追加・削除リクエストを示す定数
    private final static int ADD_FENCE = 0;
    private final static int REMOVE_FENCE = 1;

    // Google Play Services へのリクエストコード
    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    // LocationClientのインスタンス
    private LocationClient mLocationClient;
    // LocationClientへの処理要求中かどうかを管理するフラグ
    private boolean mInProgress;
    // LocationClientに要求する処理の種類 (ADD_FENCE または REMOVE_FENCE)
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
                if (!mInProgress) {
                    requestConnectLocationClient();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRequestType = REMOVE_FENCE;
                if (!mInProgress) {
                    requestConnectLocationClient();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONNECTION_FAILURE_RESOLUTION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                // Google Play Services が問題を解決した場合、再接続する
                if (mRequestType == ADD_FENCE || mRequestType == REMOVE_FENCE) {
                    mInProgress = false;
                    requestConnectLocationClient();
                }
            } else {
                // Google Play Servicesで問題が解決されなかった場合、ログ出力
                Log.d(TAG, "google play no resolution error");
            }
        } else {
            Log.d(TAG, "unknown request code: " + requestCode);
        }
    }

    // LocationClient に接続する
    private void requestConnectLocationClient() {
        getLocationClient().connect();
    }

    // LocationClientのインスタンスを取得する
    private LocationClient getLocationClient() {
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(this, this, this);
        }

        return mLocationClient;
    }

    // Google Play Services への接続チェック
    private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else {
            // Google Playに正常に接続できない時のエラー処理。
            // (Googleが以下のページで紹介している例には少しバグがあるので要修正)
            // ex. http://developer.android.com/training/location/geofencing.html
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST
            );

            if (errorDialog != null) {
                ErrorDialogFragment errorDialogFragment =
                        new ErrorDialogFragment();
                errorDialogFragment.setDialog(errorDialog);
                errorDialogFragment.show(getSupportFragmentManager(),
                        "Geofence Sample");
            }

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

    // LocationClientに接続したら呼ばれる
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

    // LocationClientから切断したら呼ばれる
    @Override
    public void onDisconnected() {
        mInProgress = false;
        mLocationClient = null;
    }

    // Google Play services への接続に失敗した場合に呼ばれる
    // hasResolution()がtrueの場合は、Google Play services に解決を要求する。
    // そうでない場合は、解決方法が無いのでエラー表示する(本サンプルではログ出力)
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;

        /*
         * Google Play services が問題解決できる場合、Google Play servicesを
         * 呼び出して、解決を要求する。
         */
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            // connectionResult.getErrorCode() の値によるエラー通知する
            Log.d(TAG, "connection failed error code: " + connectionResult.getErrorCode());
        }
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


    private static class ErrorDialogFragment extends DialogFragment {
        private Dialog mDialog;

        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}
