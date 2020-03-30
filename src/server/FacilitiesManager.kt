
package server

import ch.hsr.geohash.GeoHash
import com.google.common.collect.Sets
import com.google.type.LatLng
import covidmap.schema.Facility
import gust.backend.runtime.Logging
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.Reader
import java.util.*
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.zip.GZIPInputStream


/** Object which manages the local set of facilities in the COVAID/COVID Impact Map dataset. */
@Suppress("MemberVisibilityCanBePrivate", "RemoveExplicitTypeArguments", "unused")
class FacilitiesManager private constructor (
  /** Full set of facility data records. */
  private val data: Collection<Facility>,

  /** Index of facility keys/IDs to the records which they reference. */
  private val keyIndex: ConcurrentMap<String, Facility>,

  /** Index of geohash substrings to the facilities contained by their respective bounding boxes. */
  private val geohashIndex: ConcurrentMap<String, SortedSet<FacilityRecord>>) {

  /** Hashable data object which considers two facilities with the same ID equal. */
  data class FacilityRecord (
    /** ID to use for this record. */
    val id: String,

    /** Facility record wrapped  by this object. */
    val facility: Facility): Comparable<FacilityRecord> {
    companion object {
      /** Static factory to wrap a facility record. */
      fun wrap(facility: Facility): FacilityRecord = FacilityRecord(
        id = facility.key.id,
        facility = facility)
    }

    /** Delegate to comparison of record IDs. */
    override fun equals(other: Any?): Boolean = this.id == (other as? FacilityRecord)?.id

    /** Delegate to hashing this item's ID. */
    override fun hashCode(): Int = id.hashCode()

    /** Avoid emitting the printed facility proto value. */
    override fun toString(): String = "Facility{id=$id}"

    /** Delegate comparison to the record's ID. */
    override fun compareTo(other: FacilityRecord): Int = id.compareTo(other.id)
  }

  companion object {
    /** Path to our downloaded facilities data, in JSON-ND. */
    private const val filepath = "/external/facilities/file/downloaded"

    /** Minimum length of a geo-hash substring. */
    const val minimumGeohashLength = 2

    /** Precision of the geohash calculations we make. */
    const val geohashCharacterSize = 12

    /** Private logging pipe. */
    private val logging = Logging.logger(FacilitiesManager::class.java)

    private val instance = acquire()

    private fun acquire(): FacilitiesManager {
      try {
        val stream = FacilitiesManager::class.java.getResourceAsStream(filepath)
        if (stream == null) {
          logging.error("Failed to locate facilities data file.")
          throw FileNotFoundException("Facilities data file not found: '$filepath'.")
        }

        BufferedReader(InputStreamReader(GZIPInputStream(stream)))
          .use { buffer ->
            logging.info("Loading facilities database...")
            return parse(buffer)
          }
      } catch (exc: Exception) {
        logging.error("Encountered fatal error loading embedded facilities dataset.")
        throw exc
      }
    }

    /** Parse JSON data from the provided buffer, and initialize a [FacilitiesManager] with it. */
    private fun parse(buffer: Reader): FacilitiesManager {
      val lines = buffer.readLines()
      logging.info("Parsing ${lines.size} facility records...")

      val facilities = lines
        .parallelStream()
        .map(this::parseLine)
        .filter { facility -> facility.open }
        .map { FacilityRecord.wrap(it) }
        .collect(Collectors.toList())

      logging.info("Loaded ${facilities.size} facility records after filtering. Indexing...")

      // prep a threadsafe geohash index
      val geohashIndex: ConcurrentSkipListMap<String, SortedSet<FacilityRecord>> = facilities
        .parallelStream()
        .map(this::geohashSubstringsForRecord)
        .flatMap { pair ->
          // emit a stream, which maps each facility record to a given substring entry
          pair.second.parallelStream().map { it to pair.first }
        }
        .collect(Collectors.toConcurrentMap(
          { it.first },
          { sortedSetOf(it.second) },
          { left, right -> TreeSet(Sets.union(left, right).immutableCopy()) },
          { ConcurrentSkipListMap<String, SortedSet<FacilityRecord>>() }
        ))

      // build a key index too
      val keyIndex: ConcurrentSkipListMap<String, Facility> = facilities
        .parallelStream()
        .collect(Collectors.toConcurrentMap(
          { it.id },
          { it.facility },
          { left, _ -> throw IllegalStateException("Cannot have to records that use the same key: '${left.key.id}'.") },
          { ConcurrentSkipListMap<String, Facility>() }
        ))

      if (geohashIndex.isEmpty()) {
        logging.error("Geohash or key index is empty. Computation may have failed, or data may have not loaded.")
        throw IllegalStateException("Geohash or key index failed to compute.")
      } else {
        logging.info("Generated indexes for facility data "
          + "(keys: ${keyIndex.size} entries, geohashes: ${geohashIndex.size} entries).")
      }

      // we're ready to mount and query
      return FacilitiesManager(
        data = facilities.map { it.facility },
        keyIndex = keyIndex,
        geohashIndex = geohashIndex
      )
    }

    /** Load all embedded facility data and return an instance of the [FacilitiesManager]. */
    fun load(): FacilitiesManager = instance

    /** Return a parsed [Facility] instance from the provided data line. */
    private fun parseLine(line: String): Facility = FacilityDecoder.decode(line)

    /** Given a facility record, return the record paired with the computed set of geohash substrings. */
    private fun geohashSubstringsForRecord(record: FacilityRecord): Pair<FacilityRecord, SortedSet<String>> {
      return record to geohashSubstrings(record.facility.location.hash)
        .collect(Collectors.toCollection { ConcurrentSkipListSet<String>() })
    }

    /** Given a generic geohash, return the set of computed substrings. */
    private fun geohashSubstrings(geohash: String): Stream<String> {
      return try {
        val total = geohash.length
        (minimumGeohashLength until total).toList()
          .parallelStream()
          .map { geohash.subSequence(0 .. it).toString() }

      } catch (exc: Exception) {
        logging.error("An error occurred generating geohash substrings: '${exc.message}'.")
        Stream.empty()
      }
    }
  }

  /** Resolve a single facility by its ID. */
  fun resolve(id: String): Facility? = keyIndex[id]

  /** Retrieve a non-parallel stream of all [Facility] records. */
  fun stream(): Stream<Facility> = data.stream()

  /** Retrieve a non-parallel stream of all [Facility] records. */
  fun parallelStream(): Stream<Facility> = data.parallelStream()

  /** Return a stream of [Facility] records nearest the specified encoded geohash. */
  fun nearby(geohash: String): Stream<Facility> {
    logging.debug("Querying nearby facilities for hash '$geohash'...")
    val resultset = geohashSubstrings(geohash)
      .map { geohashIndex.getOrDefault(it, Collections.emptySortedSet()) }
      .reduce(
        ConcurrentSkipListSet<FacilityRecord>()
      ) { resultset, batch ->
        resultset.addAll(batch)
        resultset
      }

    logging.debug("Returning ${resultset.size} facilities.")
    return resultset.parallelStream().map { it.facility }
  }

  /** Return a stream of [Facility] records nearest the specified encoded geo-point. */
  fun nearby(point: LatLng): Stream<Facility> = nearby(GeoHash
    .withCharacterPrecision(point.latitude, point.longitude, geohashCharacterSize)
    .toBase32())
}
