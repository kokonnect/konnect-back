# Konnect Backend

Spring Boot 기반의 백엔드 애플리케이션입니다.

## 사전 요구사항

- Java 17
- MySQL 8.0+
- Gradle

## 환경 설정

1. **환경 변수 파일 생성**
   ```bash
   cp .env.example .env
   ```

2. **.env 파일 수정**
   ```properties
   DB_URL=jdbc:mysql://localhost:3306/konnect_db?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
   DB_USERNAME=root
   DB_PASSWORD=your_mysql_password
   DB_NAME=konnect_db
   SERVER_PORT=8080
   ```

3. **MySQL 데이터베이스 생성**
   ```sql
   CREATE DATABASE konnect_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

## 실행 방법

```bash
./gradlew bootRun
```

## API 엔드포인트

- 서버: http://localhost:8080
- 기본 인증: admin/admin

## 기술 스택

- Spring Boot 3.5.4
- Spring Security
- Spring Data JPA
- MySQL
- Flyway (Database Migration)
- Lombok
