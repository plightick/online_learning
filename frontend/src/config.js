export const API_BASE_URL = (() => {
  // Можно переопределить через: localStorage.setItem('apiBaseUrl', 'http://localhost:8080')
  const fromStorage = globalThis?.localStorage?.getItem?.('apiBaseUrl');
  return fromStorage || 'http://localhost:8080';
})();

