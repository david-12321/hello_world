package pku.ss.wyy.myweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.zip.GZIPInputStream;

import pku.ss.wyy.myweather.Bean.PinYin;
import pku.ss.wyy.myweather.Bean.TodayWeather;
import pku.ss.wyy.myweather.util.NetUtil;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final int UPDATE_TODAY_WEATHER = 1;

    TodayWeather todayWeather = null;

    private ImageView mUpdateBtn;
    private ImageView mCitySelect;

    private TextView cityTV;//城市
    private TextView timeTV;//更新时间
    private TextView humidityTV;//湿度
    private TextView weekTV;//星期几
    private TextView pmDataTV;//pm的数值
    private TextView pmQualityTV;//空气质量：：重度 or 轻度 or 良 or 优
    private TextView temperatureTV;//温度
    private TextView climateTV;//气候
    private TextView windTV;//风：微风

    private ImageView weatherImg;//天气 图片
    private ImageView pmImg;//头像 戴口罩 表示pm的大小

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("MyApp","MainActivity->onCreate");

        setContentView(R.layout.weather_info);
        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);
        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);

        initView();
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

    /**
     * 初始化控件
     */
    void initView() {
        cityTV = (TextView) findViewById(R.id.city);//城市
        timeTV = (TextView) findViewById(R.id.time);//更新时间
        humidityTV = (TextView) findViewById(R.id.humidity);//湿度
        weekTV = (TextView) findViewById(R.id.week_today);//星期几
        pmDataTV = (TextView) findViewById(R.id.pm_data);//pm的数值
        pmQualityTV = (TextView) findViewById(R.id.pm2_5_quality);//空气质量：：重度 or 轻度 or 良 or 优
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);//头像
        temperatureTV = (TextView) findViewById(R.id.temperature);//温度
        climateTV = (TextView) findViewById(R.id.climate);//气候
        windTV = (TextView) findViewById(R.id.wind);//风：微风
        weatherImg = (ImageView) findViewById(R.id.weather_img);//天气 图片

        //默认第一次登陆的时候显示
        cityTV.setText("N/A");
        timeTV.setText("N/A");
        humidityTV.setText("N/A");
        weekTV.setText("N/A");
        pmDataTV.setText("N/A");
        pmQualityTV.setText("N/A");
        temperatureTV.setText("N/A");
        climateTV.setText("N/A");
        windTV.setText("N/A");
    }

    void updateTodayWeather(TodayWeather todayWeather) {
        //测试下 更新前的要更新的内容
        Log.d("updateTodayWeather", todayWeather.toString());
        cityTV.setText(todayWeather.getCity());
        timeTV.setText("今天" + todayWeather.getUpdatetime() + "发布");
        humidityTV.setText("湿度：" + todayWeather.getShidu());
        pmDataTV.setText(todayWeather.getPm25());
        pmQualityTV.setText(todayWeather.getQuality());
        weekTV.setText(todayWeather.getDate());//日期 + 星期几
        temperatureTV.setText(todayWeather.getHigh() + "～" + todayWeather.getLow());
        climateTV.setText(todayWeather.getType());
        windTV.setText("风力: " + todayWeather.getFengli());

        //Toast.makeText(MainActivity.this, "更新成功！", Toast.LENGTH_LONG);

        //***************利用反射机制和汉语转拼音库实现图片的更新：******************
        int pmValue = Integer.parseInt(todayWeather.getPm25().trim());
        String pmImgStr = "0_50";
        if (pmValue > 50 && pmValue < 201) {
            int startV = (pmValue - 1) / 50 * 50 + 1;
            int endV = ((pmValue - 1) / 50 + 1) * 50;
            pmImgStr = Integer.toString(startV) + "_" + endV;
        } else if (pmValue >= 201 && pmValue < 301) {
            pmImgStr = "201_300";
        } else if (pmValue >= 301) {
            pmImgStr = "greater_300";
        }
        //Log.d("pmValue",pmImgStr);
        String typeImg = "biz_plugin_weather_" + PinYin.converterToSpell(todayWeather.getType());
        pmImgStr = "biz_plugin_weather_" + pmImgStr;
        //Log.d("typeImg:",typeImg);
        Class aClass = R.drawable.class;
        int typeId = -1;
        int pmImgId = -1;
        try {
            //一般尽量采用这种形式
            Field pmField = aClass.getField(pmImgStr);
            Object pmImgO = pmField.get(new Integer(0));
            pmImgId = (int) pmImgO;

            Field field = aClass.getField(typeImg);
            Object value = field.get(new Integer(0));
            typeId = (int) value;

        } catch (Exception e) {
            //e.printStackTrace();
            if (-1 == typeId)
                typeId = R.drawable.biz_plugin_weather_qing;
            if (-1 == pmImgId)
                pmImgId = R.drawable.biz_plugin_weather_0_50;
        } finally {
//            Drawable drawable = getResources().getDrawable(typeId);
//            weatherImg.setImageDrawable(drawable);
//            drawable = getResources().getDrawable(pmImgId);
//            pmImg.setImageDrawable(drawable);
            weatherImg.setImageResource(typeId);
            pmImg.setImageResource(pmImgId);
            Toast.makeText(MainActivity.this, "更新成功", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.title_update_btn) {
            SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
            String cityCode = sharedPreferences.getString("main_city_code", "101010100");
            Log.d("myWeather", cityCode);
            if (NetUtil.getNetworkState(this) != NetUtil.NETWORK_NONE) {
                Log.d("myWeather", "网络OK");
                queryWeatherCode(cityCode);
            } else {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了", Toast.LENGTH_LONG).show();
            }
        } else if (view.getId() == R.id.title_city_manager) {
            Intent intent = new Intent(MainActivity.this, SelectCity.class);
            startActivity(intent);
        }
    }

    /**
     * 根据城市编号查询所对应的天气信息
     *
     * @param cityCode
     */
    private void queryWeatherCode(String cityCode) {
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d("myWeather", address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpGet httpget = new HttpGet(address);
                    HttpResponse httpResponse = httpclient.execute(httpget);
                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
                        HttpEntity entity = httpResponse.getEntity();

                        InputStream responseStream = entity.getContent();
                        responseStream = new GZIPInputStream(responseStream);

                        BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                        StringBuilder response = new StringBuilder();
                        String str;
                        while ((str = reader.readLine()) != null) {
                            response.append(str);
                        }
                        String responseStr = response.toString();
                        Log.d("myWeather", responseStr);
                        todayWeather = parseXML(responseStr);
                        if (todayWeather != null) {
                            Log.d("myWeather", todayWeather.toString());
                            //发送消息，由主线程更新UI
                            Message msg = new Message();
                            msg.what = UPDATE_TODAY_WEATHER;
                            msg.obj = todayWeather;
                            mHandler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //解析函数
    private TodayWeather parseXML(String xmldata) {
        TodayWeather todayWeather = null;
        try {
            //因为有的节点在xml数据中出现了不止一次。只解析第一次出现的时候就行。
            int fengxiangCount = 0;
            int fengliCount = 0;
            int dateCount = 0;
            int highCount = 0;
            int lowCount = 0;
            int typeCount = 0;

            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d("myWeather", "parseXML");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    //判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        //解析到根节点的时候自动创建对象
                        if (xmlPullParser.getName().equals("resp")) {
                            todayWeather = new TodayWeather();
                        }
                        //当解析的是内部节点的时候，此时可以通过判断todayWeather对象是否已经创建来区别
                        if (todayWeather != null) {

                            if (xmlPullParser.getName().equals("city")) {
                                eventType = xmlPullParser.next();
                                Log.d("todayWeather", "city:" + xmlPullParser.getText());
                                todayWeather.setCity(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                Log.d("todayWeather", "upddatetime:" + xmlPullParser.getText());
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                Log.d("todayWeather", "shidu:" + xmlPullParser.getText());
                                todayWeather.setShidu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                Log.d("todayWeather", "wendu:" + xmlPullParser.getText());
                                todayWeather.setWendu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                Log.d("todayWeather", "pm25:" + xmlPullParser.getText());
                                todayWeather.setPm25(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                Log.d("todayWeather", "quality:" + xmlPullParser.getText());
                                todayWeather.setQuality(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                Log.d("todayWeather", "fengxiang:" + xmlPullParser.getText());
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                                eventType = xmlPullParser.next();
                                Log.d("todayWeather", "fengli:" + xmlPullParser.getText());
                                todayWeather.setFengli(xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                //**日星期*  把前面的**日 截取下来 ， 为了输出美观 ，在中间加个空格。
                                String date = xmlPullParser.getText();
                                String day = date.substring(0, 3);
                                String week = date.substring(3);
                                date = day + " " + week;
                                todayWeather.setDate(date);
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                //高温：**  把前面的高温截取掉
                                String high = xmlPullParser.getText();
                                high = high.substring(2);
                                //输出验证下
                                Log.d("todayWeather", "high:" + high);
                                todayWeather.setHigh(high);
                                highCount++;
                            } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                //低温：**  把前面的低温截取掉
                                String low = xmlPullParser.getText();
                                low = low.substring(2);
                                //输出验证下
                                Log.d("todayWeather", "low:" + low);
                                todayWeather.setLow(low);
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                Log.d("todayWeather", "type:" + xmlPullParser.getText());
                                todayWeather.setType(xmlPullParser.getText());
                                typeCount++;
                            }
                        }
                        break;
                    //判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                //进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
            return todayWeather;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
