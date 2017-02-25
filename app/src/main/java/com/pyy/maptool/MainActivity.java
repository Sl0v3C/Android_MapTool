package com.pyy.maptool;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.CoordinateConverter;
import com.amap.api.location.DPoint;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.GeocodeSearch.OnGeocodeSearchListener;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import android.widget.AdapterView.OnItemSelectedListener;

import static com.amap.api.maps2d.AMapOptions.LOGO_POSITION_BOTTOM_LEFT;

public class MainActivity extends Activity implements LocationSource, AMapLocationListener {
    private final String logTag = "[MapTool]";
    private MapView mapView;
    private AMap aMap;
    private TextView cResult;
    private GeocodeSearch geocoderSearch;
    private String addressName;
    private EditText longitude;
    private EditText latidute;
    private LatLonPoint latLonPoint;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private CoordinateConverter.CoordType cType = CoordinateConverter.CoordType.GPS;
    private Spinner spinner;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        init();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        Button geoButton = (Button) findViewById(R.id.button);
        longitude = (EditText) findViewById(R.id.longitude);
        latidute = (EditText) findViewById(R.id.latitude);
        cResult = (TextView) findViewById(R.id.checkResult);
        spinner = (Spinner) findViewById(R.id.spinner2);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.layers_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (aMap != null) {
                    setLayer((String) parent.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        geocoderSearch = new GeocodeSearch(this);
        geocoderSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                addressName = regeocodeResult.getRegeocodeAddress().getFormatAddress();
                cResult.setText(addressName);
                Toast.makeText(getApplicationContext(), addressName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

            }
        });

        geoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 得到文本输入框的中经纬 度坐标值
                String latStr = latidute.getText().toString();
                String lngStr = longitude.getText().toString();
                // 无输入则默认以下值：上海环球金融中心
                if (latStr == null || latStr.length() <= 0)
                    latStr = "31.23622";
                if (lngStr == null || lngStr.length() <= 0)
                    lngStr = "121.503359";
                // 将得到的字符串转成Double型
                double lat = Double.parseDouble(latStr);
                double lng = Double.parseDouble(lngStr);

                convert(lat, lng);
            }
        });
    }

    private void moveToAddr(double lat, double lng) {
        LatLng latlng = new LatLng(lat, lng);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 19));
    }

    private void geoToAddr(double lat, double lng) {
        latLonPoint = new LatLonPoint(lat, lng);
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
        geocoderSearch.getFromLocationAsyn(query); // 回调到上面的onRegeocodeSearched
    }

    private void convert(double lat, double lng) {
        DPoint point = new DPoint(lat, lng);
        DPoint destPoint = null;
        CoordinateConverter converter = new CoordinateConverter(getApplicationContext());
        //Log.e(logTag, "cType " + cType);
        converter.from(cType);
        //设置需要转换的坐标
        try {
            converter.coord(point);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //转换成高德坐标
        try {
            destPoint = converter.convert();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null != destPoint) {
            lat = destPoint.getLatitude();
            lng = destPoint.getLongitude();
            //Log.d(logTag, "lat " + lat + "  lng " + lng);
            // 地理坐标转换成地址
            geoToAddr(lat, lng);
            // 地图跳转到坐标位置
            moveToAddr(lat, lng);

        } else {
            Toast.makeText(getApplicationContext(), "坐标转换失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void setUpMap() {
        // 自定义系统定位小蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory
                .fromResource(R.drawable.location_marker));// 设置小蓝点的图标
        myLocationStyle.strokeColor(Color.BLACK);// 设置圆形的边框颜色
        myLocationStyle.radiusFillColor(Color.argb(100, 0, 0, 180));// 设置圆形的填充颜色
        // myLocationStyle.anchor(int,int)//设置小蓝点的锚点
        myLocationStyle.strokeWidth(1.0f);// 设置圆形的边框粗细
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.getUiSettings().setScaleControlsEnabled(true); // 设置默认显示比例尺
        //aMap.getUiSettings().setCompassEnabled(true); // 设置默认显示指南针
        aMap.getUiSettings().setLogoPosition(LOGO_POSITION_BOTTOM_LEFT);
        aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            initLocOption();
            AMapLocationListener mapLocationListener = new AMapLocationListener() {

                @Override
                public void onLocationChanged(AMapLocation aMapLocation) {
                    moveToAddr(aMapLocation.getLatitude(), aMapLocation.getLongitude());

                }
            };
            mlocationClient.setLocationListener(mapLocationListener);
            mlocationClient.startLocation();
        }

        aMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {

            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {
                //Toast.makeText(getApplicationContext(), "当前地图中心位置是否在国外: "+cameraPosition.isAbroad, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
            }
        });
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                Log.e(logTag, errText);
            }
        }
    }

    /**
     * 设置坐标来源,这里使用百度坐标作为示例
     * 可选的来源包括：
     * <li>CoordType.BAIDU ： 百度坐标
     * <li>CoordType.MAPBAR ： 图吧坐标
     * <li>CoordType.MAPABC ： 图盟坐标
     * <li>CoordType.SOSOMAP ： 搜搜坐标
     * <li>CoordType.ALIYUN ： 阿里云坐标
     * <li>CoordType.GOOGLE ： 谷歌坐标
     * <li>CoordType.GPS ： GPS坐标
     */
    private void setLayer(String layerName) {
        if (layerName.equals(getString(R.string.baidu))) {
            cType = CoordinateConverter.CoordType.BAIDU;
        } else if (layerName.equals(getString(R.string.mapbar))) {
            cType = CoordinateConverter.CoordType.MAPBAR;
        } else if (layerName.equals(getString(R.string.mapabc))) {
            cType = CoordinateConverter.CoordType.MAPABC;
        } else if (layerName.equals(getString(R.string.sousou))) {
            cType = CoordinateConverter.CoordType.SOSOMAP;
        } else if (layerName.equals(getString(R.string.alimap))) {
            cType = CoordinateConverter.CoordType.ALIYUN;
        } else if (layerName.equals(getString(R.string.google))) {
            cType = CoordinateConverter.CoordType.GOOGLE;
        } else if (layerName.equals(getString(R.string.gps))) {
            cType = CoordinateConverter.CoordType.GPS;
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        deactivate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void initLoc() {
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            initLocOption();
        }
    }

    public void initLocOption() {
        //设置为高精度定位模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位参数
        mlocationClient.setLocationOption(mLocationOption);
        //设置为单次定位
        mLocationOption.setOnceLocation(true);
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
        }
        //设置定位监听
        mlocationClient.setLocationListener(this);
        initLocOption();
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        mlocationClient.startLocation();
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
