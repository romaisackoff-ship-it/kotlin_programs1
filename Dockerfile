FROM eclipse-temurin:23-jdk
# 2. Рабочая папка внутри контейнера
WORKDIR /app

# 3. Копируем весь проект в контейнер
COPY . .

# 4. Собираем проект (создаём jar)
RUN ./gradlew build -x test

# 5. Запускаем jar
CMD ["java", "-jar", "build/libs/1gradle-practice-1.0-SNAPSHOT.jar"]