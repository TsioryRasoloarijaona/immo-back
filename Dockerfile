# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# The wrapper is distributionType=only-script, so it fetches Maven 3.9.16 at build
# time. busybox provides wget and tar, and mvnw falls back to .tar.gz when unzip
# is absent -- no extra apk packages needed.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# mvnw is committed with mode 100644 (Windows checkout drops the exec bit), so a
# bare ./mvnw is "Permission denied" inside the container.
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B package -DskipTests

# Spring Boot 4 removed the "layertools" jarmode (deprecated in 3.3). Its
# replacement, "tools extract", keeps the four layer directories but changes what
# lands inside them: without --launcher it emits a plain executable jar plus a
# lib/ directory (Main-Class + relative Class-Path in the manifest) instead of an
# exploded BOOT-INF tree. No JarLauncher is extracted at all -- spring-boot-loader/
# comes out empty -- so the entrypoint is a straight "java -jar".
# --application-filename pins the output name; the default carries the project
# version, which would break the entrypoint on every version bump.
RUN java -Djarmode=tools -jar target/*.jar extract \
        --layers \
        --application-filename app.jar \
        --destination /build/extracted

# ---------- Stage 2: runtime ----------
# JRE, not JDK: nothing compiles at runtime, and it saves ~150 MB of registry
# storage per tag. Alpine is safe here -- the PDF renderers only import
# java.awt.Color, which needs no fontconfig/freetype.
FROM eclipse-temurin:21-jre-alpine
# GHCR matches this URL against the repository path to link the package (and to inherit its
# permissions, which is what lets Actions' GITHUB_TOKEN push). The match is case-sensitive:
# the owner is TsioryRasoloarijaona, capital T.
LABEL org.opencontainers.image.source="https://github.com/TsioryRasoloarijaona/immo-back"

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

# Version embarquée : le workflow passe le tag d'image et le SHA du commit en build-args ;
# l'application les relit (app.build.*) pour la métrique immo_back_build_info et
# /actuator/info. Un build local sans arguments les laisse à "inconnu".
ARG IMAGE_TAG=inconnu
ARG GIT_SHA=inconnu
ENV APP_BUILD_TAG=${IMAGE_TAG} \
    APP_BUILD_COMMIT=${GIT_SHA}

# One COPY per layer, ordered least- to most-frequently changed, so a code-only
# change reuses the dependency layers. spring-boot-loader/ is deliberately absent:
# it is always empty in non-launcher mode.
COPY --from=build --chown=app:app /build/extracted/dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/application/ ./

USER app
EXPOSE 8080

# Without MaxRAMPercentage the JVM caps the heap at 25% of the container limit.
# ExitOnOutOfMemoryError makes the container die instead of limping, so the
# restart policy can act on it.
ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-jar", "app.jar"]
