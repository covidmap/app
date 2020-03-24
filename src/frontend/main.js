
/*global goog*/

goog.module('covidmap.main');

const AppConfig = goog.require('core.AppConfig');
const logging = goog.require('gust.logging');
const {AppAPI} = goog.require('covidmap.api');



goog.scope(function() {
  // noinspection UseOfBracketNotationInspection
  const firebaseConfig = window['__firebase_config'] = {
    'apiKey': AppConfig.getApiKey(),
    'authDomain': AppConfig.getAuthDomain(),
    'databaseURL': AppConfig.getDatabaseUrl(),
    'projectId': AppConfig.getProjectId(),
    'storageBucket': AppConfig.getStorageBucket(),
    'messagingSenderId': AppConfig.getMessagingSenderId(),
    'appId': AppConfig.getAppId(),
    'measurementId': AppConfig.getMeasurementId()
  };

  /**
   * Bootstrap function, starts the COVID Map app.
   */
  function main() {
    console.log('Firebase details: ', firebaseConfig);

    // noinspection UseOfBracketNotationInspection
    window['test'] = function() {
      logging.info('Starting test...');
      const api = AppAPI.acquire();
      const op = api.ping();
      op.then((latency) => {
        logging.info('Test complete.');
        document.getElementById('result').innerText = `Ping completed in ${latency}ms.`;
      }, (err) => {
        console.error('Test failed.', {'err': err});
      });
    };

    setTimeout(() => {
      window['test']();
    }, 1000);
  }

  // mount up our main function
  window.addEventListener('load', main, undefined);
});
