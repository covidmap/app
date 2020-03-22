
/*global goog*/

goog.module('covidmap.main');

const logging = goog.require('gust.logging');
const {AppAPI} = goog.require('covidmap.api');


goog.scope(function() {
  /**
   * Bootstrap function, starts the COVID Map app.
   */
  function main() {
    // affix test function
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
