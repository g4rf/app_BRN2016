package de.sophvaerck.eventkrake.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.sophvaerck.eventkrake.R;

/**
 * Created by Jan on 08.06.2016.
 */
public class ManageData {
    private static ArrayList<Location> locations = new ArrayList<>();
    private static ArrayList<Event> events = new ArrayList<>();

    public static void invalidate() {
        locations.clear();
        events.clear();
    }

    public static Location getLocation(String id) {
        if(locations.size() == 0) getLocations();

        for (Location l: locations) {
            if(l.id.equals(id)) {
                return l;
            }
        }

        return null;
    }

    public static ArrayList<Location> getLocations(Date start, Date end) {
        if(locations.size() == 0) getLocations();

        Set<String> locationIds = new HashSet();
        for(Event e : getEvents(start, end)) {
            locationIds.add(e.locationId);
        }

        ArrayList<Location> dateLocations = new ArrayList<>();

        for (Location l: locations) {
            if(locationIds.contains(l.id))
                dateLocations.add(l);
        }

        return dateLocations;
    }

    public static Event getEvent(String id) {
        if(events.size() == 0) getEvents();

        for (Event e: events) {
            if(e.id.equals(id)) {
                return e;
            }
        }

        return null;
    }

    public static ArrayList<Event> getEvents(Date start, Date end) {
        if(events.size() == 0) getEvents();

        ArrayList<Event> dateEvents = new ArrayList<>();

        for (Event e: events) {
            if(e.dateEnd.before(start)) continue;
            if(e.dateStart.after(end)) continue;

            dateEvents.add(e);
        }

        return dateEvents;
    }

    public static ArrayList<Event> getEvents(String s) {
        if(events.size() == 0) getEvents();

        s = s.toLowerCase();
        ArrayList<Event> searchEvents = new ArrayList<>();

        for (Event e: events) {
            if(e.title.toLowerCase().contains(s) || e.text.toLowerCase().contains(s))
                searchEvents.add(e);
        }

        return searchEvents;
    }

    public static ArrayList<Event> getEvents(Location l) {
        if(events.size() == 0) getEvents();

        ArrayList<Event> locationEvents = new ArrayList<>();
        for (Event e: events) {
            if(e.locationId.equals(l.id)) locationEvents.add(e);
        }

        return locationEvents;
    }

    public static ArrayList<Event> getEvents(Location l, Date start, Date end) {
        if(events.size() == 0) getEvents();

        ArrayList<Event> es = new ArrayList<>();

        for (Event e: events) {
            if(! e.locationId.equals(l.id)) continue;
            if(e.dateEnd.before(start)) continue;
            if(e.dateStart.after(end)) continue;

            es.add(e);
        }

        return es;
    }

    public static ArrayList<Event> getEvents() {
        // cached events
        if(events.size() > 0) return events;

        // get events
        try {
            JSONObject data = new JSONObject(readData());
            JSONObject jsonEvents = data.getJSONObject("events");

            Iterator<String> iterator = jsonEvents.keys();
            while(iterator.hasNext()) {
                String id = iterator.next();
                try {
                    JSONObject jsonEvent = jsonEvents.getJSONObject(id);
                    Event javaEvent = new Event();

                    javaEvent.userEmail = jsonEvent.getString("useremail");
                    javaEvent.id = jsonEvent.getString("id");
                    javaEvent.locationId = jsonEvent.getString("locationid");
                    javaEvent.title = jsonEvent.getString("title").replace("&amp;", "&");
                    javaEvent.excerpt = jsonEvent.getString("excerpt");
                    javaEvent.text = jsonEvent.getString("text");
                    javaEvent.url = jsonEvent.getString("url");
                    javaEvent.image = jsonEvent.getString("image");
                    javaEvent.festival = jsonEvent.getString("festival");
                    javaEvent.visible = jsonEvent.getString("visible").equals("true");
                    javaEvent.tags = jsonEvent.getString("tags");

                    javaEvent.dateStart = Helper.mysqlDate.parse(jsonEvent.getString("datetime"));
                    javaEvent.dateEnd = Helper.mysqlDate.parse(jsonEvent.getString("datetime_end"));

                    JSONArray jsonCategories = jsonEvent.getJSONArray("categories");
                    for (int i = 0; i < jsonCategories.length(); i++) {
                        javaEvent.categories.add(jsonCategories.getString(i));
                    }

                    events.add(javaEvent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // sorting
        Collections.sort(events);

        return events;
    }

    public static ArrayList<Location> getLocations() {
        // cached locations
        if(locations.size() > 0) return locations;

        // get locations
        try {
            JSONObject data = new JSONObject(readData());
            JSONObject jsonLocations = data.getJSONObject("locations");

            Iterator<String> iterator = jsonLocations.keys();
            while(iterator.hasNext()) {
                String id = iterator.next();
                try {
                    if(id.equals("0")) continue; // falls es Events ohne Location gibt
                    JSONObject jsonLocation = jsonLocations.getJSONObject(id);
                    Location javaLocation = new Location();

                    javaLocation.userEmail = jsonLocation.getString("useremail");
                    javaLocation.id = jsonLocation.getString("id");
                    javaLocation.name = jsonLocation.getString("name").replace("&amp;", "&");
                    javaLocation.address = jsonLocation.getString("address");
                    javaLocation.lat = jsonLocation.getDouble("lat");
                    javaLocation.lng = jsonLocation.getDouble("lng");
                    javaLocation.text = jsonLocation.getString("text");
                    javaLocation.url = jsonLocation.getString("url");
                    javaLocation.image = jsonLocation.getString("image");
                    javaLocation.visible = jsonLocation.getString("visible").equals("true");
                    javaLocation.tags = jsonLocation.getString("tags");

                    JSONArray jsonCategories = jsonLocation.getJSONArray("categories");
                    for (int i = 0; i < jsonCategories.length(); i++) {
                        javaLocation.categories.add(jsonCategories.getString(i));
                    }

                    JSONArray jsonFestivals = jsonLocation.getJSONArray("festivals");
                    for (int i = 0; i < jsonFestivals.length(); i++) {
                        javaLocation.festivals.add(jsonFestivals.getString(i));
                    }

                    locations.add(javaLocation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // sorting
        Collections.sort(locations);

        return locations;
    }

    private static String readData() {
        File file = new File(Helper.context.getFilesDir(), "events.json");

        InputStream is = null;
        if(! file.exists() || ! file.canRead() || file.length() == 0) { // read from resource
            is = Helper.context.getResources().openRawResource(R.raw.events);
        } else { // read from file
            try {
                is = Helper.context.openFileInput("events.json");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (is == null) return "";

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }
}
