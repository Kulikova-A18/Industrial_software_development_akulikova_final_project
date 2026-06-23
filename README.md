# Итоговый проект по программе

Работу выполнила: `Куликова Алена Владимировна`.

## Содержание

1. [Документация](#документация)
1. [Задание](#задание)

## Документация

> Вся документация располагается по следующему пути [/docs](/docs/).

Назначение [документации о планировании проекта](/docs/01_planning/)

| Документ                                             | Назначение                                                                                                        |
| :--------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------- |
| `01_Planning_OrbitaMarket_project_tasks.md`          | Поэтапный план реализации проекта OrbitaMarket с описанием задач, сроков и ожидаемых результатов каждого этапа    |
| `02_Planned_REST_requests_and_expected_responses.md` | Спецификация всех REST-эндпоинтов с примерами запросов и ожидаемых ответов, включая успешные и ошибочные сценарии |
| `03_Broker_Expected_Events.md`                       | Описание событий брокера сообщений, их структуры и маршрутов публикации и потребления между микросервисами        |
| `04_Planned_order_status.md`                         | Описание жизненного цикла заказа и всех возможных статусов с указанием переходов между ними                       |
| `05_Script_checklist.md`                             | Чек-лист сценариев для ручной и автоматизированной проверки ключевых бизнес-кейсов системы                        |
| `06_Autotest_plan.md`                                | План покрытия автотестами всех эндпоинтов и сквозных сценариев, структура тестового репозитория                   |
| `07_GUI_implementation_plan.md`                      | План дальнейшей разработки графического интерфейса для проекта                                                    |
| `08_Minimum_demonstration_plan_defense.md`           | Минимальный план демонстрации проекта на защите с командами и ожидаемыми результатами                             |

## Задание

Задание для итогового проекта по программе располагается в [lms.bmstu.ru](https://lms.bmstu.ru/mod/assign/view.php?id=45972).

## Запуск проекта

### Быстрый старт

```
sudo ./run.sh
```

### Ручное управление контейнерами

```
# Остановить все контейнеры и удалить тома
sudo docker-compose down -v

# Запустить все сервисы в фоновом режиме
sudo docker-compose up -d

# Просмотр статуса контейнеров
sudo docker-compose ps

# Просмотр логов всех сервисов
sudo docker-compose logs --tail=50

# Просмотр логов конкретного сервиса
sudo docker-compose logs zookeeper
sudo docker-compose logs kafka
sudo docker-compose logs orders
sudo docker-compose logs payments
sudo docker-compose logs gateway
```

### Тестирование API

Установка утилит (если не установлены)
```
sudo apt-get install jq -y
```

Проверка работоспособности

```
# Проверка API Gateway
curl -s http://localhost:8080/actuator/health | jq .

# Создание аккаунта
curl -X POST http://localhost:8080/api/v1/payments/accounts \
  -H "X-User-Id: test-user" \
  -H "Content-Type: application/json" | jq .

# Пополнение баланса
curl -X POST http://localhost:8080/api/v1/payments/accounts/top-up \
  -H "X-User-Id: test-user" \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000}' | jq .

# Проверка баланса
curl -s http://localhost:8080/api/v1/payments/accounts/balance \
  -H "X-User-Id: test-user" | jq .

# Создание заказа
curl -X POST http://localhost:8080/api/v1/orders \
  -H "X-User-Id: test-user" \
  -H "Content-Type: application/json" \
  -d '{
    "product_type": "ARCHIVE",
    "price": 100,
    "payload": {
      "aoi": "test-area",
      "capture_date": "2024-01-01",
      "sensor_type": "optical"
    }
  }' | jq .

# Просмотр заказов
curl -s http://localhost:8080/api/v1/orders \
  -H "X-User-Id: test-user" | jq .
```