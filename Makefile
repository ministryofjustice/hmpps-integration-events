include .env

serve:
	docker-compose up --build -d

unit-test:
	DB_USER=${DB_USER} DB_PASS=${DB_PASS} API_KEY=${API_KEY} TEST_CLIENT_SECRET_NAME=${TEST_CLIENT_SECRET_NAME} ./gradlew test

lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

check:
	DB_USER=${DB_USER} DB_PASS=${DB_PASS} API_KEY=${API_KEY} TEST_CLIENT_SECRET_NAME=${TEST_CLIENT_SECRET_NAME} ./gradlew check

analyse-dependencies:
	./gradlew dependencyCheckAnalyze --info
