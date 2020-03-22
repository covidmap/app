
package server

import javax.inject.Singleton


/**
 * Defines application logic for the COVID Impact Map app. This logic is exposed via gRPC services (like [AppService]),
 * or, alternatively, made use of directly via dependency injection in server-side controllers (i.e. [HomeController]).
 *
 * In this manner, validation logic in the COVID Impact Map app is centralized here. Some aspects of the logic contained
 * herein are packaged separately to allow sharing that logic with the frontend, which must perform a subset of the
 * tasks incumbent on the server (for instance, basic validation of data before submission to the service, which is
 * performed in duplicate on the server-side).
 */
@Singleton
class CovidmapLogic {
}
