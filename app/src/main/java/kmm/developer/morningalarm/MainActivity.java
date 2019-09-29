package kmm.developer.morningalarm;


import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TimePicker;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;

public class MainActivity extends Activity {
    TextToSpeech textToSpeech;
    AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    TimePicker alarmTimePicker;
    Switch onOffSwitch;
    public static String city;
    public static double latitude, longitude;
    public static String newsSource;
    public String name, voice, notification;
    public static String units;
    private static MainActivity inst;
    Context context;

    public String[] accents = new String[]{"CANADA", "CHINA", "FRANCE", "GERMANY", "ITALY",
            "JAPAN", "KOREA", "TAIWAN", "UK", "US"};
    public String[] news = new String[]{"", "bbc-news", "bbc-sport", "bloomberg", "business-insider", "business-insider-uk",
            "buzzfeed", "cnbc", "cnn", "espn", "fortune", "fox-sports", "google-news", "independent", "national-geographic",
            "new-scientist", "nfl-news", "polygon", "sky-news", "the-huffington-post", "the-wall-street-journal", "the-washington-post",
            "time", "usa-today"};
    Locale[] localeArray = {Locale.CANADA, Locale.CHINA, Locale.FRANCE, Locale.GERMANY, Locale.ITALY,
            Locale.JAPAN, Locale.KOREA, Locale.TAIWAN, Locale.UK, Locale.US};
    Locale[] localeArray2 = {stringToLocale("en-us"), stringToLocale("en-au"), stringToLocale("en-gb"), stringToLocale("en-ca"), stringToLocale("en-nz"),
            stringToLocale("en-SG"), Locale.KOREA, Locale.TAIWAN, Locale.UK, Locale.US};
    public String[] unitsArray = new String[]{"f", "c"};

    public static MainActivity instance() {
        return inst;
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }
    public Locale stringToLocale(String s) {
        String l = "";
        String c = "";
        StringTokenizer tempStringTokenizer = new StringTokenizer(s,",");
        if(tempStringTokenizer.hasMoreTokens())
            l = (String )tempStringTokenizer.nextElement();
        if(tempStringTokenizer.hasMoreTokens())
            c = (String)tempStringTokenizer.nextElement();
        return new Locale(l,c);
        /*
        return new Locale.Builder().setLanguageTag(s).build();*/
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        Log.i("oncreate MainActivity", "running");
        //get username. Make sure this calls when return from settings activity
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName), Context.MODE_PRIVATE);
        voice = sharedPref.getString(getString(R.string.voiceSetting), "UK");
        newsSource = sharedPref.getString(getString(R.string.newsSetting), "cnn");
        name = sharedPref.getString(getString(R.string.userName), "");
        units = sharedPref.getString(getString(R.string.unitsUsed), "f");

        Log.i("name_of_user", " " + name);
        Log.i("name_of_voice", " " + voice);
        Log.i("name_of_news", " " + newsSource);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        onOffSwitch = (Switch) findViewById(R.id.switchButton);
        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        /*alarmTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                updateAlarm();
                Log.i("onTimeChangedWorking", "yep");
            }
        });*/
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    Locale language = localeArray[java.util.Arrays.asList(accents).indexOf(voice)];
                    if (textToSpeech.isLanguageAvailable(language) != TextToSpeech.LANG_MISSING_DATA || textToSpeech.isLanguageAvailable(language) != TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech.setLanguage(language);
                    } else {
                        textToSpeech.setLanguage(Locale.UK);
                    }
                }
            }
        });
        //find the current city
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            city = getLocationName(latitude, longitude);
        }

        TimePicker.OnTimeChangedListener onTimeChangedListener = new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                Log.i("timeChanged", "listener called");
                updateAlarm();

            }
        };
        alarmTimePicker.setOnTimeChangedListener(onTimeChangedListener);
    }

    public void openSettings(View view) {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName), Context.MODE_PRIVATE);
        voice = sharedPref.getString(getString(R.string.voiceSetting), "UK");
        newsSource = sharedPref.getString(getString(R.string.newsSetting), "cnn");
        name = sharedPref.getString(getString(R.string.userName), "");
        units = sharedPref.getString(getString(R.string.unitsUsed), "f");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Currently using:\n" + convertToNormalText(voice, getString(R.string.voiceSetting)))
                .setItems(R.array.language, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        changeVoice(accents[which]);
                        Log.i("changed voice to ", accents[which] + "");
                        alertDiagName();
                    }
                });
        builder.create();
        builder.show();
    }

    public void alertDiagName() {
        final EditText input = new EditText(this);
        input.setHint("Name");
        input.setText(name + "");
        String title = "";
        if (name.equals("")) {
            title = "Enter Name:";
        } else {
            title = "Currently using:\n" + name;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //provide user with caution before uninstalling
                changeName(input.getText().toString());
                alertDiagNews();
            }
        });
        builder.setTitle(title);
        builder.setView(input);
        builder.create();
        builder.show();
    }

    public void alertDiagNews() {


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Currently using:\n" + convertToNormalText(newsSource, getString(R.string.newsSetting)))
                .setItems(R.array.news, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        changeNews(news[which]);
                        alertDiagUnits();
                    }
                });
        builder.create();
        builder.show();
    }

    public void alertDiagUnits() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Currently using:\n" + convertToNormalText(units, getString(R.string.unitsUsed)))
                .setItems(R.array.units, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        changeUnits(unitsArray[which]);
                        alertDiagNotification();

                    }
                });
        builder.create();
        builder.show();
    }
    public void alertDiagNotification(){

        final EditText input = new EditText(this);
        input.setHint("Notification for morning");
        if(notification!=null){
            input.setText(notification + "");
        }
        String title = "Enter a reminder for the morning:";

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //provide user with caution before uninstalling
                changeNotification(input.getText().toString());
            }
        });
        builder.setTitle(title);
        builder.setView(input);
        builder.create();
        builder.show();
    }

    public void changeVoice(String setting) {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.voiceSetting), setting);
        editor.apply();

        Locale language = localeArray[java.util.Arrays.asList(accents).indexOf(voice)];
        if (textToSpeech.isLanguageAvailable(language) != TextToSpeech.LANG_MISSING_DATA || textToSpeech.isLanguageAvailable(language) != TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech.setLanguage(language);
            Log.i("lang set", "true");
            Log.i("set to ", language.toString());
        } else {
            textToSpeech.setLanguage(Locale.UK);
        }
    }

    public void changeName(String setting) {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.userName), setting);
        editor.apply();
        name = sharedPref.getString(getString(R.string.userName), "");

    }

    public void changeUnits(String setting) {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.unitsUsed), setting);
        editor.apply();
        units = sharedPref.getString(getString(R.string.unitsUsed), "f");

    }

    public void changeNews(String setting) {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.newsSetting), setting);
        editor.apply();
        newsSource = sharedPref.getString(getString(R.string.newsSetting), "cnn");

    }
    public void changeNotification(String setting){
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.notificationSet), setting);
        editor.apply();
        notification = sharedPref.getString(getString(R.string.notificationSet), "");
    }

    public String convertToNormalText(String txt, String type) {
        String output;
        int indx;
        String[] array;
        if (type.equals(getString(R.string.voiceSetting))) {
            //get index of raw text
            indx = java.util.Arrays.asList(accents).indexOf(txt);
            array = getResources().getStringArray(R.array.language);
            //get readable equivalent
            return array[indx];
        } else if (type.equals(getString(R.string.newsSetting))) {
            //get index of raw text
            indx = java.util.Arrays.asList(news).indexOf(txt);
            array = getResources().getStringArray(R.array.news);
            //get readable equivalent
            return array[indx];
        } else if (type.equals(getString(R.string.unitsUsed))) {
            //get index of raw text
            indx = java.util.Arrays.asList(unitsArray).indexOf(txt);
            array = getResources().getStringArray(R.array.units);
            //get readable equivalent
            return array[indx];
        }
        return "";
    }
    /*
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        updateAlarm();
        Log.i("was user interaction", "true");
    }*/

    public String getUnits() {
        String units;
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName), Context.MODE_PRIVATE);
        units = sharedPref.getString(getString(R.string.unitsUsed), "f");
        Log.i("Units: ", units);
        return units;
    }

    public void updateAlarm() {
        Switch switchView = (Switch) findViewById(R.id.switchButton);

        if (switchView.isChecked()) {
            //if alarm is already on, restart the alarm with new time. If keeping track of time sleeping, don't restart that here.
            setAlarmTime();
            Log.i("alarmUpdate", "alarmUpdated");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Intent myIntent = new Intent(MainActivity.this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, PendingIntent.FLAG_NO_CREATE);

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location!=null){
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            city = getLocationName(latitude, longitude);
        }


        Log.i("onResume MainActivity", "running");
        boolean alarmUp = (pendingIntent != null);
        Log.i("alarm is already made: ", "is " + alarmUp);
        if (alarmUp)
        {
            onOffSwitch.setChecked(true);
            onOffSwitch.setText("On");
            SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName),Context.MODE_PRIVATE);
            int minute = sharedPref.getInt(getString(R.string.minuteSetting), 1);
            int hour = sharedPref.getInt(getString(R.string.hourSetting), 1);
            Log.i("minuts for on alarm", minute + "");
            Log.i("hours for on alarm", hour + "");

            alarmTimePicker.setCurrentHour(hour);
            alarmTimePicker.setCurrentMinute(minute);
            //reset time picker to alarm time

        }
        else{
            onOffSwitch.setChecked(false);
            onOffSwitch.setText("Off");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //before leave app, cancel alarm
        Log.i("ondestroy mainactiv", "called");

        Intent myIntent = new Intent(MainActivity.this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, PendingIntent.FLAG_NO_CREATE);
        boolean alarmUp = (pendingIntent != null);

        if(alarmUp){
            pendingIntent.cancel();
        }
    }

    public void onToggleClicked(View view) {

        //view.getId()== R.id.alarmToggle is needed because i call toggle when change time to update alarm
        if (((Switch) view).isChecked()) {
            //alarm turned on
            setAlarmTime();
            ((Switch) view).setText("On");
        }

        else {
            //alarm turned off
            ((Switch) view).setText("Off");
            Intent myIntent = new Intent(MainActivity.this, AlarmReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, PendingIntent.FLAG_NO_CREATE);
            pendingIntent.cancel();
            //alarmManager.cancel(pendingIntent);
            if(AlarmReceiver.ringtone!=null && AlarmReceiver.ringtone.isPlaying()){
                AlarmReceiver.ringtone.stop();
            }
            if(textToSpeech.isSpeaking()){
                textToSpeech.stop();
            }
        }
    }

    public void setAlarmText(final String topArticle, final String secondArticle, final String thirdArticle, final int windSpeed, final int airTemp, final String weatherCond, String speedUnit, String tempUnit) {
        //alarmTextView.setText( topArticle +" "+secondArticle + " " + thirdArticle );
        final String speed;
        final String temp;
        final String message;
        Log.e("Main Activity", "setAlarmText called with news param");

        if(topArticle ==null || secondArticle==null || thirdArticle==null){
            //couldn't get news
            Log.e("Main Activity", "setAlarmText fail: null articles");

            if(speedUnit.equals("mph")){
                speed = "miles per hour";
            }
            else{
                speed = "kilometers per hour";
            }
            if(tempUnit.equals("F")){
                temp = "Fahrenheit";
            }
            else{
                temp = "Celsius";
            }
            message = getGreeting() +" The weather in " + city+ "is " + weatherCond + "." + " Wind speed is " + windSpeed + " "+ speed +", air temperature is " + airTemp + " degrees "+ temp + ". I could not retrieve news data. " + getNotificationReading() + ". Have a good day";
        }
        else if(weatherCond==null){
            //couldn't get weather
            message = getGreeting() +". Here is your news for the day: " + topArticle +", "+secondArticle + ", and " + thirdArticle + ". I could not retrieve weather data."+getNotificationReading()+ ". Have a good day";
        }
        else {
            Log.e("Main Activity", "setAlarmText success: should work");

            if(speedUnit.equals("mph")){
                speed = "miles per hour";
            }
            else{
                speed = "kilometers per hour";
            }
            if(tempUnit.equals("F")){
                temp = "Fahrenheit";
            }
            else{
                temp = "Celsius";
            }
            message = getGreeting() + ". Here is your news for the day: " + topArticle + ", " + secondArticle + ", and " + thirdArticle +
                    ". The weather in " + city + "is " + weatherCond + "." + " Wind speed is " + windSpeed + " " + speed + ", air temperature is " + airTemp + " degrees " + temp + ". " + getNotificationReading() +". Have a good day";
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            }

        }, 5000);
    }

    public void setAlarmText(final int windSpeed, final int airTemp, final String weatherCond, String speedUnit, String tempUnit) {
        //alarmTextView.setText( topArticle +" "+secondArticle + " " + thirdArticle );
        Log.e("Main Activity", "setAlarmText fail: wrong one called");

        final String speed;
        final String temp;
        final String message;
        if(speedUnit.equals("mph")){
            speed = "miles per hour";
        }
        else{
            speed = "kilometers per hour";
        }
        if(tempUnit.equals("F")){
            temp = "Fahrenheit";
        }
        else{
            temp = "Celsius";
        }
        message = getGreeting() +" The weather in " + city+ "is " + weatherCond + "." + " Wind speed is " + windSpeed + " "+ speed +", air temperature is " + airTemp + " degrees "+ temp + ". "+ getNotificationReading() + ". Have a good day";

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            }

        }, 5000);
    }
    public void setAlarmText() {
        //alarmTextView.setText( topArticle +" "+secondArticle + " " + thirdArticle );
        Handler handler = new Handler();
        final String text = getGreeting() + ". I could not find news or weather information, you may not have internet connection."+ getNotificationReading() +". Have a good day!";

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }

        }, 5000);
    }
    public String getDayOfWeek(){
        String weekDay;
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);
        Calendar calendar = Calendar.getInstance();
        weekDay = dayFormat.format(calendar.getTime());
        return weekDay;
    }
    public String getGreeting(){
        Log.i("name of user", name);
        Random rand = new Random();
        int randInt = rand.nextInt(3);
        switch(randInt){
            case 0:
                return "Good morning, " + name + ".";
            case 1:
                return name + ", good morning.";
            case 2:
                return name + ", it's a great " + getDayOfWeek() + " morning.";
        }
        return "Good morning.";
    }
    public String getNotificationReading(){
        String output = "";

        if(notification==null || notification.replaceAll("\\s+","").equals("")){
            output += "You have no reminders set for today.";
        }
        else{
            output+= "Here is your morning reminder: " + notification;
        }
        return output;
    }
    public String getLocationName(double lattitude, double longitude) {

        String cityName = "Not Found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {

            List<Address> addresses = gcd.getFromLocation(lattitude, longitude,
                    10);

            for (Address adrs : addresses) {
                if (adrs != null) {

                    String city = adrs.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                        System.out.println("city ::  " + cityName);
                    } else {

                    }
                    // // you should also try with addresses.get(0).toSring();

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;

    }
    public void setAlarmTime(){
        Calendar now = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
        calendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
        Intent myIntent = new Intent(MainActivity.this, AlarmReceiver.class);

        //save time that its set
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.prefName),Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.hourSetting), alarmTimePicker.getCurrentHour());
        editor.putInt(getString(R.string.minuteSetting), alarmTimePicker.getCurrentMinute());
        editor.apply();


        //0 is a unique id for this alarm, allow me to override previous alarm with same id
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, 0);

        long timeToSetOff;
        //if set to an hour in the past,
        if(calendar.getTimeInMillis()<= now.getTimeInMillis()){
            //if time set to past, make it go off at next day by adding 24 hours in milliseconds
            timeToSetOff = calendar.getTimeInMillis() + (AlarmManager.INTERVAL_DAY+1);
        }
        else{
            timeToSetOff = calendar.getTimeInMillis();
        }
        Log.i("hours set to", alarmTimePicker.getCurrentHour() + " ");
        Log.i("minutes set to", alarmTimePicker.getCurrentMinute() + " ");

        alarmManager.set(AlarmManager.RTC_WAKEUP, timeToSetOff, pendingIntent);
    }

    public ArrayList<String> getNotificationSounds() {
        RingtoneManager manager = new RingtoneManager(this);
        manager.setType(RingtoneManager.TYPE_ALARM);
        Cursor cursor = manager.getCursor();

        ArrayList<String> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            String id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX);
            String uri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX);

            list.add(uri + "/" + id);
        }

        return list;
    }

}