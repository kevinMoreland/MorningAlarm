package kmm.developer.morningalarm;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class AlarmReceiver extends WakefulBroadcastReceiver {
    public static Ringtone ringtone;
    String topArticle, secondArticle, thirdArticle, weatherCondition, unitSpeed, unitTemp;
    int windSpeed, airTemp;

    @Override
    public void onReceive(final Context context, Intent intent) {
        //this will update the UI with message
        //retrieve feed
        new RetrieveFeedTask().execute();

        //this will sound the alarm tone
        //this will sound the alarm once, if you wish to
        //raise alarm in loop continuously then use MediaPlayer and setLooping(true)
        MainActivity instance = MainActivity.instance();
        ArrayList<String> sounds = instance.getNotificationSounds();

        //Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        //get a random ringtone uri
        Uri alarmUri;
        Random rand = new Random();
        int random = rand.nextInt(sounds.size());
        alarmUri = Uri.parse(sounds.get(random));

        //if can't find this uri,
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(context, alarmUri);
        ringtone.play();

        //this will send a notification message
        ComponentName comp = new ComponentName(context.getPackageName(),
                AlarmService.class.getName());
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }

    class RetrieveFeedTask extends AsyncTask<Void, Void, ArrayList> {

        protected void onPreExecute() {
            //progressBar.setVisibility(View.VISIBLE);
            //responseView.setText("");
        }

        protected ArrayList doInBackground(Void... urls) {
            //String email = emailText.getText().toString();
            // Do some validation here
            ArrayList<String> outputArray = new ArrayList<>();
            try {

                String API_URL = "https://newsapi.org/v1/articles?source=" + MainActivity.newsSource + "&sortBy=top";
                String API_KEY = "bb67b75c0918495bbc46e5b65c38271e";
                String unit = MainActivity.units;
                Log.i("Units actually used: ", unit);

                String location = "(" + MainActivity.latitude + "," + MainActivity.longitude + ")";

                String YQL = String.format("select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\"%s\") and u='" + unit + "'", location);
                String YahooEndpoint = String.format("https://query.yahooapis.com/v1/public/yql?q=%s&format=json", Uri.encode(YQL));
                String WeatherBackUp = "http://api.apixu.com/v1/current.json?key=9a46bba093da424ab28210855160412&q=" + "Paris";

                URL YAHOO_WEATHER_URL = new URL(YahooEndpoint);
                URL url = new URL(API_URL + "&apiKey=" + API_KEY);
                URL backupWeather = new URL(WeatherBackUp);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                HttpURLConnection urlConnection2 = (HttpURLConnection) YAHOO_WEATHER_URL.openConnection();
                HttpURLConnection urlConnection3 = (HttpURLConnection) backupWeather.openConnection();

                String return1 = null;
                String return2 = null;
                String return3 = null;
                if (!MainActivity.newsSource.equals("")) {
                    //using news

                try {
                    //news----------------------
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder1 = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder1.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return1 = stringBuilder1.toString();
                } finally {
                    urlConnection.disconnect();
                }
                }
                try{
                    //weather-------------------
                    BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(urlConnection2.getInputStream()));
                    StringBuilder stringBuilder2 = new StringBuilder();
                    String line2;
                    while ((line2 = bufferedReader2.readLine()) != null) {
                        stringBuilder2.append(line2).append("\n");
                    }
                    bufferedReader2.close();
                    return2 = stringBuilder2.toString();

                    if(return2.contains("\"results:null\"")){
                        Log.e("ERROR", "Results will be null");
                    }

                    //Pair<String, String> returnVal = new Pair<String, String>(stringBuilder1.toString(), stringBuilder2.toString());
                }
                finally{
                    urlConnection2.disconnect();
                }
                try{
                    //use backup weather
                    BufferedReader bufferedReader3 = new BufferedReader(new InputStreamReader(urlConnection3.getInputStream()));

                    //weather-------------------
                    StringBuilder stringBuilder2 = new StringBuilder();
                    String line2;
                    while ((line2 = bufferedReader3.readLine()) != null) {
                        stringBuilder2.append(line2).append("\n");
                    }
                    bufferedReader3.close();
                    return3 = stringBuilder2.toString();

                }
                finally{
                    urlConnection3.disconnect();
                }
                outputArray.add(return1);
                outputArray.add(return2);
                outputArray.add(return3);
                return outputArray;
                //return new Pair<>(return1, return2);

            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(ArrayList input) {
            String response = null;
            String response2 = null;
            if(input!=null){
                if(input.get(0)!=null){
                    response = input.get(0).toString();
                }
                if(input.get(1)!=null){
                    response2 = input.get(1).toString();
                }

            }

            //if response == null, news is not being used

            //progressBar.setVisibility(View.GONE);
            Log.i("INFO response", response + " ");
            Log.i("INFO response2", response2 + " ");


            //responseView.setText(response);
            try {
                if(response!=null){
                    try {
                        //if news is being found,
                        JSONObject json = new JSONObject(response);
                        JSONObject j;

                        JSONArray jsonArray = json.getJSONArray("articles");
                        j = jsonArray.getJSONObject(0);
                        topArticle = j.getString("title");

                        j = jsonArray.getJSONObject(1);
                        secondArticle = j.getString("title");

                        j = jsonArray.getJSONObject(2);
                        thirdArticle = j.getString("title");

                    }
                    catch(Exception e){
                        Log.e("ERROR", e.getMessage(), e);
                    }
                }
                else{
                    topArticle=null;
                    secondArticle =null;
                    thirdArticle = null;
                }

                    //if found weather data,
                    try {
                        JSONObject json2 = new JSONObject(response2);
                        JSONObject channel = json2.getJSONObject("query").getJSONObject("results").getJSONObject("channel");

                        windSpeed = channel.getJSONObject("wind").getInt("speed");
                        airTemp = channel.getJSONObject("item").getJSONObject("condition").getInt("temp");
                        weatherCondition = channel.getJSONObject("item").getJSONObject("condition").getString("text");
                        unitSpeed = channel.getJSONObject("units").getString("speed");
                        unitTemp = channel.getJSONObject("units").getString("temperature");
                        //get lows and highs, sunset and sunrise
                    }
                    catch(JSONException e) {
                        Log.e("ERROR", e.getMessage(), e);
                    }

                MainActivity instance = MainActivity.instance();
                //if no news data, report only weather
                if(topArticle ==null || secondArticle==null || thirdArticle==null){
                    instance.setAlarmText(windSpeed, airTemp, weatherCondition, unitSpeed, unitTemp);

                }
                else{
                    instance.setAlarmText(topArticle, secondArticle, thirdArticle, windSpeed, airTemp, weatherCondition, unitSpeed, unitTemp);
                }


            } catch(Exception e){
                Log.e("ERROR", e.getMessage(), e);
                //couldn't get information
                MainActivity instance = MainActivity.instance();
                instance.setAlarmText();
            }

        }
    }

}