FROM adoptopenjdk/openjdk14-openj9 as builder

WORKDIR /usr/app

COPY . .

RUN ./gradlew installDist -Dorg.gradle.daemon=false

FROM adoptopenjdk/openjdk14-openj9

WORKDIR /usr/app

COPY --from=builder /usr/app/build/install/gamegearapi .
COPY --from=builder /usr/app/src src

ENTRYPOINT ["/usr/app/bin/gamegearapi"]