
package server

import com.google.gson.Gson
import covidmap.schema.Facility
import gust.backend.runtime.Logging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.lang.RuntimeException
import java.util.stream.Collectors


/** Object which manages the local set of facilities in the COVAID/COVID Impact Map dataset. */
class FacilitiesManager private constructor (val data: Collection<Facility>) {
  companion object {
    /** Path to our downloaded facilities data, in JSON-ND. */
    private const val filepath = "external/facilities/file/downloaded"

    /** Private logging pipe. */
    private val logging = Logging.logger(FacilitiesManager::class.java)

    /** GSON instance to use for decoding. */
    private val gson = acquireGSON()

    /** Setup GSON for use with our dataset. */
    private fun acquireGSON(): Gson = Gson()

    /** Load all embedded facility data and return an instance of the [FacilitiesManager]. */
    fun load(): FacilitiesManager {
      try {
        BufferedReader(InputStreamReader(FacilitiesManager::class.java.getResourceAsStream(filepath)))
          .use { buffer ->
          logging.info("Loading facilities database...")
          return parse(buffer)
        }
      } catch (exc: Exception) {
        logging.error("Encountered fatal error loading embedded facilities dataset.")
        throw RuntimeException(exc)
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
        .collect(Collectors.toList())

      logging.info("Loaded ${facilities.size} facility records after filtering...")
      TODO("not yet implemented")
    }

    /** Return a parsed [Facility] instance from the provided data line. */
    private fun parseLine(line: String): Facility {
      TODO("not yet implemented")
    }
  }
}
