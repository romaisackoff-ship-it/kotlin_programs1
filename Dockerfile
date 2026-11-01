FROM eclipse-temurin:23-jdk
WORKDIR /app

# 1. Сначала копируем файлы сборки + сам скрипт gradlew
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY gradle ./gradle

# 2. Качаем зависимости (этот слой кэшируется)
RUN chmod +x gradlew && ./gradlew dependencies

# 3. Копируем остальной код
COPY src ./src

# 4. Собираем проект
RUN ./gradlew build -x test

CMD ["java", "-jar", "build/libs/1gradle-practice-1.0-SNAPSHOT.jar"]