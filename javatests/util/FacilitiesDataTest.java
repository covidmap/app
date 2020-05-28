package util;

import com.google.type.LatLng;
import covidmap.schema.Facility;
import org.junit.jupiter.api.Test;
import server.FacilitiesManager;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


/** Test the static facilities dataset for consistency. */
public class FacilitiesDataTest {
  private static final String nearbyTest = "de0xfjt95kxx";
  private static final String expectedFindId = "700641";

  @Test void testLoadFacilitiesManager() {
    final FacilitiesManager manager = FacilitiesManager.Companion.load();
    assertNotNull(manager, "loaded manager should not be null");
  }

  @Test void testFacilitiesAll() {
    final FacilitiesManager manager = FacilitiesManager.Companion.load();
    assertNotNull(manager, "loaded manager should not be null");
    List<Facility> facilities = manager.stream().collect(Collectors.toList());
    assertNotNull(facilities, "should be able to acquire an interable of all facilities");
    List<Facility> facilities2 = manager.stream().collect(Collectors.toList());
    assertEquals(facilities.size(), facilities2.size(), "facilities should remain immutable");
  }

  @Test void testFacilitiesNearbyGeohash() {
    final FacilitiesManager manager = FacilitiesManager.Companion.load();
    assertNotNull(manager, "loaded manager should not be null");
    List<Facility> facilities = manager.nearby(nearbyTest).collect(Collectors.toList());
    assertNotNull(facilities, "should be able to query for nearby facilities by geohash");
    assertFalse(facilities.isEmpty(), "resulting facilities should not be empty from geohash search");
    assertEquals(facilities.stream().filter((item) -> item.getKey().getId().equals(expectedFindId)).count(),
      1,
      "should find expected facility after geohash search");
  }

  @Test void testFacilitiesNearbyGeopoint() {
    final FacilitiesManager manager = FacilitiesManager.Companion.load();
    final LatLng point = LatLng.newBuilder()
      .setLatitude(37.780727)
      .setLongitude(-122.38876)
      .build();

    assertNotNull(manager, "loaded manager should not be null");
    List<Facility> facilities = manager.nearby(point).collect(Collectors.toList());
    assertNotNull(facilities, "should be able to query for nearby facilities by geohash");
    assertFalse(facilities.isEmpty(), "resulting facilities should not be empty from geohash search");
  }
}
