# Online Learning API

Spring Boot REST API для учебной предметной области онлайн-обучения с PostgreSQL и JPA.

## Что реализовано

1. Подключена реляционная БД PostgreSQL.
2. В модели есть 5 сущностей:
   - `Instructor`
   - `Course`
   - `Lesson`
   - `Student`
   - `Category`
3. Реализованы CRUD-операции:
   - для `Course`
   - для `Student`
4. Реализованы связи:
   - `Instructor` -> `Course` как `OneToMany`
   - `Course` -> `Lesson` как `OneToMany`
   - `Course` <-> `Student` как `ManyToMany`
   - `Course` <-> `Category` как `ManyToMany`
5. Продемонстрирована проблема `N+1` и решение через `@EntityGraph`.
6. Добавлены сценарии частичного сохранения без транзакции и полного rollback с `@Transactional`.
7. Реализован bulk-импорт курсов через `POST /api/courses/bulk?transactional=true|false`.
8. В сервисном слое используются `Stream API` и `Optional`.
9. Добавлена ER-диаграмма.
10. Реализована асинхронная бизнес-операция через `@Async` и `CompletableFuture` с возвратом `taskId`.
11. Добавлена проверка статуса выполнения async-задачи.
12. Реализован потокобезопасный счётчик на `AtomicInteger`.
13. Добавлена демонстрация race condition через `ExecutorService` с 64 потоками и исправление через `AtomicInteger` и `synchronized`.
14. Подготовлен и проверен JMeter-план под новые endpoints.

## ER-диаграмма

```text
instructors
-----------
PK id
first_name
last_name
specialization

courses
-------
PK id
title
level
FK instructor_id -> instructors.id

lessons
-------
PK id
title
duration_minutes
lesson_order
FK course_id -> courses.id

students
--------
PK id
first_name
last_name
email UNIQUE

categories
----------
PK id
name UNIQUE

course_students
---------------
PK/FK course_id -> courses.id
PK/FK student_id -> students.id

course_categories
-----------------
PK/FK course_id -> courses.id
PK/FK category_id -> categories.id
```

## Почему такие `CascadeType` и `FetchType`

### `Course -> Lesson`

- `cascade = CascadeType.ALL`
- `orphanRemoval = true`
- причина: уроки не живут отдельно от курса, поэтому при сохранении/обновлении/удалении курса их жизненный цикл должен идти вместе с ним

### `Course -> Instructor`

- `fetch = FetchType.LAZY`
- без cascade
- причина: преподаватель является самостоятельной сущностью, один преподаватель ведёт много курсов, поэтому удаление курса не должно удалять преподавателя

### `Course -> Student`, `Course -> Category`

- `fetch = LAZY` по умолчанию
- без `REMOVE`
- причина: студенты и категории разделяются между курсами; каскадное удаление здесь опасно и может повредить общие данные

## PostgreSQL

По умолчанию приложение ожидает:

```properties
DB_URL=jdbc:postgresql://localhost:5432/online_learning
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

Можно передать свои значения через переменные окружения.

## Запуск

1. Поднять PostgreSQL и создать БД `online_learning`.
2. Запустить приложение:

```bash
./mvnw spring-boot:run
```

При старте приложение добавляет тестовые данные.

## Основные endpoints

### Courses CRUD

```text
POST   /api/courses
POST   /api/courses/bulk
GET    /api/courses
GET    /api/courses/{id}
PUT    /api/courses/{id}
DELETE /api/courses/{id}
```

### Students CRUD

```text
POST   /api/students
GET    /api/students
GET    /api/students/{id}
PUT    /api/students/{id}
DELETE /api/students/{id}
```

### Демонстрация N+1

```text
GET /api/courses/n-plus-one
GET /api/courses/optimized
```

- `/api/courses/n-plus-one` использует обычную загрузку `findAll()` и при маппинге дёргает ленивые связи, что порождает дополнительные SQL-запросы
- `/api/courses/optimized` использует `@EntityGraph(attributePaths = {"instructor", "lessons", "students", "categories"})`

Чтобы увидеть разницу, включены SQL-логи:

```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

## Демонстрация транзакций

```text
POST /api/demo/persistence/without-transaction
POST /api/demo/persistence/with-transaction
POST /api/courses/bulk?transactional=true|false
```

Эти endpoint'ы позволяют показать разницу между частичным сохранением и rollback.

- `without-transaction`: часть данных успевает сохраниться
- `with-transaction`: всё откатывается целиком благодаря `@Transactional`

Для bulk-операции используется сценарий, повторяющий идею из `FinanceTracker`.

- `transactional=true`: весь bulk-импорт курсов атомарный; если один элемент завершился ошибкой, откатываются все уже обработанные элементы
- `transactional=false`: ранее успешно обработанные элементы остаются в БД, даже если один из следующих завершился ошибкой

### Bulk-импорт курсов

```text
POST /api/courses/bulk?transactional=true|false
```

Endpoint принимает JSON-массив объектов `CourseRequestDto`.

Правила:

- request body должен содержать хотя бы один объект, пустой список -> `400`
- каждый элемент валидируется по тем же правилам, что и обычный `POST /api/courses`
- `transactional=true`: весь импорт атомарный
- `transactional=false`: успешные элементы сохраняются по мере обработки списка

Пример bulk-запроса:

```json
[
  {
    "title": "Spring Security Deep Dive",
    "level": "ADVANCED",
    "instructorFirstName": "Pavel",
    "instructorLastName": "Ivanov",
    "instructorSpecialization": "Security",
    "lessons": [
      { "title": "Authentication", "durationMinutes": 40, "lessonOrder": 1 }
    ],
    "studentIds": [1],
    "categoryNames": ["Backend", "Security"]
  },
  {
    "title": "Broken Bulk Demo",
    "level": "ADVANCED",
    "instructorFirstName": "Pavel",
    "instructorLastName": "Ivanov",
    "instructorSpecialization": "Security",
    "lessons": [
      { "title": "Authorization", "durationMinutes": 45, "lessonOrder": 1 }
    ],
    "studentIds": [999],
    "categoryNames": ["Backend"]
  }
]
```

Как показать разницу в состоянии БД:

- сначала убедиться, что курса `Spring Security Deep Dive` в БД ещё нет
- выполнить `POST /api/courses/bulk?transactional=true`
- второй элемент завершится ошибкой `404 Student with id 999 was not found`
- после этого новый курс в БД не появится
- затем выполнить тот же запрос с `POST /api/courses/bulk?transactional=false`
- снова будет та же ошибка `404`
- но первый курс `Spring Security Deep Dive` уже останется в БД вместе с уроком, преподавателем и связями

## Async и конкурентность

### Новые endpoints

```text
POST   /api/course-analytics/tasks/course/{courseId}
GET    /api/course-analytics/tasks/{taskId}
GET    /api/concurrency/counter
POST   /api/concurrency/counter/increment?times=7
DELETE /api/concurrency/counter
POST   /api/concurrency/race-condition-demo?threads=64&incrementsPerThread=500
```

### Что показывают endpoints

- `POST /api/course-analytics/tasks/course/{courseId}` запускает асинхронный расчёт аналитики курса и сразу возвращает `taskId`.
- `GET /api/course-analytics/tasks/{taskId}` показывает статус `RUNNING | COMPLETED | FAILED` и готовый результат.
- `GET/POST/DELETE /api/concurrency/counter` демонстрируют потокобезопасный счётчик на `AtomicInteger`.
- `POST /api/concurrency/race-condition-demo` запускает 50+ потоков и сравнивает unsafe-счётчик с решениями на `AtomicInteger` и `synchronized`.

### Пример показа через Swagger

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Рекомендуемая последовательность:

1. Выполнить `POST /api/course-analytics/tasks/course/1`.
2. Получить `taskId`, например `course-analytics-1`.
3. Сразу выполнить `GET /api/course-analytics/tasks/course-analytics-1` и показать статус `RUNNING`.
4. Повторить `GET` через 1-2 секунды и показать `COMPLETED`.
5. Выполнить `GET /api/concurrency/counter`.
6. Выполнить `POST /api/concurrency/counter/increment?times=7`.
7. Выполнить `DELETE /api/concurrency/counter`.
8. Выполнить `POST /api/concurrency/race-condition-demo?threads=64&incrementsPerThread=500`.

Фактический результат живой проверки async-задачи:

```json
{
  "status": "COMPLETED",
  "taskId": "course-analytics-1",
  "courseId": 1,
  "result": {
    "courseId": 1,
    "courseTitle": "Spring Boot Intensive",
    "instructorFullName": "Ivan Petrov",
    "lessonCount": 2,
    "totalDurationMinutes": 105,
    "enrolledStudentCount": 2,
    "categoryCount": 2
  }
}
```

Фактический результат race condition demo:

```json
{
  "threads": 64,
  "incrementsPerThread": 500,
  "expectedTotal": 32000,
  "unsafeTotal": 581,
  "atomicTotal": 32000,
  "synchronizedTotal": 32000,
  "lostUpdates": 31419
}
```

## JMeter

### Что адаптировано

В проект добавлен план:

```text
jmeter/online-learning-concurrency-demo.jmx
```

Он адаптирован под структуру присланного `tmgr.jmx`, но работает уже с предметной областью `online_learning`.

Используемые samplers:

- `POST /api/course-analytics/tasks/course/${course_id}`
- `GET /api/course-analytics/tasks/${taskId}`
- `POST /api/concurrency/counter/increment?times=10`
- `POST /api/concurrency/race-condition-demo?threads=64&incrementsPerThread=200`
- `GET /api/courses?page=1&size=5`

### Как подключить JMeter к приложению

1. Собрать приложение:

```bash
./mvnw package
```

2. Запустить приложение с профилем `jmeter`:

```bash
java -jar target/online_learning-0.0.1-SNAPSHOT.jar --spring.profiles.active=jmeter
```

Профиль `jmeter` использует H2 и автоматически поднимает тестовые данные, поэтому отдельный PostgreSQL для нагрузки не нужен.

3. Открыть план `jmeter/online-learning-concurrency-demo.jmx`.

4. Проверить переменные в `Test Plan -> User Defined Variables`:

- `protocol=http`
- `host=localhost`
- `port=8080`
- `base_path=/api`
- `course_id=1`

5. Запустить тест через GUI или CLI.

Пример CLI-запуска:

```bash
/home/zhenya/apache-jmeter-5.6.3/bin/jmeter \
  -n \
  -t jmeter/online-learning-concurrency-demo.jmx \
  -l target/jmeter-artifacts/results.jtl \
  -j target/jmeter-artifacts/jmeter.log \
  -e \
  -o target/jmeter-artifacts/report
```

HTML-отчёт после запуска:

```text
target/jmeter-artifacts/report/index.html
```

### Результаты проведённого нагрузочного теста

Конфигурация прогона:

- `64` потока
- `15` секунд ramp-up
- `5` циклов на поток
- `1600` HTTP samples суммарно

Итог по всему тесту:

- throughput: `96.60 req/s`
- average response time: `9.10 ms`
- median: `3 ms`
- p95: `36 ms`
- max: `96 ms`
- errors: `0.00%`

По sampler'ам:

- `Start Async Analytics Task`: avg `4.58 ms`, p95 `11.95 ms`, max `64 ms`, errors `0`
- `Get Async Task Status`: avg `2.32 ms`, p95 `6 ms`, max `26 ms`, errors `0`
- `Increment Atomic Counter`: avg `2.03 ms`, p95 `5 ms`, max `11 ms`, errors `0`
- `Run Race Condition Demo`: avg `31.62 ms`, p95 `64.85 ms`, max `96 ms`, errors `0`
- `Get Courses Page`: avg `4.95 ms`, p95 `12 ms`, max `85 ms`, errors `0`

Вывод:

- async endpoints выдержали конкурентную нагрузку без ошибок;
- статус задачи корректно читался параллельно с её выполнением;
- потокобезопасный счётчик не терял инкременты;
- race condition воспроизводится на unsafe-сценарии и исчезает при `AtomicInteger` и `synchronized`.

## Пример запроса на создание курса

```json
{
  "title": "Hibernate Deep Dive",
  "level": "ADVANCED",
  "instructorFirstName": "Pavel",
  "instructorLastName": "Ivanov",
  "instructorSpecialization": "Persistence",
  "lessons": [
    { "title": "Entity Lifecycle", "durationMinutes": 45, "lessonOrder": 1 },
    { "title": "EntityGraph", "durationMinutes": 50, "lessonOrder": 2 }
  ],
  "studentIds": [1, 2],
  "categoryNames": ["Backend", "Database"]
}
```

## Пример запроса на создание/обновление студента

```json
{
  "firstName": "Anna",
  "lastName": "Petrova",
  "email": "anna.petrova@example.com"
}
```

## Тесты

Для тестов используется профиль `test` с H2 в режиме совместимости с PostgreSQL:

```bash
./mvnw test
```
