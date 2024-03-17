# Архитектура решения Nexo Hub

## 1. Введение

Данный документ является описанием архитектуры программной системы
Nexo Hub для управления умным домом.

## 2. Обзор

[//]: TODO: Рассказ о том, как работает система человеческим литературным
      языком. Кто инициирует взаимодействие, с кем кто общается и когда,
      какие процессы происходят без лишних подробностей.

## 3. Компоненты

### 3.1 App

#### 3.1.1 Описание

Мобильное приложение. Позволяет управлять личным кабинетом и умным домом.
Является фронтэндом системы, через который люди могут взаимодействовуют
со всей инфраструктурой.

#### 3.1.2 Технологии

1. **Kotlin** в качестве языка программирования.

2. **Android Framework** как платформа для разворачивания приложения.

3. **Jetpack Compose** для построения пользовательского интерфейса.

4. **gRPC** протокол для взаимодействия с **Gate**.

### 3.2 Gate

#### 3.2.1 Описание

Микросервис для связи с мобильными приложениями. Пользователи (мобильного
приложения) взаимодействует исключительно с данным сервисом, он является
фасадом всей микросервисвисной системы.

#### 3.2.2 Обязанности

1. Принимает и обрабатывает запросы мобильного приложения, выдает
   ответы на них.

2. Проводит аутентификацию и первичную авторизацию пользователя.

3. ...

#### 3.2.3 Технологии

1. **Kotlin** в качестве языка программирования.

2. **Ktor** как основа приложения.

3. **gRPC** протокол для взаимодействия с **App** и другими микросервисами.

4. **Yandex OAuth** для аутентификации пользователей.

5. **Redis** для кеширования данных аутентификации и слабо волатильных данных.

6. **PostgreSQL** - основная база данных.

### 3.3 Kernel

Микросервис для работы с БД. Хранит информацию о домах, привязанных устройствах,
подробные данные владельцев.

#### 3.3.1 Обязанности

1. Хранит подробную информацию о пользователях и их правах.

2. Хранит информацию о домах и привязанных к ним устройствах.

3. Хранит информацию о типах устройств и данные о конкретных приборов.

#### 3.3.2 Технологии

1. **Kotlin** в качестве языка программирования.

2. **Ktor** как основа приложения.

3. **gRPC** протокол для взаимодействия с **App** и другими микросервисами.

4. **PostgreSQL** - основная база данных.

### 3.4 Logger

Микросервис для ведения логов.

#### 3.4.1 Обязанности

[//]: TODO: Аналогично другим сервисам

#### 3.4.2 Технологии

[//]: TODO: Аналогично другим сервисам. ClickHouse.

### 3.5 Apparatus

Микросервис для имитации 10-ти тысяч домов. Климат, электрооборудование,
люди, погода и т.п.

#### 3.5.1 Обязанности

[//]: TODO: Аналогично другим сервисам

#### 3.5.2 Технологии

[//]: TODO: Аналогично другим сервисам

### 3.6 Mankind

Микросервис имитатор 10-ти тысяч мобильных приложений для тестирования
системы. Без GUI, просто рандомные запросы через API к серверу.

#### 3.6.1 Обязанности

[//]: TODO: Аналогично другим сервисам

#### 3.6.2 Технологии

[//]: TODO: Аналогично другим сервисам

## 4. Прецеденты использования

### 4.1 Прецедент 1

- **Акторы**: Вася, Петя, Ваня.
- **Краткое описание**: Кушаем обед.
- **Последовательность**: Во время обеда съедается салат, а далее
  выпивается чашка кофе.
- **Альтернатива**: Мы подавились и передумали кушать.
- **Предусловия**: Мы голодны.
- **Постусловия**: Мы покушали.

```mermaid
flowchart TB
    c1-->a2
    subgraph ide1 [one]
    a1-->a2
    end
```

## 5. Развёртывание

### 5.1 Технологии

- Каждый микросервис разворачивается в собственном **Docker** контейнере.

- Вся инфраструктура для тестирования поднимается локально с помощью
  **Docker Compose**.

- Мобильное приложение размещается на телефонах клиента.

### 5.2 Диаграмма

[//]: TODO: Схема скопирована с <https://mermaid.js.org/syntax/c4.html>
      просто для демонстрации, можно скипать.

```mermaid
      C4Deployment
    title Deployment Diagram for Internet Banking System - Live

    Deployment_Node(mob, "Customer's mobile device", "Apple IOS or Android"){
        Container(mobile, "Mobile App", "Xamarin", "Provides a limited subset of the Internet Banking functionality to customers via their mobile device.")
    }

    Deployment_Node(comp, "Customer's computer", "Microsoft Windows or Apple macOS"){
        Deployment_Node(browser, "Web Browser", "Google Chrome, Mozilla Firefox,<br/> Apple Safari or Microsoft Edge"){
            Container(spa, "Single Page Application", "JavaScript and Angular", "Provides all of the Internet Banking functionality to customers via their web browser.")
        }
    }

    Deployment_Node(plc, "Big Bank plc", "Big Bank plc data center"){
        Deployment_Node(dn, "bigbank-api*** x8", "Ubuntu 16.04 LTS"){
            Deployment_Node(apache, "Apache Tomcat", "Apache Tomcat 8.x"){
                Container(api, "API Application", "Java and Spring MVC", "Provides Internet Banking functionality via a JSON/HTTPS API.")
            }
        }
        Deployment_Node(bb2, "bigbank-web*** x4", "Ubuntu 16.04 LTS"){
            Deployment_Node(apache2, "Apache Tomcat", "Apache Tomcat 8.x"){
                Container(web, "Web Application", "Java and Spring MVC", "Delivers the static content and the Internet Banking single page application.")
            }
        }
        Deployment_Node(bigbankdb01, "bigbank-db01", "Ubuntu 16.04 LTS"){
            Deployment_Node(oracle, "Oracle - Primary", "Oracle 12c"){
                ContainerDb(db, "Database", "Relational Database Schema", "Stores user registration information, hashed authentication credentials, access logs, etc.")
            }
        }
        Deployment_Node(bigbankdb02, "bigbank-db02", "Ubuntu 16.04 LTS") {
            Deployment_Node(oracle2, "Oracle - Secondary", "Oracle 12c") {
                ContainerDb(db2, "Database", "Relational Database Schema", "Stores user registration information, hashed authentication credentials, access logs, etc.")
            }
        }
    }

    Rel(mobile, api, "Makes API calls to", "json/HTTPS")
    Rel(spa, api, "Makes API calls to", "json/HTTPS")
    Rel_U(web, spa, "Delivers to the customer's web browser")
    Rel(api, db, "Reads from and writes to", "JDBC")
    Rel(api, db2, "Reads from and writes to", "JDBC")
    Rel_R(db, db2, "Replicates data to")

    UpdateRelStyle(spa, api, $offsetY="-40")
    UpdateRelStyle(web, spa, $offsetY="-40")
    UpdateRelStyle(api, db, $offsetY="-20", $offsetX="5")
    UpdateRelStyle(api, db2, $offsetX="-40", $offsetY="-20")
    UpdateRelStyle(db, db2, $offsetY="-10")
```
