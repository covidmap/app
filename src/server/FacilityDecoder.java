package server;


import ch.hsr.geohash.GeoHash;
import com.google.common.base.Joiner;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.type.LatLng;
import covidmap.schema.Facility;
import covidmap.schema.Facility.FacilityKey;
import covidmap.schema.Facility.FacilityCapabilities;
import covidmap.schema.Facility.FacilityCapabilities.TraumaCapability;
import covidmap.schema.Facility.FacilityContact;
import covidmap.schema.Facility.FacilityContact.FacilityPhone;
import covidmap.schema.Facility.FacilityGovernance;
import covidmap.schema.Facility.FacilityLocation;
import covidmap.schema.Facility.FacilityLocation.FacilityAddress;
import covidmap.schema.Facility.FacilityType;
import covidmap.schema.Facility.TraumaType;
import gust.backend.runtime.Logging;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;


/** Decodes `Facility` records from raw JSON. */
public final class FacilityDecoder {
  /** Access to GSON for JSON decoding. */
  private static final Gson gson = new GsonBuilder().create();

  /** Logging pipe. */
  private static final Logger logging = Logging.logger(FacilityDecoder.class);

  /** Character-size of computed geohash values. */
  private static final int geohashSize = 12;

  /** Describes the raw flattened facility format. */
  private static final class RawFacility {
    Double latitude;
    Double longitude;
    String state;
    String city;
    String name;
    String objectId;
    String rowId;
    String address;
    String zip;
    String telephone;
    String type;
    String status;
    Boolean open;
    String county;
    String country;
    String naicsCode;
    String naicsDesc;
    String website;
    String altName;
    String ownerType;
    Integer beds;
    String trauma1;
    String trauma2;
    Boolean helipad;

    @SuppressWarnings("unused") String source;
    @SuppressWarnings("unused") String owner;
    @SuppressWarnings("unused") Boolean pediatric;

    /**
     * Decode a raw facility from the provided JSON line.
     *
     * @param line Line of JSON to decode.
     * @return Expanded facility.
     */
    static @Nonnull RawFacility decode(@Nonnull String line) {
      try {
        return gson.fromJson(line, RawFacility.class);
      } catch (JsonSyntaxException jse) {
        logging.error("Failed to decode facility data:\n" + line);
        throw jse;
      }
    }

    /**
     * Check a generic value for `null`, and additionally check strings for emptiness.
     *
     * @param value Value to check.
     * @param label Label for this value.
     * @param <T> Generic type for this value.
     * @return The value, after being checked, assuming checks pass.
     * @throws IllegalStateException If the provided value does not pass checks.
     */
    @CanIgnoreReturnValue
    private @Nonnull <T> T check(@Nullable T value, String label) {
      if (value == null)
        throw new IllegalStateException("Dataset could not resolve value for property '" + label + "'.");
      if (value instanceof String) {
        String val = (String)value;
        if (val.trim().isEmpty())
          throw new IllegalStateException("Dataset could not resolve string value for property '" + label + "'.");
      }
      return value;
    }

    /**
     * Re-format a generic string value so it is no longer in all-caps form.
     *
     * @param value String to re-format.
     * @return Pretty-formatted string.
     */
    private @Nonnull String prettify(@Nonnull String value) {
      if (value.isEmpty())
        return value;
      if (value.contains(" ")) {
        // split by space
        String[] segments = value.split(" ");
        ArrayList<String> pretty = new ArrayList<>(segments.length);

        // for each segment, trim and capitalize
        for (String element : segments) {
          String trimmed = element.trim();
          if (trimmed.isEmpty())
            continue;
          pretty.add(trimmed.charAt(0) + trimmed.substring(1).toLowerCase());
        }
        return Joiner.on(" ").join(pretty).replace(" And ", " & ");
      } else {
        String trimmed = value.trim();
        return trimmed.charAt(0) + value.substring(1).toLowerCase();
      }
    }

    /**
     * Resolve the type of facility, from the specified string value.
     *
     * @param type Type of facility.
     * @return Enumerated facility type.
     */
    private @Nonnull FacilityType resolveType(@Nonnull String type) {
      switch (type.trim().toUpperCase()) {
        case "GENERAL ACUTE CARE": return FacilityType.GENERAL_ACUTE_CARE;
        case "CRITICAL ACCESS": return FacilityType.CRITICAL_ACCESS;
        case "PSYCHIATRIC": return FacilityType.PSYCHIATRIC;
        case "LONG TERM CARE": return FacilityType.LONG_TERM_CARE;
        case "REHABILITATION": return FacilityType.REHABILITATION;
        case "MILITARY": return FacilityType.MILITARY;
        case "CHILDREN": return FacilityType.CHILDREN;
        case "SPECIAL": return FacilityType.SPECIAL;
        case "WOMEN": return FacilityType.WOMEN;
        case "CHRONIC DISEASE": return FacilityType.CHRONIC_DISEASE;
      }
      throw new IllegalStateException("Unrecognized facility type: '" + type + "'.");
    }

    /**
     * Resolve the governance type for a facility, from the specified string value.
     *
     * @param governance Governance type for this facility.
     * @return Enumerated facility type.
     */
    private @Nonnull FacilityGovernance resolveGovernance(@Nonnull String governance) {
      switch (governance.trim().toUpperCase()) {
        case "GOVERNMENT": return FacilityGovernance.GOVERNMENT;
        case "NON-PROFIT": return FacilityGovernance.NON_PROFIT;
        case "PROPRIETARY": return FacilityGovernance.PRIVATE;
      }
      return FacilityGovernance.UNKNOWN_TYPE;
    }

    /**
     * Resolve the trama type specified by the provided string.
     *
     * @param type Type of trauma equipment/certification to resolve.
     * @return Enumerated facility type.
     */
    private @Nonnull Optional<TraumaCapability.Builder> resolveTraumaType(@Nonnull String type) {
      final TraumaType resolved;
      switch (type.trim().toUpperCase().replace(" PEDIATRIC", "")) {
        case "LEVEL I": resolved = TraumaType.LEVEL_1; break;
        case "LEVEL II": resolved = TraumaType.LEVEL_2; break;
        case "LEVEL III": resolved = TraumaType.LEVEL_3; break;
        case "LEVEL IV": resolved = TraumaType.LEVEL_4; break;
        case "LEVEL V": resolved = TraumaType.LEVEL_5; break;
        case "TRH": resolved = TraumaType.TRH; break;
        case "TRF": resolved = TraumaType.TRF; break;
        case "CTH": resolved = TraumaType.CTH; break;
        case "ATH": resolved = TraumaType.ATH; break;
        case "TRAUMA SYSTEM HOSPITAL": resolved = TraumaType.TRAUMA_SYSTEM_HOSPITAL; break;
        case "RTC": resolved = TraumaType.RTC; break;
        case "RTH": resolved = TraumaType.RTH; break;
        case "AREA": resolved = TraumaType.AREA; break;
        case "CTF": resolved = TraumaType.CTF; break;
        case "PARC": resolved = TraumaType.PARC; break;
        case "RPTC": resolved = TraumaType.RPTC; break;

        case "NOT DESIGNATED":
        case "UNCLASSIFIED":
          return Optional.empty();

        default: throw new IllegalStateException("Unrecognized trauma type: '" + type + "'.");
      }
      return Optional.of(TraumaCapability.newBuilder()
        .setTraumaType(resolved)
        .setPediatric(type.toUpperCase().contains("PEDIATRIC")));
    }

    /** @return Filled-out builder of this facility's capabilities. */
    private @Nonnull FacilityCapabilities.Builder specifyCapabilities() {
      FacilityCapabilities.Builder builder = FacilityCapabilities.newBuilder();
      builder.setHelipad(this.check(this.helipad, "helipad"));
      if (this.beds != null && this.beds > 0) builder.setBeds(this.beds);

      if (this.trauma1 != null && !this.trauma1.isEmpty()) {
        this.resolveTraumaType(this.trauma1.trim()).ifPresent(builder::addTrauma);
        if (this.trauma1.toLowerCase().contains("pediatric"))
          builder.setPediatric(true);
      }
      if (this.trauma2 != null && !this.trauma2.isEmpty())
        this.resolveTraumaType(this.trauma2.trim()).ifPresent(builder::addTrauma);

      if (this.trauma2 != null && this.trauma2.toLowerCase().contains("pediatric"))
        builder.setPediatric(true);
      return builder;
    }

    /** @return Empty, or the decoded URL, if the provided string is a valid URL. */
    private @Nonnull Optional<URL> decodeURL(@Nonnull String url) {
      try {
        return Optional.of(new URL(url));
      } catch (MalformedURLException err) {
        try {
          // try adding `http://` to it
          return Optional.of(new URL("http://" + url));
        } catch (MalformedURLException err2) {
          logging.warn("Failed to decode string as URL: '" + url + "'. Skipping value.");
          return Optional.empty();
        }
      }
    }

    /**
     * Export this raw facility record as a Proto record.
     *
     * @return Fabricated proto record.
     */
    @Nonnull Facility export() {
      Facility.Builder builder = Facility.newBuilder();
      FacilityLocation.Builder location = FacilityLocation.newBuilder();
      FacilityAddress.Builder address = FacilityAddress.newBuilder();
      FacilityContact.Builder contact = FacilityContact.newBuilder();

      // start with the key
      builder.setKey(FacilityKey.newBuilder()
          .setId(check(this.rowId, "row ID"))
          .setObjectId(check(this.objectId, "object ID")));

      // next up: name + alt name
      builder.setName(prettify(check(this.name, "name")));
      if (this.altName != null && !this.altName.isEmpty()) builder.addAlternateName(this.altName);

      // next up: classification
      builder.setType(resolveType(check(this.type, "type")));
      if ((this.open != null && this.open) || this.status.toLowerCase().equals("open")) builder.setOpen(true);
      builder.setNaics(check(this.naicsCode, "naicsCode"));
      builder.setCategory(prettify(check(this.naicsDesc, "naicsDesc")));
      if (this.ownerType != null)
        builder.setGovernance(resolveGovernance(check(this.ownerType, "ownerType")));

      // lat/long
      check(this.latitude, "latitude");
      check(this.longitude, "longitude");
      LatLng.Builder point = LatLng.newBuilder()
        .setLatitude(this.latitude)
        .setLongitude(this.longitude);
      location.setPoint(point);

      // compute + set geohash
      String geohash = GeoHash
        .withCharacterPrecision(this.latitude, this.longitude, geohashSize)
        .toBase32();
      check(geohash, "geohash");
      location.setHash(geohash);

      // build the address
      check(this.country, "country");
      check(this.state, "state");
      check(this.city, "city");
      check(this.zip, "zip");
      check(this.address, "address");

      address.setCountry(this.country.toUpperCase());
      address.setCounty(prettify(this.county));
      address.setCity(prettify(this.city));
      if ("US".equals(this.country.toUpperCase()) || "USA".equals(this.country.toUpperCase()))
        address.setUsState(check(this.state, "US state").toUpperCase());
      else
        address.setProvince(check(this.state, "province/state"));

      address.setPostalCode(this.zip);
      Arrays.asList(prettify(this.address).split("\\n"))
        .forEach(address::addLine);
      location.setAddress(address);
      builder.setLocation(location);

      // contact info is next
      if (this.website != null && !this.website.trim().isEmpty())
        this.decodeURL(this.website)
          .ifPresent(url -> contact.addWebsite(url.toString()));

      if (this.telephone != null && !this.telephone.trim().isEmpty()) {
        FacilityPhone.Builder phoneNumber = FacilityPhone.newBuilder();
        phoneNumber.setE164(this.telephone);
        phoneNumber.setType(FacilityPhone.PhoneType.MAIN);
        contact.addPhone(phoneNumber);
      }
      builder.setContact(contact);

      // and finally, capabilities
      builder.setCapabilities(specifyCapabilities());
      return builder.build();
    }
  }

  /**
   * Decode a raw facility record from JSON, and return it as its protobuf expression.
   *
   * @param json JSON line to inflate.
   * @return Protobuf record for the facility.
   */
  public static @Nonnull Facility decode(@Nonnull String json) {
    return RawFacility.decode(json).export();
  }
}
