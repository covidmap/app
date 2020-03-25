
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
   * Pull init data from the DOM at this ID.
   *
   * @type {!string}
   * @const
   */
  const dataElement = 'page-data_';

  // noinspection UseOfBracketNotationInspection
  /**
   * @type {!function(!string, *): void}
   * @const
   */
  const appBoot = /** @type {!function(?string): void} */ (window['__boot']);

  let mapsLoaded = false;

  // noinspection UseOfBracketNotationInspection
  /**
   * @suppress {checkDebuggerStatement}
   * @private
   */
  window['__init_map'] = function() {
    document.addEventListener('DOMContentLoaded',() => {
      if (!mapsLoaded) {
        mapsLoaded = true;
        logging.info('Initializing Google Maps.');

        // noinspection UseOfBracketNotationInspection
        window['dispatcher']['dispatch'](window['DISPATCHER_MESSAGES']['MapReady'], true);
        // noinspection UseOfBracketNotationInspection
        window['dispatcher']['dispatch'](window['DISPATCHER_MESSAGES']['SetLoadingFalse'], true);
      }
    });
  };

  /**
   * Bootstrap function, starts the COVID Map app.
   *
   * @suppress {checkDebuggerStatement,reportUnknownTypes}
   */
  function main() {
    const appContainer = /** @type {!string} */ (AppConfig.getAppContainerId());
    const dataContainer = document.getElementById(dataElement);
    let initData = {};
    if (!!dataContainer) {
      try {
        initData = JSON.parse(dataContainer.innerText);
      } catch (err) {
        logging.error('Failed to initialize page data.', {err});
        initData = {};
      }
    }
    logging.info('Booting COVID Impact Map...', {'container': appContainer, 'data': initData});
    appBoot(appContainer, initData);

    // noinspection UseOfBracketNotationInspection
    function rpcTest() {
      logging.info('Starting test...');
      const api = AppAPI.acquire();
      const op = api.ping();
      op.then((latency) => {
        logging.info('Test complete.');
        document.getElementById('result').innerText = `Ping completed in ${latency}ms.`;
      }, (err) => {
        console.error('Test failed.', {'err': err});
      });
    }

    if (window.location.origin.indexOf('localhost') === -1) {
      setTimeout(() => {
        // noinspection UseOfBracketNotationInspection
        rpcTest();
      }, 1000);
    }
  }

  // mount up our main function
  document.addEventListener('DOMContentLoaded', main, undefined);
});
