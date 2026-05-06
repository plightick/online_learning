import { API_BASE_URL } from './config.js';

function joinUrl(base, path) {
  if (!base) return path;
  if (base.endsWith('/') && path.startsWith('/')) return base.slice(0, -1) + path;
  if (!base.endsWith('/') && !path.startsWith('/')) return base + '/' + path;
  return base + path;
}

function withQuery(url, query) {
  const u = new URL(url, 'http://placeholder.local');
  for (const [k, v] of Object.entries(query || {})) {
    if (v === undefined || v === null || v === '') continue;
    u.searchParams.set(k, String(v));
  }
  // убираем origin placeholder
  return u.pathname + (u.search ? u.search : '');
}

async function request(method, path, { query, body } = {}) {
  const url = joinUrl(API_BASE_URL, withQuery(path, query));
  let res;
  try {
    res = await fetch(url, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    const err = new Error(`Не удалось подключиться к серверу (в т.ч. возможен CORS): ${API_BASE_URL}`);
    err.cause = e;
    err.url = url;
    throw err;
  }

  const contentType = res.headers.get('content-type') || '';
  const isJson = contentType.includes('application/json');
  const payload = isJson ? await res.json().catch(() => null) : await res.text().catch(() => '');

  if (!res.ok) {
    const message =
      (payload && (payload.message || payload.error || payload.detail)) ||
      (typeof payload === 'string' && payload) ||
      `HTTP ${res.status}`;
    const err = new Error(message);
    err.status = res.status;
    err.payload = payload;
    err.url = url;
    throw err;
  }

  return payload;
}

export const api = {
  getCourses({ level, page = 1, size = 10, sortBy = 'id', ascending = true } = {}) {
    return request('GET', '/api/courses', { query: { level, page, size, sortBy, ascending } });
  },
  getAllCourses() {
    return request('GET', '/api/courses/all');
  },
  searchCourses({ categoryName, instructorSpecialization, queryType = 'JPQL', page = 1, size = 10 } = {}) {
    return request('GET', '/api/courses/search', {
      query: { categoryName, instructorSpecialization, queryType, page, size },
    });
  },
  getCourse(id) {
    return request('GET', `/api/courses/${id}`);
  },
  createCourse(dto) {
    return request('POST', '/api/courses', { body: dto });
  },
  updateCourse(id, dto) {
    return request('PUT', `/api/courses/${id}`, { body: dto });
  },
  deleteCourse(id) {
    return request('DELETE', `/api/courses/${id}`);
  },

  getStudents() {
    return request('GET', '/api/students');
  },
  getStudent(id) {
    return request('GET', `/api/students/${id}`);
  },
  createStudent(dto) {
    return request('POST', '/api/students', { body: dto });
  },
  updateStudent(id, dto) {
    return request('PUT', `/api/students/${id}`, { body: dto });
  },
  deleteStudent(id) {
    return request('DELETE', `/api/students/${id}`);
  },
};

