import { join } from 'path';

import { SeedConfig } from './seed.config';
import { ExtendPackages } from './seed.config.interfaces';

const proxy = require('proxy-middleware');

/**
 * This class extends the basic seed configuration, allowing for project specific overrides. A few examples can be found
 * below.
 */
export class ProjectConfig extends SeedConfig {

  PROJECT_TASKS_DIR = join(process.cwd(), this.TOOLS_DIR, 'tasks', 'project');
  FONTS_DEST = `${this.APP_DEST}/fonts`;
  FONTS_SRC = [
    'node_modules/font-awesome/fonts/**',
  ];

  constructor() {
    super();

    this.APP_TITLE = 'Stream Juggler';

    /* Enable typeless compiler runs (faster) between typed compiler runs. */
    // this.TYPED_COMPILE_INTERVAL = 5;

    // Add `NPM` third-party libraries to be injected/bundled.
    this.NPM_DEPENDENCIES = [
      ...this.NPM_DEPENDENCIES,
      // CSS
      { src: 'font-awesome/css/font-awesome.min.css', inject: true },
      { src: 'bootstrap/dist/css/bootstrap.css', inject: true },
      // JS
      { src: 'jquery/dist/jquery.min.js', inject: 'libs' },
      { src: 'tether/dist/js/tether.min.js', inject: 'libs' },
      { src: 'bootstrap/dist/js/bootstrap.js', inject: 'libs' },

      // {src: 'lodash/lodash.min.js', inject: 'libs'},
    ];

    // Add `local` third-party libraries to be injected/bundled.
    this.APP_ASSETS = [
      ...this.APP_ASSETS,
      // {src: `${this.APP_SRC}/your-path-to-lib/libs/jquery-ui.js`, inject: true, vendor: false}
      // {src: `${this.CSS_SRC}/path-to-lib/test-lib.css`, inject: true, vendor: false},
    ];

    let additionalPackages: ExtendPackages[] = [];

    additionalPackages.push({
      name: 'ng2-bootstrap',
      path: `${this.APP_BASE}node_modules/ng2-bootstrap/bundles/ng2-bootstrap.umd.js`,
      packageMeta: {
        defaultExtension: 'js'
      }
    }, {
      name: 'moment',
      path: `${this.APP_BASE}node_modules/moment/min/moment-with-locales.js`,
      packageMeta: {
        defaultExtension: 'js'
      }
    });

    this.addPackagesBundles(additionalPackages);

    /* Add to or override NPM module configurations: */
    /* @todo: Fetch API url from environment config */
    this.mergeObject(this.PLUGIN_CONFIGS['browser-sync'], {
      middleware: [
        proxy({
          hostname: '176.120.25.19',
          port: 18080,
          pathname: '/v1',
          route: '/v1'
        }),
        require('connect-history-api-fallback')({ index: `${this.APP_BASE}index.html` })
      ]
    });
  }

}
