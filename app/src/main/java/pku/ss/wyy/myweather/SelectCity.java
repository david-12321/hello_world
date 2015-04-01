package pku.ss.wyy.myweather;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

import pku.ss.wyy.myweather.Bean.City;
import pku.ss.wyy.myweather.db.CityDB;

/**
 * Created by wyy on 15-3-29.
 */
public class SelectCity extends Activity implements View.OnClickListener{

    private static final int UPDATA_CITY_LIST = 1;

    private ImageView mBackBtn;
    private List<City> mCityList;
    private CityDB mCityDB;
    private ListView mListView;


    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.select_city);

        mListView = (ListView) findViewById(R.id.city_list);

        mBackBtn = (ImageView) findViewById(R.id.title_back);
        mBackBtn.setOnClickListener(this);

        mCityDB = openCityDB();
        initCityList();
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg){
            switch (msg.what){
                case UPDATA_CITY_LIST:
                    String[] data = new String[mCityList.size()];
                    List<City> list = (List<City>) msg.obj;
                    for(int i=0; i<list.size(); i++ ){
                        data[i] = list.get(i).getCity().toString();
                    }
                    Log.d("testCity:",data[0]);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(SelectCity.this, android.R.layout.simple_list_item_1, data);
                    mListView.setAdapter(adapter);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onClick(View view){
        switch (view.getId()) {
            case R.id.title_back:
                finish();
                break;
            default:
                break;
        }
    }

    public CityDB openCityDB(){
        String path = "/data"
                + Environment.getDataDirectory().getAbsolutePath()
                + File.separator + getPackageName()
                + File.separator + "databases"
                + File.separator
                + CityDB.CITY_DB_NAME;
        File db = new File(path);
        Log.d("DBpath",path);
        if(!db.exists()){
            Log.i("MyApp","db is not exists");
            try{
                InputStream is = getAssets().open("city.db");
                FileOutputStream fos = new FileOutputStream(db);
                int len = -1;
                byte[] buffer = new byte[1024];
                while((len = is.read(buffer))!= -1) {
                    fos.write(buffer, 0 ,len);
                    fos.flush();
                }
                fos.close();
                is.close();
            } catch (IOException e){
                e.printStackTrace();
                System.exit(0);
            }
        }
        return new CityDB(this, path);
    }

    private void initCityList() {
        mCityList = new ArrayList<City>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                prepareCityList();
            }
        }).start();
    }

    private boolean prepareCityList() {
        mCityList = mCityDB.getAllCity();
        for (City city : mCityList) {
            String cityName = city.getCity();
            Log.d("city:", cityName);
        }
        Message msg = new Message();
        msg.what = UPDATA_CITY_LIST;
        msg.obj = mCityList;
        mHandler.sendMessage(msg);
        return true;
    }

}
