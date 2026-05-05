import React from 'https://esm.sh/react@18.3.1';
import { api } from '../api.js';
import { html, formatError, Pill, TextField, SelectField, NumberField } from '../ui.js';

const LEVELS = [
  { value: '', label: 'Все уровни' },
  { value: 'Beginner', label: 'Начальный' },
  { value: 'Intermediate', label: 'Средний' },
  { value: 'Advanced', label: 'Продвинутый' },
];

const SORT_OPTIONS = [
  { value: 'id', label: 'ID' },
  { value: 'title', label: 'Название' },
  { value: 'level', label: 'Уровень' },
];
const PAGE_SIZE = 5;

function parseCsv(value) {
  return value
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

function normalizeLessons(lessons) {
  if (!Array.isArray(lessons)) return [];
  const seen = new Set();
  const normalized = [];
  for (const l of lessons) {
    const item = {
      id: l?.id,
      title: l?.title ?? '',
      durationMinutes: Number(l?.durationMinutes ?? 10),
      lessonOrder: Number(l?.lessonOrder ?? 1),
    };
    const key = item.id != null ? `id:${item.id}` : `${item.lessonOrder}|${item.title}|${item.durationMinutes}`;
    if (seen.has(key)) continue;
    seen.add(key);
    normalized.push(item);
  }
  return normalized.sort((a, b) => a.lessonOrder - b.lessonOrder);
}

function CourseForm({ initial, students, onCancel, onSave, busy }) {
  const [title, setTitle] = React.useState(initial?.title ?? '');
  const [level, setLevel] = React.useState(initial?.level ?? 'Beginner');
  const [instructorFirstName, setInstructorFirstName] = React.useState(initial?.instructorFirstName ?? '');
  const [instructorLastName, setInstructorLastName] = React.useState(initial?.instructorLastName ?? '');
  const [instructorSpecialization, setInstructorSpecialization] = React.useState(
    initial?.instructorSpecialization ?? ''
  );

  // Lessons (OneToMany): [{title,durationMinutes,lessonOrder}]
  const [lessons, setLessons] = React.useState(
    normalizeLessons(initial?.lessons ?? [{ title: 'Введение', durationMinutes: 20, lessonOrder: 1 }]).map((l, idx) => ({
      title: l.title ?? '',
      durationMinutes: l.durationMinutes ?? 10,
      lessonOrder: l.lessonOrder ?? idx + 1,
    }))
  );

  // Students (ManyToMany) by IDs
  const [studentIds, setStudentIds] = React.useState(initial?.studentIds ?? []);

  // Categories (ManyToMany) by names
  const [categoryNamesRaw, setCategoryNamesRaw] = React.useState((initial?.categoryNames ?? []).join(', '));

  const canSave =
    title.trim() &&
    level.trim() &&
    instructorFirstName.trim() &&
    instructorLastName.trim() &&
    instructorSpecialization.trim() &&
    lessons.length > 0 &&
    lessons.every((l) => l.title.trim() && Number(l.durationMinutes) > 0 && Number(l.lessonOrder) > 0);

  function updateLesson(i, patch) {
    setLessons((prev) => prev.map((l, idx) => (idx === i ? { ...l, ...patch } : l)));
  }

  function addLesson() {
    setLessons((prev) => [
      ...prev,
      { title: '', durationMinutes: 10, lessonOrder: prev.length ? Math.max(...prev.map((x) => x.lessonOrder)) + 1 : 1 },
    ]);
  }

  function removeLesson(i) {
    setLessons((prev) => prev.filter((_, idx) => idx !== i));
  }

  function toggleStudent(id) {
    setStudentIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  }

  function submit() {
    const dto = {
      title: title.trim(),
      level: level.trim(),
      instructorFirstName: instructorFirstName.trim(),
      instructorLastName: instructorLastName.trim(),
      instructorSpecialization: instructorSpecialization.trim(),
      lessons: lessons.map((l) => ({
        title: l.title.trim(),
        durationMinutes: Number(l.durationMinutes),
        lessonOrder: Number(l.lessonOrder),
      })),
      studentIds,
      categoryNames: parseCsv(categoryNamesRaw),
    };
    onSave(dto);
  }

  return html`<div className="card">
    <div className="card-h">
      <div>
        <h2 className="card-title">${initial ? `Редактировать курс #${initial.id}` : 'Новый курс'}</h2>
        <p className="card-sub">Заполни данные курса и сохрани изменения.</p>
      </div>
      <div className="row">
        <button className="btn" onClick=${onCancel} disabled=${busy}>Отмена</button>
        <button className="btn btn-primary" onClick=${submit} disabled=${busy || !canSave}>
          ${busy ? 'Сохранение…' : 'Сохранить'}
        </button>
      </div>
    </div>

    <div className="card-b">
      <div className="row">
        ${TextField({ label: 'Название', value: title, onChange: setTitle, placeholder: 'Основы Java' })}
        ${SelectField({ label: 'Уровень', value: level, onChange: setLevel, options: LEVELS.filter((x) => x.value) })}
      </div>

      <div style=${{ height: '10px' }}></div>

      <div className="row">
        ${TextField({ label: 'Инструктор: имя', value: instructorFirstName, onChange: setInstructorFirstName, placeholder: 'Иван' })}
        ${TextField({ label: 'Инструктор: фамилия', value: instructorLastName, onChange: setInstructorLastName, placeholder: 'Иванов' })}
        ${TextField({
          label: 'Специализация',
          value: instructorSpecialization,
          onChange: setInstructorSpecialization,
          placeholder: 'Разработка серверной части',
        })}
      </div>

      <div style=${{ height: '14px' }}></div>

      <div className="row-between">
        <div>
          <div className="label">Уроки</div>
        </div>
        <button className="btn" onClick=${addLesson} disabled=${busy}>+ Урок</button>
      </div>

      <div className="list" style=${{ marginTop: '10px' }}>
        ${lessons.map(
          (l, i) => html`<div className="item" key=${i}>
            <div style=${{ flex: 1 }}>
              <div className="row">
                ${TextField({ label: 'Название урока', value: l.title, onChange: (v) => updateLesson(i, { title: v }), placeholder: 'Введение' })}
                ${NumberField({ label: 'Минут', value: l.durationMinutes, onChange: (v) => updateLesson(i, { durationMinutes: v || 0 }), min: 1 })}
                ${NumberField({ label: 'Порядок', value: l.lessonOrder, onChange: (v) => updateLesson(i, { lessonOrder: v || 1 }), min: 1 })}
              </div>
            </div>
            <div className="row">
              <button className="btn btn-danger" onClick=${() => removeLesson(i)} disabled=${busy || lessons.length <= 1}>
                Удалить
              </button>
            </div>
          </div>`
        )}
      </div>

      <div style=${{ height: '14px' }}></div>

      <div className="label">Студенты</div>
      <div className="meta" style=${{ marginTop: '10px' }}>
        ${students.length === 0
          ? html`<span className="hint">Нет студентов. Сначала добавь на вкладке “Студенты”.</span>`
          : students.map((s) => {
              const id = s.id;
              const checked = studentIds.includes(id);
              return html`<button
                key=${id}
                className=${checked ? 'chip' : 'chip'}
                style=${checked
                  ? { borderColor: 'rgba(45,108,255,.35)', background: 'rgba(45,108,255,.08)', color: 'var(--primary-2)' }
                  : null}
                type="button"
                onClick=${() => toggleStudent(id)}
                disabled=${busy}
              >
                ${checked ? '✓' : '+'} ${s.firstName} ${s.lastName} (#${id})
              </button>`;
            })}
      </div>

      <div style=${{ height: '14px' }}></div>

      <div className="label">Категории</div>
      <div className="row" style=${{ marginTop: '6px' }}>
        ${TextField({
          label: 'Категории',
          value: categoryNamesRaw,
          onChange: setCategoryNamesRaw,
          placeholder: 'Программирование, Java',
          hint: 'Напр.: Программирование, Java, Базы данных',
        })}
      </div>
    </div>
  </div>`;
}

function CourseDetails({ course, onEdit }) {
  if (!course) return null;
  return html`<div className="card">
    <div className="card-h">
      <div>
        <h2 className="card-title">Детали курса</h2>
        <p className="card-sub">Основная информация и связанные данные курса.</p>
      </div>
      <button className="btn" onClick=${onEdit}>Редактировать</button>
    </div>
    <div className="card-b">
      <div className="kvs">
        <div className="k">ID</div>
        <div className="v mono">${course.id}</div>
        <div className="k">Название</div>
        <div className="v">${course.title}</div>
        <div className="k">Уровень</div>
        <div className="v">${Pill({ children: course.level, strong: true })}</div>
        <div className="k">Инструктор</div>
        <div className="v">${course.instructorFirstName} ${course.instructorLastName}</div>
        <div className="k">Уроки</div>
        <div className="v">
          ${normalizeLessons(course.lessons).length
            ? html`<div className="meta">
                ${normalizeLessons(course.lessons).map((l) => html`<span className="pill" key=${l.id ?? `${l.title}-${l.lessonOrder}`}>
                  ${l.lessonOrder}. ${l.title} (${l.durationMinutes} мин)
                </span>`)}
              </div>`
            : html`<span className="hint">нет</span>`}
        </div>
        <div className="k">Студенты</div>
        <div className="v">
          ${Array.isArray(course.studentNames) && course.studentNames.length
            ? html`<div className="meta">
                ${course.studentNames.map((n) => html`<span className="pill" key=${n}>${n}</span>`)}
              </div>`
            : html`<span className="hint">нет</span>`}
        </div>
        <div className="k">Категории</div>
        <div className="v">
          ${Array.isArray(course.categoryNames) && course.categoryNames.length
            ? html`<div className="meta">
                ${course.categoryNames.map((n) => html`<span className="pill" key=${n}>${n}</span>`)}
              </div>`
            : html`<span className="hint">нет</span>`}
        </div>
      </div>
    </div>
  </div>`;
}

export function CoursesPage({ toast }) {
  const [students, setStudents] = React.useState([]);
  const [pageData, setPageData] = React.useState(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState('');

  const [filters, setFilters] = React.useState({
    mode: 'list', // list | apiSearch | localSearch
    level: '',
    page: 1,
    sortBy: 'id',
    ascending: true,
    categoryName: '',
    instructorSpecialization: '',
    queryType: 'JPQL',
    search: '',
  });

  const [selected, setSelected] = React.useState(null);
  const [details, setDetails] = React.useState(null);

  const [formMode, setFormMode] = React.useState('none'); // none | create | edit
  const [busy, setBusy] = React.useState(false);
  const [reloadKey, setReloadKey] = React.useState(0);

  const loadStudents = React.useCallback(async () => {
    try {
      const list = await api.getStudents();
      setStudents(Array.isArray(list) ? list : []);
    } catch {
      setStudents([]);
    }
  }, []);

  const loadList = React.useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data =
        filters.mode === 'apiSearch'
          ? await api.searchCourses({
              categoryName: filters.categoryName,
              instructorSpecialization: filters.instructorSpecialization,
              queryType: filters.queryType,
              page: filters.page,
              size: PAGE_SIZE,
            })
          : filters.mode === 'localSearch' || filters.mode === 'list'
            ? await api.getAllCourses()
            : await api.getAllCourses();
      setPageData(data);
    } catch (e) {
      setError(formatError(e));
      setPageData(null);
    } finally {
      setLoading(false);
    }
  }, [
    filters.mode,
    filters.level,
    filters.page,
    filters.sortBy,
    filters.ascending,
    filters.categoryName,
    filters.instructorSpecialization,
    filters.queryType,
    reloadKey,
  ]);

  React.useEffect(() => {
    loadStudents();
  }, [loadStudents]);

  React.useEffect(() => {
    loadList();
  }, [loadList]);

  async function openDetails(item) {
    setSelected(item);
    setDetails(null);
    try {
      const d = await api.getCourse(item.id);
      setDetails(d);
    } catch (e) {
      toast.show('Ошибка', formatError(e));
    }
  }

  async function onCreate(dto) {
    setBusy(true);
    try {
      const created = await api.createCourse(dto);
      toast.show('Курс создан', `ID: ${created?.id ?? '—'}`);
      setFormMode('none');
      setFilters((prev) => ({ ...prev, mode: 'list', page: 1, search: '' }));
      setReloadKey((x) => x + 1);
    } catch (e) {
      toast.show('Ошибка', formatError(e));
    } finally {
      setBusy(false);
    }
  }

  async function onUpdate(dto) {
    setBusy(true);
    try {
      await api.updateCourse(details.id, dto);
      toast.show('Сохранено');
      setFormMode('none');
      setFilters((prev) => ({ ...prev, page: 1 }));
      setReloadKey((x) => x + 1);
      const d = await api.getCourse(details.id);
      setDetails(d);
    } catch (e) {
      toast.show('Ошибка', formatError(e));
    } finally {
      setBusy(false);
    }
  }

  async function onDelete(id) {
    if (!confirm(`Удалить курс #${id}?`)) return;
    setBusy(true);
    try {
      await api.deleteCourse(id);
      toast.show('Удалено');
      setSelected(null);
      setDetails(null);
      setFilters((prev) => ({ ...prev, page: 1 }));
      setReloadKey((x) => x + 1);
    } catch (e) {
      toast.show('Ошибка', formatError(e));
    } finally {
      setBusy(false);
    }
  }

  const sourceItems = Array.isArray(pageData?.content) ? pageData.content : Array.isArray(pageData) ? pageData : [];
  const normalizedSearch = filters.search.trim().toLowerCase();
  const itemsAfterModeFilters =
    filters.mode === 'apiSearch'
      ? sourceItems
      : sourceItems
          .filter((course) => {
            if (filters.mode === 'list' && filters.level && course?.level !== filters.level) return false;
            if (filters.mode === 'localSearch' && normalizedSearch) {
              const title = (course?.title ?? '').toLowerCase();
              const instructor = `${course?.instructorFirstName ?? ''} ${course?.instructorLastName ?? ''}`.toLowerCase();
              const level = (course?.level ?? '').toLowerCase();
              const categories = Array.isArray(course?.categoryNames) ? course.categoryNames.join(' ').toLowerCase() : '';
              return (
                title.includes(normalizedSearch) ||
                instructor.includes(normalizedSearch) ||
                level.includes(normalizedSearch) ||
                categories.includes(normalizedSearch)
              );
            }
            return true;
          })
          .slice()
          .sort((a, b) => {
            if (filters.mode !== 'list') return 0;
            const av = a?.[filters.sortBy];
            const bv = b?.[filters.sortBy];
            if (av == null && bv == null) return 0;
            if (av == null) return 1;
            if (bv == null) return -1;
            if (typeof av === 'number' && typeof bv === 'number') {
              return filters.ascending ? av - bv : bv - av;
            }
            const cmp = String(av).localeCompare(String(bv), 'ru');
            return filters.ascending ? cmp : -cmp;
          });

  const isPagedResponse = Boolean(pageData && !Array.isArray(pageData));
  const serverPage = isPagedResponse && pageData?.number !== undefined ? pageData.number + 1 : 1;
  const serverTotalElements = isPagedResponse ? Number(pageData?.totalElements ?? itemsAfterModeFilters.length) : itemsAfterModeFilters.length;
  const serverTotalPages = isPagedResponse ? Number(pageData?.totalPages ?? 1) : 1;
  const shouldUseClientPagination =
    filters.mode !== 'apiSearch' ||
    !isPagedResponse ||
    (serverTotalPages <= 1 && itemsAfterModeFilters.length > PAGE_SIZE);

  const totalElements = shouldUseClientPagination ? itemsAfterModeFilters.length : serverTotalElements;
  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));
  const safePage = shouldUseClientPagination ? Math.min(filters.page, totalPages) : Math.min(serverPage, totalPages);
  const items = shouldUseClientPagination
    ? itemsAfterModeFilters.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE)
    : itemsAfterModeFilters;
  const page = safePage;

  const filtersView =
    filters.mode === 'apiSearch'
      ? html`<div className="filters-grid">
          ${TextField({
            label: 'Категория',
            value: filters.categoryName,
            onChange: (v) => setFilters((p) => ({ ...p, categoryName: v, page: 1 })),
            placeholder: 'Например: Backend',
          })}
          ${TextField({
            label: 'Специализация преподавателя',
            value: filters.instructorSpecialization,
            onChange: (v) => setFilters((p) => ({ ...p, instructorSpecialization: v, page: 1 })),
            placeholder: 'Например: Security',
          })}
          ${SelectField({
            label: 'Тип запроса',
            value: filters.queryType,
            onChange: (v) => setFilters((p) => ({ ...p, queryType: v, page: 1 })),
            options: [
              { value: 'JPQL', label: 'JPQL' },
              { value: 'NATIVE', label: 'NATIVE' },
            ],
          })}
        </div>`
      : filters.mode === 'localSearch'
        ? html`<div className="filters-grid">
            ${TextField({
              label: 'Быстрый поиск по курсам',
              value: filters.search,
              onChange: (v) => setFilters((p) => ({ ...p, search: v })),
              placeholder: 'Название, преподаватель, категория...',
            })}
          </div>`
        : html`<div className="filters-grid">
            ${SelectField({
              label: 'Уровень',
              value: filters.level,
              onChange: (v) => setFilters((p) => ({ ...p, level: v, page: 1 })),
              options: LEVELS,
            })}
            ${SelectField({
              label: 'Сортировка',
              value: filters.sortBy,
              onChange: (v) => setFilters((p) => ({ ...p, sortBy: v })),
              options: SORT_OPTIONS,
            })}
          </div>`;

  if (formMode === 'create') {
    return html`<div className="container">
      <${CourseForm}
        initial=${null}
        students=${students}
        onCancel=${() => setFormMode('none')}
        onSave=${onCreate}
        busy=${busy}
      />
    </div>`;
  }

  if (formMode === 'edit' && details) {
    const nameToId = new Map(students.map((s) => [`${s.firstName} ${s.lastName}`, s.id]));
    const inferredStudentIds = Array.isArray(details.studentNames)
      ? details.studentNames.map((n) => nameToId.get(n)).filter((id) => typeof id === 'number')
      : [];
    const initial = {
      id: details.id,
      title: details.title,
      level: details.level,
      instructorFirstName: details.instructorFirstName,
      instructorLastName: details.instructorLastName,
      instructorSpecialization: details.instructorSpecialization || '',
      lessons: details.lessons || [],
      studentIds: inferredStudentIds,
      categoryNames: details.categoryNames || [],
    };
    return html`<div className="container">
      <${CourseForm}
        initial=${initial}
        students=${students}
        onCancel=${() => setFormMode('none')}
        onSave=${onUpdate}
        busy=${busy}
      />
    </div>`;
  }

  return html`<div className="container">
    <div className="hero">
      <h1>Курсы</h1>
      <p>Список курсов.</p>
    </div>

    <div className="grid">
      <div>
        <div className="card">
          <div className="card-h">
            <div>
              <h2 className="card-title">Список курсов</h2>
              <p className="card-sub">Всего: ${totalElements}</p>
            </div>
            <div className="row">
              <button className="btn" onClick=${() => setReloadKey((x) => x + 1)} disabled=${loading || busy}>Обновить</button>
              <button className="btn btn-primary" onClick=${() => setFormMode('create')} disabled=${busy}>Добавить</button>
            </div>
          </div>
          <div className="card-b">
            <div className="row-between">
              <div className="row">
                <button
                  type="button"
                  className=${filters.mode === 'list' ? 'btn btn-active' : 'btn'}
                  onClick=${() => setFilters((p) => ({ ...p, mode: 'list', page: 1, search: '' }))}
                >
                  Фильтр
                </button>
                <button
                  type="button"
                  className=${filters.mode === 'apiSearch' ? 'btn btn-active' : 'btn'}
                  onClick=${() => setFilters((p) => ({ ...p, mode: 'apiSearch', page: 1, search: '' }))}
                >
                  Поиск API
                </button>
                <button
                  type="button"
                  className=${filters.mode === 'localSearch' ? 'btn btn-active' : 'btn'}
                  onClick=${() => setFilters((p) => ({ ...p, mode: 'localSearch', page: 1 }))}
                >
                  Быстрый поиск
                </button>
              </div>
            </div>
            ${filters.mode === 'localSearch' && normalizedSearch
              ? html`<div className="hint" style=${{ marginTop: '10px' }}>
                  Найдено по запросу «${filters.search.trim()}»: ${items.length}
                </div>`
              : null}

            <div style=${{ height: '10px' }}></div>
            ${filtersView}

            <div style=${{ height: '12px' }}></div>
            ${loading ? html`<div className="hint">Загрузка…</div>` : null}
            ${error ? html`<div className="hint"><strong>Ошибка:</strong> ${error}</div>` : null}

            ${!loading && !error && items.length === 0 ? html`<div className="hint">Пока пусто.</div>` : null}

            <div className="list" style=${{ marginTop: '10px' }}>
              ${items.map(
                (c) => html`<div className="item" key=${c.id}>
                  <div>
                    <h3>${c.title}</h3>
                    <p>${c.instructorFirstName} ${c.instructorLastName}</p>
                    <div className="meta">${Pill({ children: c.level, strong: true })}</div>
                  </div>
                  <div className="row">
                    <button className="btn" onClick=${() => openDetails(c)} disabled=${busy}>Открыть</button>
                    <button className="btn btn-danger" onClick=${() => onDelete(c.id)} disabled=${busy}>Удалить</button>
                  </div>
                </div>`
              )}
            </div>

            <div className="row-between" style=${{ marginTop: '12px' }}>
              <div className="hint">Страница: ${page} / ${totalPages}</div>
              <div className="row">
                <button className="btn" disabled=${busy || loading || page <= 1} onClick=${() => setFilters((p) => ({ ...p, page: Math.max(1, p.page - 1) }))}>
                  Назад
                </button>
                <button
                  className="btn"
                  disabled=${busy || loading || page >= totalPages || totalPages <= 1}
                  onClick=${() => setFilters((p) => ({ ...p, page: p.page + 1 }))}
                >
                  Вперёд
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div>
        ${CourseDetails({ course: details, onEdit: () => setFormMode('edit') })}
        ${selected && !details ? html`<div className="card"><div className="card-b"><div className="hint">Загрузка деталей…</div></div></div>` : null}
        ${!selected ? html`<div className="card"><div className="card-b"><div className="hint">Открой курс, чтобы увидеть связи.</div></div></div>` : null}
      </div>
    </div>
  </div>`;
}

