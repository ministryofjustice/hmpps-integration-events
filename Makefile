include .env

serve:
	docker-compose up --build -d

unit-test:
	DB_NAME=${DB_NAME} DB_USER=${DB_USER} DB_PASS=${DB_PASS} ./gradlew test

lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

check:
	DB_NAME=${DB_NAME} DB_USER=${DB_USER} DB_PASS=${DB_PASS} ./gradlew check

analyse-dependencies:
	./gradlew dependencyCheckAnalyze --info
