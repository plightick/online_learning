# 🎓 Online Learning — Spring Boot REST API

## 📌 Описание проекта

`online_learning` — это Spring Boot приложение, реализующее REST API для управления ключевой сущностью предметной области онлайн‑обучения.

В рамках задания реализовано:

1. Создание Spring Boot приложения
2. REST API для ключевой доменной сущности
3. GET endpoint с использованием `@RequestParam`
4. GET endpoint с использованием `@PathVariable`
5. Архитектура слоёв: **Controller → Service → Repository**
6. Использование **DTO** и **Mapper**
7. Настроенный **Checkstyle** и приведение кода к единому стилю

---

## 🧩 Предметная область

### 🎯 Ключевая сущность: `Course`

Сущность описывает онлайн‑курс.

**Основные поля:**

* `id` — уникальный идентификатор
* `title` — название курса
* `description` — описание
* `author` — автор курса
* `duration` — продолжительность (в часах)

---

## 🏗 Архитектура проекта

Проект реализован по классической многослойной архитектуре:

```
Controller → Service → Repository → Database
```

### 📁 Структура пакетов

```
src/main/java/.../
 ├── controller
 │    └── CourseController
 ├── service
 │    ├── CourseService
 │    └── impl/CourseServiceImpl
 ├── repository
 │    └── CourseRepository
 ├── dto
 │    └── CourseDto
 ├── mapper
 │    └── CourseMapper
 └── entity
      └── Course
```

---

## 🔁 DTO и Mapper

Для разделения внутренней модели данных и API-ответов используется `CourseDto`.

**Зачем это нужно:**

* Изоляция Entity от внешнего API
* Гибкость изменения внутренней структуры
* Безопасность (не отдаём лишние поля)

### 🔄 Mapper

`CourseMapper` реализует преобразование:

* `Course → CourseDto`
* `CourseDto → Course`

Mapper может быть реализован вручную или с использованием MapStruct.

---

## 🌐 REST API

Базовый путь:

```
/api/courses
```

### 📥 GET с `@PathVariable`

Получение курса по ID:

```
GET /api/courses/{id}
```

Пример:

```
GET /api/courses/1
```

Используется:

```java
@GetMapping("/{id}")
public CourseDto getById(@PathVariable Long id)
```

---

### 🔎 GET с `@RequestParam`

Поиск курсов по автору:

```
GET /api/courses?author=Ivan
```

Используется:

```java
@GetMapping
public List<CourseDto> getByAuthor(@RequestParam String author)
```

Можно расширить дополнительными параметрами фильтрации.

---

## 🗄 Repository Layer

Используется Spring Data JPA:

```java
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByAuthor(String author);
}
```

Repository отвечает только за работу с БД.

---

## ⚙️ Service Layer

Слой бизнес‑логики:

* Валидация данных
* Преобразование Entity ↔ DTO
* Вызов методов Repository

```java
public interface CourseService {
    CourseDto getById(Long id);
    List<CourseDto> getByAuthor(String author);
}
```

---

## 🧪 Технологии

* Java 17+
* Spring Boot
* Spring Web
* Spring Data JPA
* H2 / PostgreSQL
* Maven / Gradle
* Checkstyle

---

## 🎨 Checkstyle

В проекте подключён Checkstyle для:

* Контроля форматирования
* Единого стиля кода
* Проверки соглашений по именованию

Проверка выполняется при сборке проекта:

```
mvn verify
```

или

```
gradle check
```

Код приведён к единому стилю согласно конфигурации `checkstyle.xml`.

---

## 🚀 Запуск проекта

### 1️⃣ Клонировать репозиторий

```
git clone https://github.com/plightick/online_learning.git
cd online_learning
```

### 2️⃣ Запуск

Через Maven:

```
mvn spring-boot:run
```

Или запуск из IDE.

Приложение будет доступно по адресу:

```
http://localhost:8080
```

---

## 📬 Пример ответа API

```json
{
  "id": 1,
  "title": "Spring Boot Basics",
  "description": "Introduction to Spring Boot",
  "author": "Ivan Ivanov",
  "duration": 12
}
```

---

## ✅ Требования задания — выполнено

✔ Создано Spring Boot приложение
✔ Реализован REST API для ключевой сущности
✔ Реализован GET с `@PathVariable`
✔ Реализован GET с `@RequestParam`
✔ Реализована архитектура Controller → Service → Repository
✔ Использованы DTO и Mapper
✔ Настроен Checkstyle

---

## 📄 Лицензия

Проект создан в учебных целях.
