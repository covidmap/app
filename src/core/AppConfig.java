package core;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;


/** Holds static credentials in a cross-platform lib. */
@JsType
public final class AppConfig {
  /** API endpoint to use. */
  private static final String apiEndpoint = "https://beta.covidmap.link/v1/";

  /** App container ID to expose. */
  private static final String appContainerId = "appContainer";

  /** Static config values for Firebase. */
  @JsType
  public static final class Firebase {
    private static final String apiKey = "AIzaSyCeY_Zx04BrI3gYvI1_Ai_J7X1zIubP6N4";
    private static final String authDomain = "covid-impact-map.firebaseapp.com";
    private static final String databaseUrl = "https://covid-impact-map.firebaseio.com";
    private static final String projectId = "covid-impact-map";
    private static final String storageBucket = "covid-impact-map.appspot.com";
    private static final String messagingSenderId = "651878046665";
    private static final String appId = "1:651878046665:web:b50041ea8857edabe6e618";
    private static final String measurementId = "G-Q07TZ2VYKG";
  }

  /** @return Firebase API key. */
  public static String getApiKey() {
    return Firebase.apiKey;
  }

  /** @return Firebase auth domain. */
  @JsMethod public static String getAuthDomain() {
    return Firebase.authDomain;
  }

  /** @return Database URL for Firebase Realtime DB. */
  @JsMethod public static String getDatabaseUrl() {
    return Firebase.databaseUrl;
  }

  /** @return Firebase project ID. */
  @JsMethod public static String getProjectId() {
    return Firebase.projectId;
  }

  /** @return Firebase storage bucket. */
  @JsMethod public static String getStorageBucket() {
    return Firebase.storageBucket;
  }

  /** @return Firebase Messaging Sender ID. */
  @JsMethod public static String getMessagingSenderId() {
    return Firebase.messagingSenderId;
  }

  /** @return Firebase App ID (web app). */
  @JsMethod public static String getAppId() {
    return Firebase.appId;
  }

  /** @return Firebase Analytics ID (web app). */
  @JsMethod public static String getMeasurementId() {
    return Firebase.measurementId;
  }

  /** @return API endpoint to make use of. */
  @JsMethod public static String getApiEndpoint() {
    return apiEndpoint;
  }

  /** @return Application container ID. */
  @JsMethod public static String getAppContainerId() {
    return appContainerId;
  }
}
