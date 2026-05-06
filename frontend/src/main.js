import React from 'https://esm.sh/react@18.3.1';
import ReactDOM from 'https://esm.sh/react-dom@18.3.1/client';
import { html, useHashRoute, useToast } from './ui.js';
import { CoursesPage } from './pages/courses.js';
import { StudentsPage } from './pages/students.js';
import { InstructorsPage } from './pages/instructors.js';
import { API_BASE_URL } from './config.js';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch() {
    // no-op: message is shown in UI
  }

  render() {
    if (!this.state.error) return this.props.children;
    const msg = this.state.error?.message || String(this.state.error);
    return html`<div className="container">
      <div className="card">
        <div className="card-h">
          <div>
            <h2 className="card-title">Фронт упал</h2>
            <p className="card-sub">Открой DevTools → Console, там будет причина.</p>
          </div>
          <button className="btn btn-primary" onClick=${() => location.reload()}>Перезагрузить</button>
        </div>
        <div className="card-b">
          <div className="hint mono">${msg}</div>
        </div>
      </div>
    </div>`;
  }
}

function Layout({ route, toast }) {
  const nav = [
    { href: '#/courses', label: 'Курсы', key: 'courses' },
    { href: '#/students', label: 'Студенты', key: 'students' },
    { href: '#/instructors', label: 'Преподаватели', key: 'instructors' },
  ];
  const current = route.path.replace(/^\//, '');

  return html`<div>
    <div className="topbar">
      <div className="topbar-inner">
        <a className="brand" href="#/courses">
          <span className="brand-badge" aria-hidden="true"></span>
          <span>
            <span className="brand-title">Онлайн-обучение</span>
            <span className="brand-sub">Учебная платформа</span>
          </span>
        </a>

        <nav className="nav" aria-label="Навигация">
          ${nav.map(
            (n) =>
              html`<a
                key=${n.key}
                className="chip"
                aria-current=${current === n.key ? 'page' : null}
                href=${n.href}
                >${n.label}</a
              >`
          )}
        </nav>
      </div>
    </div>

    ${route.path === '/students'
      ? html`<${StudentsPage} toast=${toast} />`
      : route.path === '/instructors'
        ? html`<${InstructorsPage} toast=${toast} />`
        : html`<${CoursesPage} toast=${toast} />`}
    ${toast.view}
  </div>`;
}

function App() {
  const route = useHashRoute();
  const toast = useToast();
  return html`<${ErrorBoundary}><${Layout} route=${route} toast=${toast} /></${ErrorBoundary}>`;
}

ReactDOM.createRoot(document.getElementById('app')).render(html`<${App} />`);

