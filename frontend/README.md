# Online Learning SPA (React, без npm)

Это учебный SPA‑клиент под backend из `online_learning/`.

## Что реализовано

- **SPA** на React (ESM через CDN, без сборки)
- Работа с API лабораторных: курсы и студенты
- **OneToMany**: `Course → Lesson` (в форме курса редактируются уроки)
- **ManyToMany**: `Course ↔ Student` и `Course ↔ Category`
- **CRUD**:
  - `/api/students` — полный CRUD
  - `/api/courses` — create/update/delete + получение списка
- **Фильтрация/поиск**:
  - `/api/courses?level=...` + сортировка/пагинация
  - `/api/courses/search?categoryName=...&instructorSpecialization=...&queryType=...`

## Запуск

1) Подними backend (обычно `http://localhost:8080`).

2) Запусти статический сервер для фронта:

```bash
cd online_learning/frontend
python3 -m http.server 5173
```

Открой в браузере `http://localhost:5173/`.

## Настройка API URL

По умолчанию фронт ходит на `http://localhost:8080`.
Можно поменять в DevTools консоли:

```js
localStorage.setItem('serverBaseUrl', 'http://localhost:8080')
location.reload()
```

Если фронт запущен в Docker/на PaaS, URL также может подставляться через `API_BASE_URL`
в `runtime-config.js`.

