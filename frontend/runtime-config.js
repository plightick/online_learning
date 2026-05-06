// This file is intended to be overwritten at deploy/runtime (Docker/PaaS).
// Locally it can stay empty and the SPA will fallback to localStorage/defaults.
globalThis.__RUNTIME_CONFIG__ = globalThis.__RUNTIME_CONFIG__ || {};

