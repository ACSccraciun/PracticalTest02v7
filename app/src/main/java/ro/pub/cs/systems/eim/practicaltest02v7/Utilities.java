package ro.pub.cs.systems.eim.practicaltest02v7;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utilities {
    public static BufferedReader getReader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static PrintWriter getWriter(Socket socket) throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }

    public static String getHourAndMinute(String utcNistTime) {
        // Define the input date format (adjust it according to the actual NIST time format you receive)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.US);
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            // Parse the input string to a Date object
            Date date = inputFormat.parse(utcNistTime);

            // Define the output format to extract hour and minute
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH,mm", Locale.US);
            outputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            // Format the Date object to the desired output string
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // or handle the error as needed
        }
    }

    public static String getUrlContent(String urlString) {
        StringBuilder result = new StringBuilder();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return result.toString();
    }
}
