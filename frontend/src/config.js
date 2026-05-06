export const API_BASE_URL = (() => {
  // Можно переопределить через: localStorage.setItem('serverBaseUrl', 'http://localhost:8080')
  const fromStorage = globalThis?.localStorage?.getItem?.('serverBaseUrl');
  return fromStorage || 'http://localhost:8080';
})();

