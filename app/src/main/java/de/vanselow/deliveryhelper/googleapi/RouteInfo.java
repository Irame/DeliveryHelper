package de.vanselow.deliveryhelper.googleapi;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.HashMap;
import java.util.Map;

public class RouteInfo<T> {
    public String overviewPolyline;
    public LatLngBounds bounds;
    public Map<T, Integer> waypointOrder;

    RouteInfo() {
        waypointOrder = new HashMap<>();
    }
}
