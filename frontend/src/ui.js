import React from 'https://esm.sh/react@18.3.1';
import htm from 'https://esm.sh/htm@3.1.1';

export const html = htm.bind(React.createElement);

export function useHashRoute() {
  const get = () => (globalThis.location.hash || '#/courses').replace(/^#/, '');
  const [route, setRoute] = React.useState(get);

  React.useEffect(() => {
    const onHash = () => setRoute(get());
    globalThis.addEventListener('hashchange', onHash);
    if (!globalThis.location.hash) globalThis.location.hash = '#/courses';
    return () => globalThis.removeEventListener('hashchange', onHash);
  }, []);

  const [path, queryString = ''] = route.split('?');
  const query = Object.fromEntries(new URLSearchParams(queryString));
  return { path, query };
}

export function formatError(e) {
  if (!e) return 'Неизвестная ошибка';
  if (typeof e === 'string') return e;
  if (e.url) return `${e.message} (URL: ${e.url})`;
  return e.message || 'Ошибка запроса';
}

export function useToast() {
  const [toast, setToast] = React.useState(null);

  const show = React.useCallback((title, details) => {
    setToast({ title, details, id: crypto.randomUUID() });
  }, []);

  React.useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3800);
    return () => clearTimeout(t);
  }, [toast]);

  const view = toast
    ? html`<div className="toast" role="status" aria-live="polite">
        <div><strong>${toast.title}</strong></div>
        ${toast.details ? html`<small>${toast.details}</small>` : null}
      </div>`
    : null;

  return { show, view };
}

export function Pill({ children, strong }) {
  return html`<span className=${strong ? 'pill pill-strong' : 'pill'}>${children}</span>`;
}

function FieldLabel({ label, required }) {
  return html`<span className="label">
    ${label}${required ? html`<span className="req" aria-hidden="true">*</span>` : null}
  </span>`;
}

export function TextField({ label, value, onChange, placeholder, hint, type = 'text', required = false }) {
  return html`<label className="field">
    <${FieldLabel} label=${label} required=${required} />
    <input
      className="input"
      type=${type}
      value=${value ?? ''}
      required=${required || undefined}
      placeholder=${placeholder ?? ''}
      onInput=${(e) => onChange(e.target.value)}
    />
    ${hint ? html`<span className="hint">${hint}</span>` : null}
  </label>`;
}

export function SelectField({ label, value, onChange, options, hint, required = false }) {
  return html`<label className="field">
    <${FieldLabel} label=${label} required=${required} />
    <select
      className="select"
      value=${value ?? ''}
      required=${required || undefined}
      onChange=${(e) => onChange(e.target.value)}
    >
      ${options.map(
        (o) => html`<option key=${o.value} value=${o.value}>${o.label}</option>`
      )}
    </select>
    ${hint ? html`<span className="hint">${hint}</span>` : null}
  </label>`;
}

export function NumberField({ label, value, onChange, placeholder, hint, min, required = false }) {
  return html`<label className="field">
    <${FieldLabel} label=${label} required=${required} />
    <input
      className="input"
      type="number"
      value=${value ?? ''}
      min=${min ?? undefined}
      required=${required || undefined}
      placeholder=${placeholder ?? ''}
      onInput=${(e) => onChange(e.target.value === '' ? '' : Number(e.target.value))}
    />
    ${hint ? html`<span className="hint">${hint}</span>` : null}
  </label>`;
}

