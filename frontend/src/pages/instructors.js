import React from 'https://esm.sh/react@18.3.1';
import { api } from '../api.js';
import { html, formatError, Pill } from '../ui.js';

function buildInstructorList(courses) {
  const map = new Map();
  for (const course of courses) {
    const firstName = (course?.instructorFirstName ?? '').trim();
    const lastName = (course?.instructorLastName ?? '').trim();
    const specialization = (course?.instructorSpecialization ?? '').trim();
    const fullName = `${firstName} ${lastName}`.trim();
    if (!fullName) continue;

    const key = `${fullName}|${specialization}`;
    const existing = map.get(key) ?? {
      fullName,
      specialization,
      coursesCount: 0,
      levels: new Set(),
    };
    existing.coursesCount += 1;
    if (course?.level) existing.levels.add(course.level);
    map.set(key, existing);
  }

  return Array.from(map.values())
    .map((x) => ({ ...x, levels: Array.from(x.levels) }))
    .sort((a, b) => a.fullName.localeCompare(b.fullName, 'ru'));
}

export function InstructorsPage({ toast }) {
  const [items, setItems] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState('');

  const load = React.useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const courses = await api.getAllCourses();
      setItems(buildInstructorList(Array.isArray(courses) ? courses : []));
    } catch (e) {
      setError(formatError(e));
      toast.show('Ошибка', formatError(e));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  React.useEffect(() => {
    load();
  }, [load]);

  return html`<div className="container">
    <div className="hero">
      <h1>Преподаватели</h1>
      <p>Список построен на основе текущих курсов.</p>
    </div>

    <div className="card">
      <div className="card-h">
        <div>
          <h2 className="card-title">Список преподавателей</h2>
          <p className="card-sub">Всего: ${items.length}</p>
        </div>
        <div className="row">
          <a className="btn" href="#/courses">К курсам</a>
          <button className="btn" onClick=${load} disabled=${loading}>Обновить</button>
        </div>
      </div>
      <div className="card-b">
        ${loading ? html`<div className="hint">Загрузка…</div>` : null}
        ${error ? html`<div className="hint"><strong>Ошибка:</strong> ${error}</div>` : null}
        ${!loading && !error && items.length === 0
          ? html`<div className="hint">Пока пусто.</div>`
          : html`<div className="list">
              ${items.map(
                (i) => html`<div className="item" key=${`${i.fullName}-${i.specialization}`}>
                  <div>
                    <h3>${i.fullName}</h3>
                    <p>${i.specialization || 'Без специализации'}</p>
                    <div className="meta">
                      <span className="pill">Курсов: ${i.coursesCount}</span>
                      ${i.levels.map((lvl) => Pill({ children: lvl, strong: true }))}
                    </div>
                  </div>
                </div>`
              )}
            </div>`}
      </div>
    </div>
  </div>`;
}

