# Guía de contribución

¡Gracias por tu interés en **Reto 9 · Mapa GPS**! Esta guía resume cómo colaborar.

## Requisitos
- Android Studio (o el SDK de Android con `compileSdk 35`).
- JDK 17.

## Flujo de trabajo
1. Crea una rama a partir de `main`:
   ```bash
   git checkout -b feat/mi-mejora
   ```
2. Haz tus cambios y compílalos:
   ```bash
   ./gradlew assembleDebug
   ```
3. Abre un Pull Request hacia `main`. El workflow **Build APK** debe quedar en verde ✅.

## Estilo
- Lenguaje: **Kotlin**.
- Nombra las ramas con prefijos: `feat/`, `fix/`, `docs/`, `chore/`.
- Mensajes de commit en modo imperativo y en español.

## Reportar problemas
Abre un *issue* describiendo el problema, el dispositivo y los pasos para reproducirlo.
