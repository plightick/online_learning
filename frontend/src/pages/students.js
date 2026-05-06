import React from 'https://esm.sh/react@18.3.1';
import { api } from '../api.js';
import { html, formatError, TextField } from '../ui.js';

function StudentForm({ initial, onCancel, onSave, busy }) {
  const [firstName, setFirstName] = React.useState(initial?.firstName ?? '');
  const [lastName, setLastName] = React.useState(initial?.lastName ?? '');
  const [email, setEmail] = React.useState(initial?.email ?? '');

  const canSave = firstName.trim() && lastName.trim() && email.trim();

  return html`<div className="card">
    <div className="card-h">
      <div>
        <h2 className="card-title">${initial ? 'Редактировать студента' : 'Новый студент'}</h2>
        <p className="card-sub">Заполни данные студента и сохрани изменения.</p>
      </div>
      <div className="row">
        <button className="btn" onClick=${onCancel} disabled=${busy}>Отмена</button>
        <button className="btn btn-primary" disabled=${busy || !canSave} onClick=${() =>
          onSave({ firstName, lastName, email })}>
          ${busy ? 'Сохранение…' : 'Сохранить'}
        </button>
      </div>
    </div>
    <div className="card-b">
      <div className="row">
        ${TextField({ label: 'Имя', value: firstName, onChange: setFirstName, placeholder: 'Alice' })}
        ${TextField({ label: 'Фамилия', value: lastName, onChange: setLastName, placeholder: 'Smith' })}
        ${TextField({ label: 'Эл. почта', value: email, onChange: setEmail, placeholder: 'student@example.com', type: 'email' })}
      </div>
    </div>
  </div>`;
}

export function StudentsPage({ toast }) {
  const [items, setItems] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState('');
  const [mode, setMode] = React.useState('list'); // list | create | edit
  const [editing, setEditing] = React.useState(null);
  const [busy, setBusy] = React.useState(false);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getStudents();
      setItems(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(formatError(e));
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    load();
  }, [load]);

  async function onCreate(dto) {
    setBusy(true);
    try {
      await api.createStudent(dto);
      toast.show('Студент создан');
      setMode('list');
      await load();
    } catch (e) {
      toast.show('Ошибка', formatError(e));
    } finally {
      setBusy(false);
    }
  }

  async function onUpdate(dto) {
    setBusy(true);
    try {
      await api.updateStudent(editing.id, dto);
      toast.show('Сохранено');
      setMode('list');
      setEditing(null);
      await load();
    } catch (e) {
      toast.show('Ошибка', formatError(e));
    } finally {
      setBusy(false);
    }
  }

  async function onDelete(id) {
    if (!confirm('Удалить студента?')) return;
    setBusy(true);
    try {
      await api.deleteStudent(id);
      toast.show('Удалено');
      await load();
    } catch (e) {
      toast.show('Ошибка', formatError(e));
    } finally {
      setBusy(false);
    }
  }

  if (mode === 'create') {
    return html`<div className="container">
      <${StudentForm} initial=${null} onCancel=${() => setMode('list')} onSave=${onCreate} busy=${busy} />
    </div>`;
  }

  if (mode === 'edit') {
    return html`<div className="container">
      <${StudentForm}
        initial=${editing}
        onCancel=${() => (setMode('list'), setEditing(null))}
        onSave=${onUpdate}
        busy=${busy}
      />
    </div>`;
  }

  return html`<div className="container">
    <div className="hero">
      <h1>Студенты</h1>
      <p>Список студентов.</p>
    </div>

    <div className="section">
      <div className="card">
      <div className="card-h">
        <div>
          <h2 className="card-title">Список</h2>
          <p className="card-sub">Всего: ${items.length}</p>
        </div>
        <div className="row">
          <a className="btn" href="#/courses">К курсам</a>
          <button className="btn" onClick=${load} disabled=${loading || busy}>Обновить</button>
          <button className="btn btn-primary" onClick=${() => setMode('create')} disabled=${busy}>Добавить</button>
        </div>
      </div>
      <div className="card-b">
        ${loading ? html`<div className="hint">Загрузка…</div>` : null}
        ${error ? html`<div className="hint"><strong>Ошибка:</strong> ${error}</div>` : null}
        ${!loading && !error && items.length === 0
          ? html`<div className="hint">Пока пусто.</div>`
          : html`<div className="list">
              ${items.map(
                (s) => html`<div className="item" key=${s.id}>
                  <div>
                    <h3>${s.firstName} ${s.lastName}</h3>
                    <p className="mono">${s.email}</p>
                  </div>
                  <div className="row">
                    <button className="btn" onClick=${() => (setEditing(s), setMode('edit'))} disabled=${busy}>
                      Редактировать
                    </button>
                    <button className="btn btn-danger" onClick=${() => onDelete(s.id)} disabled=${busy}>
                      Удалить
                    </button>
                  </div>
                </div>`
              )}
            </div>`}
      </div>
    </div>
    </div>
  </div>`;
}

