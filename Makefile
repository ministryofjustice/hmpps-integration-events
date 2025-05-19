serve:
	docker-compose up --build -d

unit-test:
	./gradlew test

lint:
	./gradlew ktlintCheck

format:
	./gradlew ktlintFormat

check:
	./gradlew check

analyse-dependencies:
	./gradlew dependencyCheckAnalyze --info
