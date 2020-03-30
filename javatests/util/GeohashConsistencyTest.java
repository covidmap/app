package util;

import ch.hsr.geohash.GeoHash;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/** Test geohash consistency between the server and BigQuery. */
public class GeohashConsistencyTest {
  private static final int characterSize = 12;
  private static final String computed1 = "de0xfjt95ksc";
  private static final String expected1 = "de0xfjt95kscjzyk5309";
  private static final String computed2 = "de28z5uvjd48";
  private static final String expected2 = "de28z5uvjd48hd5qs0wk";
  private static final double expected1Lat = 18.2677131;
  private static final double expected1Long = -66.70128518;
  private static final double expected2Lat = 18.43455435;
  private static final double expected2Long = -66.4824951;

  @Test void testCompareGeohashConsistency() {
    GeoHash hash1 = GeoHash.withCharacterPrecision(expected1Lat, expected1Long, characterSize);
    assertNotNull(hash1, "hash object should not be null");
    assertEquals(computed1, hash1.toBase32(), "hash1 should be expected value");

    GeoHash hash2 = GeoHash.withCharacterPrecision(expected2Lat, expected2Long, characterSize);
    assertNotNull(hash2, "hash object should not be null");
    assertEquals(computed2, hash2.toBase32(), "hash2 should be expected value");

    assertTrue(expected1.startsWith(hash1.toBase32()), "geohash prefix should match for hash1");
    assertTrue(expected2.startsWith(hash2.toBase32()), "geohash prefix should match for hash2");
  }
}
