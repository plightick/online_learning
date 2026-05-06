export const API_BASE_URL = (() => {
  const fromAppConfig = globalThis?.window?.__APP_CONFIG__?.apiBaseUrl;
  if (typeof fromAppConfig === 'string' && fromAppConfig.trim() !== '') {
    return fromAppConfig.trim();
  }

  const fromRuntime = globalThis?.__RUNTIME_CONFIG__?.API_BASE_URL;
  if (typeof fromRuntime === 'string' && fromRuntime.trim() !== '') {
    return fromRuntime.trim();
  }

  // Можно переопределить через: localStorage.setItem('serverBaseUrl', 'http://localhost:8080')
  const fromStorage = globalThis?.localStorage?.getItem?.('serverBaseUrl');
  return fromStorage || 'http://localhost:8080';
})();

