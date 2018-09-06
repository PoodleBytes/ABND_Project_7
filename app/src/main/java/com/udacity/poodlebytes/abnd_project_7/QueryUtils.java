/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.udacity.poodlebytes.abnd_project_7;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods related to requesting and receiving earthquake data from USGS.
 */
public final class QueryUtils {

    /**
     * Tag for the log messages
     */
    private static final String TAG = QueryUtils.class.getSimpleName();

    //time-out value for http connections
    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECT_TIMEOUT = 15000;


    /**
     * Empty Constructor
     */
    private QueryUtils() {
    }

    /**
     * Query the Guardian API and return a list of {@link News} objects.
     */
    public static List<News> fetchNewsData(String requestUrl) {
        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(TAG, "Problem making the HTTP request.", e);
        }

        // Extract relevant fields from the JSON response and create a News list
        List<News> news = extractFeatureFromJson(jsonResponse);

        // Return the list
        return news;
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(READ_TIMEOUT /* milliseconds */);
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException ie) {
            Log.e(TAG, "Problem retrieving JSON results.", ie);
        } finally {     //close connection & inputStream
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into String from the JSON response
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Return a News array from JSON response.
     */
    private static List<News> extractFeatureFromJson(String newsJSON) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(newsJSON)) {
            return null;
        }

        // Create an empty ArrayList & start adding articles
        List<News> news = new ArrayList<>();

        // Try to parse the JSON response string.
        try {
            Log.i(TAG, "Start Parsing " + newsJSON);
            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(newsJSON);
            JSONObject baseJsonResult = baseJsonResponse.getJSONObject("response");
            JSONArray newsArray = baseJsonResult.getJSONArray("results");

            // For each news article create a news  object
            for (int i = 0; i < newsArray.length(); i++) {

                String title;
                String date;
                String url;
                String author = "Guardian Staff"; //default if no author
                String category;

                // Get a single article at position i within the JSON array
                JSONObject currentNews = newsArray.getJSONObject(i);

                /* Parse JSON values:*/
                //article title
                title = currentNews.getString("webTitle");

                //date
                String rawdate = currentNews.getString("webPublicationDate");
                String[] dateAndTime;
                dateAndTime = rawdate.split("T");
                date = dateAndTime[0];

                //article "url"
                url = currentNews.getString("webUrl");

                /* process  author(s) from tags portion of JSON Object */
                JSONArray authorArray = currentNews.getJSONArray("tags");
                String authorName = "";
                if (authorArray.length() != 0) {
                    //get author from tags portion of JSON Object
                    JSONObject currentAuthor = authorArray.getJSONObject(0);
                    authorName = currentAuthor.getString("webTitle");
                    //Concatenation of author name and type of author (pulled from JSON)
                    StringBuilder authorBuilder = new StringBuilder();
                    authorBuilder.append(authorName);
                    Log.i(TAG, "authorName" + authorName);
                    //Check for and append other authors
                    if (authorArray.length() > 1) {
                        for (int n = 1; n < authorArray.length(); n++) {
                            JSONObject secondaryAuthor = authorArray.getJSONObject(n);
                            String secondAuthor = secondaryAuthor.getString("webTitle");
                            authorBuilder.append(" & ");
                            authorBuilder.append(secondAuthor);
                            Log.i(TAG, "secondAuthor " + secondAuthor);
                        }
                    }
                    if (authorBuilder.toString() != null) {
                        author = authorBuilder.toString();
                    }

                }

                //article category
                category = currentNews.getString("sectionName");

                // Create a news object from the JSON response.
                News myNews = new News(title, date, url, author, category);

                // Add the article to the list
                news.add(myNews);
            }
        } catch (JSONException e) {
            Log.e("QueryUtils", "Problem parsing JSON results", e);
        }

        // Return the list of earthquakes
        return news;
    }

    public class URLBuilder {

        public static final String HttpRequest = "http://content.guardianapis.com/search?show-tags=contributor&q=";
        private static final String API_KEY = BuildConfig.ApiKey;

        public String BuildURL(String GUARDIAN_REQUEST) {
            // add web address and key to input string and return full web address.
            return HttpRequest + GUARDIAN_REQUEST + API_KEY;
        }
    }
}
