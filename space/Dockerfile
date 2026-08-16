# PixelStore — Hugging Face Space (Docker SDK)
#
# İki aşamalı build:
#   1) build  : Android SDK kurulur, ./gradlew assembleDebug ile APK üretilir.
#   2) runtime: Yalnızca çıktılar ve küçük bir Python sunucu kalır (SDK imaja taşınmaz).
#
# Gradle build başarısız olsa bile imaj ayağa kalkar; açılış sayfası build günlüğünü gösterir,
# böylece Space'te hata ayıklanabilir.

# ---------------------------------------------------------------------------------------------
# 1) Build aşaması
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS build

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_USER_HOME=/opt/gradle-home \
    DEBIAN_FRONTEND=noninteractive

ARG ANDROID_CMDLINE_TOOLS=https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
ARG ANDROID_PLATFORM=android-35
ARG ANDROID_BUILD_TOOLS=35.0.0

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl unzip zip ca-certificates git \
 && rm -rf /var/lib/apt/lists/*

# Android command-line tools + gerekli SDK paketleri
RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
 && curl -fsSL "$ANDROID_CMDLINE_TOOLS" -o /tmp/cmdline-tools.zip \
 && unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools" \
 && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
 && rm /tmp/cmdline-tools.zip \
 && yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null \
 && "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
      "platform-tools" \
      "platforms;${ANDROID_PLATFORM}" \
      "build-tools;${ANDROID_BUILD_TOOLS}" > /dev/null

WORKDIR /workspace

# Önce yalnızca build betikleri kopyalanır: kaynak değiştiğinde bağımlılık katmanı yeniden inmez.
COPY project/gradle ./gradle
COPY project/gradlew project/settings.gradle.kts project/build.gradle.kts project/gradle.properties ./
COPY project/app/build.gradle.kts ./app/
RUN chmod +x ./gradlew && ./gradlew --no-daemon help > /dev/null 2>&1 || true

COPY project/ ./

# Kaynak arşivi build'den ÖNCE alınır: böylece çıktı klasörlerini dışlamak gerekmez.
RUN mkdir -p /out \
 && zip -q -r /out/pixelstore-source.zip . -x '.git/*' -x '.gradle/*' -x 'local.properties'

RUN chmod +x ./gradlew \
 && (./gradlew assembleDebug --no-daemon --stacktrace 2>&1 | tee /out/build-log.txt \
     || touch /out/BUILD_FAILED) \
 && (cp app/build/outputs/apk/debug/app-debug.apk /out/PixelStore-debug.apk 2>/dev/null || true)

# ---------------------------------------------------------------------------------------------
# 2) Runtime aşaması
# ---------------------------------------------------------------------------------------------
FROM python:3.11-slim

# Hugging Face Spaces için önerilen uid.
RUN useradd -m -u 1000 pixel

WORKDIR /app

COPY --from=build /out/ /app/artifacts/
COPY --from=build /workspace/app/src/main/assets/www/ /app/demo/
COPY server.py /app/server.py

RUN chown -R pixel:pixel /app
USER pixel

ENV PORT=7860
EXPOSE 7860

CMD ["python", "-u", "/app/server.py"]
