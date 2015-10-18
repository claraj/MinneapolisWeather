package com.example.hello.minneapolisweather;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends ActionBarActivity {

	private static final String TAG = "Weather Application";
	TextView tempTV;
	ImageView radarImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tempTV = (TextView) findViewById(R.id.today_temp);

		//Start asynchronous process to request temperature data
		//Send URL as argument.

		//URL needs valid key. Read in from /res/raw/key.txt in the following method
		String key = getKeyFromRawResource();

		//Provide base URL into which we will unsert the key
		//Note %s instead of key. Will use string formatting to replace %s with key
		String wuTempUrlBase = "http://api.wunderground.com/api/%s/conditions/q/MN/Minneapolis.json";

		//TODO handle case when key == null. The string key will be null if it can't can't be read from the file
		String wuTempUrl = String.format(wuTempUrlBase, key);

		new RequestCurrentMplsTempTask().execute(wuTempUrl);

		radarImage = (ImageView) findViewById(R.id.weather_radar);

		String wuRadarUrlBase = "http://api.wunderground.com/api/%s/radar/q/MN/Minneapolis.gif?width=280&height=280&newmaps=1";
		String wuRadarUrl = String.format(wuRadarUrlBase, key);

		new RequestMinneapolisWeatherMap().execute(wuRadarUrl);
	}

	private String getKeyFromRawResource() {

		//Open the raw resource, as an InputStream
		InputStream keyStream = getResources().openRawResource(R.raw.key);
		//Wrap InputStream in InputStreamReader and then in a BufferedReader...
		BufferedReader keyStreamReader = new BufferedReader(new InputStreamReader(keyStream));
		try {
			//Can read lines of text from a BufferedReader
			String key = keyStreamReader.readLine();
			return key;
		} catch (IOException e) {
			Log.e(TAG, "Error reading secret key from raw resource file", e);
			return null;
		}
		//TODO how would you handle a situation when you needed to store and read more than one key?
	}


	class RequestMinneapolisWeatherMap extends AsyncTask<String, String, Boolean> {

		Bitmap weatherRadar;

		@Override
		protected Boolean doInBackground(String... urls) {

			Boolean completedSuccessfully = false;
			try {
				//Create a URL for the first argument passed
				URL url = new URL(urls[0]);
				//Create a connection, makes request to server
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				//Again, response is returned as a byte stream
				InputStream responseStream = connection.getInputStream();
				//Processing is simpler since Android's BitmapFactory can turn the stream into an image
				weatherRadar = BitmapFactory.decodeStream(responseStream);
				completedSuccessfully = true;
			} catch (Exception e) {
				Log.e(TAG, "Error fetching weather map", e);
			}
			return completedSuccessfully;
		}

		@Override
		protected void onPostExecute(Boolean completed) {

			//We use the result here to check that no errors were thrown during fetching and
			//decoding the data into the Bitmap.
			//If all seems to have gone well, set the
			//radarImage Bitmap to be the Bitmap fetched from the server.

			if (completed) {
				radarImage.setImageBitmap(weatherRadar);
			}
			else {
				Log.e(TAG, "No image to display");
			}
		}
	}


	class RequestCurrentMplsTempTask extends AsyncTask<String, String, String>{
		@Override
		protected String doInBackground(String...urls) {

			String responseString = null;

			try {

				URL url = new URL(urls[0]);  //Use the first argument supplied
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();

				//Connection returns an InputStream (think input to the program). It represents a stream of bytes
				InputStream responseStream = new BufferedInputStream(connection.getInputStream());

				//We can't use single bytes. Need to read all of the data into a String so we can treat it as JSON
				//First, wrap in a InputStreamReader. This will convert the bytes into characters
				InputStreamReader streamReader = new InputStreamReader(responseStream);

				//Create a StringBuffer, will be used to collect all the characters from the InputStreamReader
				StringBuffer buffer = new StringBuffer();

				int c;
				while ( (c = streamReader.read() ) != -1) {
					//Cast c to a char and append to the buffer
					buffer.append( (char)c );
				}

				responseString = buffer.toString();
				Log.i("WEATHER", "String is " + responseString);

			} catch (Exception e) {
				//TODO catch and handle exceptions separately - for parsing the Url, for the connection, for processing the response stream...
				Log.e(TAG, "Error fetching weather data, see exception for details: ", e);

			}

			return responseString;
		}

		@Override
		protected void onPostExecute(String result) {

			if (result != null) {
				try {
					JSONObject response = new JSONObject(result);
					JSONObject forecast = response.getJSONObject("current_observation");
					String temp = forecast.getString("temp_f");
					tempTV.setText("The temperature in Minneapolis is " + temp + "F");
				} catch (JSONException e) {
					Log.e(TAG, "parsing error, check schema?", e);
					tempTV.setText("Error fetching temperature for Minneapolis");
				}
			}
			else {
				tempTV.setText("Error fetching temperature for Minneapolis");
				Log.e(TAG, "Result was null, check doInBackground for errors");
			}
		}
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
}
