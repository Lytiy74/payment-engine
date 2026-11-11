# Payment Engine: роадмапа (перезапуск)

Рушій P2P-переказів на чистій Java 21, без Spring. Проєкт про багатопотоковість: гроші не губляться і не подвоюються під паралельним навантаженням.

| | |
|---|---|
| **Зроблено** | Етап 0 (збірка, CI) і етап 1 (прототип, який губить гроші) |
| **Лишилось** | 5 етапів, 14 задач, ~28 годин |
| **Темп** | 4–5 тижнів при 5–10 год/тиждень |
| **Фініш** | v1.0: HTTP-сервіс із чергою, який тримає інваріант під навантаженням, плюс README з цифрами |

## Що змінилось порівняно зі старим планом

- Було 12 етапів до v1.0, стало 6. Прибрано БД, fraud-перевірки, закриття дня: Spring, БД і API ти закриваєш у Seat Booking Service.
- Прибрано все, що не код: письмові питання, bug hunt, запуски на ARM VM, CI-матрицю, incident reports, ADR. Лишився один README наприкінці.
- Задачі більші: 1–3 години замість 25 хвилин. Кожна закінчується зеленими тестами.
- Усі задачі видно одразу, до самого кінця.

## Як працює задача

Тести на всі задачі вже лежать у репозиторії, вимкнені через `@Disabled`. Заглушки класів теж є: вони кидають `UnsupportedOperationException`.

1. Відкрий тест задачі й видали рядок `@Disabled(...)`.
2. Запусти тест, він червоний:
   `./mvnw -q -pl engine test -Dtest=GlobalLockTransferServiceTest`
3. Пиши реалізацію в класі-заглушці, поки тест не стане зеленим.
4. `./mvnw -B verify`, PR, merge. Задача закрита.

Правила:

1. Тести не змінюй, щоб вони пройшли. Якщо тест здається неправильним, спершу розберись чому.
2. Закрив задачу, і не повертаєшся її шліфувати.
3. Задача зайняла вдвічі більше часу, ніж у плані: зупинись і попроси підказку, а не сиди далі.
4. Жодних фіч поза списком. Ідеї записуй у розділ "Ідеї на потім".
5. В In Progress одна задача.

## Що вже є в коді

```
com.azaika.payment
├── account      Account, Bank
├── transfer     TransferService, TransferResult, NaiveTransferService + заглушки стратегій
├── metrics      заглушки (етап 3)
├── processing   заглушки (етап 4)
├── http         заглушка (етап 5)
└── sim          Simulator: навантаження з консолі
```

Симулятор показує проблему й рішення на цифрах:

```
./mvnw -q -B package
java -jar engine/target/engine.jar --strategy=naive      # RESULT: BROKEN
java -jar engine/target/engine.jar --strategy=global     # після задачі 2.1: RESULT: OK
```

Параметри: `--strategy=naive|global|ordered|trylock|auditable`, `--threads`, `--transfers`, `--accounts`, `--balance`, `--max-amount`.

---

## Етап 2. Locks: гроші не губляться (6 год)

Тема: `synchronized`, monitor, deadlock, lock ordering, `ReentrantLock`, `tryLock`.

- [ ] **2.1 Global-lock transfer strategy** (1 год)
  Клас `GlobalLockTransferService`, тест `GlobalLockTransferServiceTest`.
  Один спільний lock на всі перекази. Перевірка балансу і списання мають бути в одній критичній секції.
  Готово, коли: тест зелений, симулятор із `--strategy=global` друкує `RESULT: OK`.

- [ ] **2.2 Ordered per-account locks: reproduce and fix the deadlock** (2.5 год)
  Клас `OrderedLockTransferService`, тест `OrderedLockTransferServiceTest`.
  Спершу зроби lock на кожен рахунок через вкладений `synchronized` у порядку "from, потім to". Запусти тест: він упаде з повідомленням `DEADLOCK` і стеком потоків. Потім прибери deadlock, не повертаючись до глобального lock.
  Готово, коли: тест зелений, і ти можеш пояснити, яка послідовність кроків двох потоків давала deadlock.

- [ ] **2.3 TryLock transfer strategy with timeout and ACCOUNT_BUSY** (2.5 год)
  Клас `TryLockTransferService`, тест `TryLockTransferServiceTest`.
  Явні locks рахунків (`Account.lock()`), захоплення з таймаутом. Не встиг узяти обидва: поверни `ACCOUNT_BUSY` і відпусти те, що вже взяв.
  Готово, коли: тест зелений, після будь-якого результату жоден lock не лишається захопленим.

## Етап 3. Спільний стан: метрики, реєстр, аудит (5.5 год)

Тема: atomics, `LongAdder`, `ConcurrentHashMap`, check-then-act, `ReadWriteLock` / `StampedLock`.

- [ ] **3.1 Thread-safe transfer metrics and metered decorator** (1.5 год)
  Класи `TransferMetrics`, `MeteredTransferService`, тест `TransferMetricsTest`.
  Лічильники результатів без втрачених оновлень і без одного великого lock. Декоратор записує результат кожного переказу.
  Готово, коли: тест зелений (8 потоків по мільйону записів, рахунок точний).

- [ ] **3.2 Open accounts concurrently: thread-safe account registry** (1.5 год)
  Метод `Bank.openAccount`, тест `BankOpenAccountTest`.
  Відкриття рахунків на льоту з багатьох потоків: унікальні id, жоден рахунок не губиться, читачі не падають. Зараз у `Bank` звичайний `HashMap`.
  Готово, коли: тест зелений, старі `BankTest` теж.

- [ ] **3.3 Consistent total balance while transfers run** (2.5 год)
  Клас `AuditableTransferService`, тест `AuditableTransferServiceTest`.
  `totalBalance()` під час переказів завжди повертає точну суму: аудит ніколи не бачить гроші "в польоті" між списанням і зарахуванням. Перекази при цьому мають іти паралельно між собою.
  Готово, коли: тест зелений, і ти можеш пояснити, чому `bank.totalBalance()` тут не підходить.

## Етап 4. Асинхронна обробка: черга і workers (5.5 год)

Тема: `BlockingQueue`, `ExecutorService`, `CompletableFuture`, backpressure, graceful shutdown.

- [ ] **4.1 Payment processor: bounded queue and worker threads** (2.5 год)
  Клас `PaymentProcessor` (метод `submit`), тест `PaymentProcessorTest`.
  `submit` не блокує викликача й повертає `CompletableFuture`. Переказ виконує worker-потік. Виняток у сервісі завершує лише цей future, worker живе далі.
  Готово, коли: тест зелений (20 000 запитів із 4 потоків, кожен виконано рівно один раз).

- [ ] **4.2 Backpressure: reject requests when the queue is full** (1 год)
  Той самий клас, тест `PaymentProcessorBackpressureTest`.
  Черга повна: `submit` одразу повертає завершений future з `QUEUE_FULL`, не чекає і не виконує переказ у потоці викликача.
  Готово, коли: тест зелений.

- [ ] **4.3 Graceful shutdown of the payment processor** (2 год)
  Метод `PaymentProcessor.shutdown`, тест `PaymentProcessorShutdownTest`.
  Після `shutdown` нові запити отримують `SHUTTING_DOWN`, усе вже прийняте доробляється, worker-потоки зупиняються. Не встигли за таймаут: `false`.
  Готово, коли: тест зелений, жоден прийнятий запит не загублено.

## Етап 5. HTTP API (6.5 год)

Тема: HTTP threading, virtual threads, ідемпотентність, check-then-act на рівні запитів.

Контракт API описано в Javadoc класу `PaymentHttpServer`. Сервер будується на `com.sun.net.httpserver.HttpServer` із JDK. JSON можна розбирати вручну або додати бібліотеку, тести цього не диктують.

- [ ] **5.1 HTTP API: account and transfer endpoints** (3 год)
  Клас `PaymentHttpServer` (`start`, `port`), тест `PaymentHttpServerTest`.
  `GET /accounts/{id}` і `POST /transfers`, коди 200/400/404/405/422.
  Готово, коли: тест зелений, переказ можна зробити через `curl`.

- [ ] **5.2 Idempotency-Key: execute a repeated request once** (2 год)
  Той самий клас, тест `IdempotencyTest`.
  Запити з однаковим `Idempotency-Key` виконують переказ один раз і отримують ту саму відповідь, навіть коли 32 клієнти надсилають їх одночасно.
  Готово, коли: тест зелений.

- [ ] **5.3 Serve HTTP requests concurrently and stop cleanly** (1.5 год)
  Метод `close` і executor сервера, тест `PaymentHttpServerConcurrencyTest`.
  Повільні перекази не стоять у черзі один за одним на рівні HTTP. `close()` зупиняє сервер, повторний виклик безпечний.
  Готово, коли: тест зелений, уся збірка `./mvnw -B verify` зелена без жодного `@Disabled`.

## Етап 6. Цифри і README, v1.0 (4 год)

- [ ] **6.1 JMH: compare transfer strategies** (2 год)
  Один бенчмарк у модулі `benchmarks`: `global`, `ordered`, `trylock` на 1, 4 і 8 потоках, 10 і 1000 рахунків. Лише на ноутбуці.
  Готово, коли: є таблиця throughput і одне речення висновку про те, де глобальний lock програє і чому.

- [ ] **6.2 README with design decisions and numbers, release v1.0** (2 год)
  README англійською: що це, як запустити, схема (HTTP → черга → workers → стратегія), таблиця "проблема → рішення" по етапах, цифри з 6.1. Тег `v1.0` і GitHub Release.
  Готово, коли: README читається за 2 хвилини, реліз опубліковано.

---

## Орієнтовний графік

| Тиждень | Задачі |
|---|---|
| 1 | 2.1, 2.2, 2.3 |
| 2 | 3.1, 3.2, 3.3 |
| 3 | 4.1, 4.2, 4.3 |
| 4 | 5.1, 5.2 |
| 5 | 5.3, 6.1, 6.2 |

## Питання на співбесіді, які закриває проєкт

- Що таке race condition і lost update? Покажи на прикладі.
- Чим `synchronized` відрізняється від `ReentrantLock`? Коли потрібен `tryLock`?
- Чотири умови deadlock і як lock ordering знімає одну з них.
- Чому `LongAdder` швидший за `AtomicLong` під навантаженням?
- Чому `HashMap` не можна використовувати з кількох потоків і що дає `ConcurrentHashMap`?
- Що таке backpressure і що робити, коли черга повна?
- Як зупинити пул потоків, не загубивши задачі?
- Що таке ідемпотентність і як її зробити без подвійного виконання?
- Чим virtual threads відрізняються від platform threads?

## Ідеї на потім

(записуй сюди все, що хочеться додати, і не роби цього зараз)

- Деплой на Oracle Cloud VM як systemd-сервіс.
- Fraud-перевірки з `CompletableFuture` і `Semaphore`.
- Збереження в БД.
