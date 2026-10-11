import { bootstrapApplication } from '@angular/platform-browser';

import { App } from './app/app';
import { appConfig } from './app/app.config';
import { APP_CONFIG, loadAppConfig } from './app/core/config/app-config';

loadAppConfig()
  .then((config) =>
    bootstrapApplication(App, {
      ...appConfig,
      providers: [...appConfig.providers, { provide: APP_CONFIG, useValue: config }],
    }),
  )
  .catch((error) => {
    console.error(error);
    const message = document.createElement('p');
    message.setAttribute('role', 'alert');
    message.style.padding = 'var(--mu-space-6)';
    message.textContent =
      'Het beheerportaal kon niet starten omdat de configuratie ontbreekt. Neem contact op met de beheerder.';
    document.body.replaceChildren(message);
  });
