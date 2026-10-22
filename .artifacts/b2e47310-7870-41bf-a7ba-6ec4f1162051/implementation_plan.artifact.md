# Plan de Implementación: Reto 9 - Accediendo al GPS y Puntos de Interés

Este plan detalla la arquitectura y los pasos para construir la aplicación del Reto 9, la cual obtendrá la ubicación GPS del usuario y mostrará en un mapa (usando OpenStreetMap) los Puntos de Interés (POIs) cercanos dentro de un radio configurable. Además, se generará una documentación robusta y profesional ("varonil y animada") en el `README.md` para tu repositorio en GitHub.

## User Review Required
- **Uso de Mapa**: Se utilizará **OSMDroid (OpenStreetMap)** en lugar de Google Maps para evitar la necesidad de configurar tarjetas de crédito y API Keys de Google, alineándose con los recursos de YouTube y Medium proporcionados.
- **Obtención de POIs**: Se utilizará la **API de Overpass (OSM)**, que es gratuita y permite consultar lugares (hospitales, turismo, etc.) alrededor de unas coordenadas sin requerir API Keys.
- **Interfaz de Usuario**: Dado que el proyecto base usa Android Views (XML), la interfaz principal consistirá en un `MapView` de OSMDroid, un botón para centrar la ubicación y un menú/diálogo para ajustar el radio de búsqueda.

## Open Questions
- ¿Deseas que los puntos de interés se limiten estrictamente a "Hospitales" y "Lugares turísticos", o prefieres que incluya un menú para seleccionar el tipo de POI a buscar? *(Por defecto, buscaré una mezcla de ambos o incluiré un selector sencillo).*

## Proposed Changes

### 1. Configuración y Dependencias
- **`app/build.gradle.kts`**:
  - [MODIFY] Agregar dependencias para `osmdroid-android` (Mapa).
  - [MODIFY] Agregar dependencias para `play-services-location` (GPS).
  - [MODIFY] Agregar dependencias para `Retrofit` y `Gson` (Consumo de la API de Overpass para los POIs).
- **`AndroidManifest.xml`**:
  - [MODIFY] Agregar permisos: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`.

### 2. Lógica de Red y Datos
- **`OverpassApi.kt`**:
  - [NEW] Interfaz de Retrofit para realizar las consultas (queries) a la API de Overpass buscando nodos cercanos al usuario según el radio dado.
- **`PreferencesManager.kt`**:
  - [NEW] Clase para gestionar el guardado y lectura del radio de búsqueda en `SharedPreferences` (por defecto 2 km).

### 3. Interfaz Gráfica (UI)
- **`activity_main.xml`**:
  - [MODIFY] Diseño principal que incluirá el `MapView` ocupando toda la pantalla y un `FloatingActionButton` para actualizar la ubicación.
- **`dialog_settings.xml`**:
  - [NEW] Diseño de un diálogo (o BottomSheet) para que el usuario pueda configurar el radio de búsqueda mediante un Slider o un campo de texto.

### 4. Lógica Principal (Activity)
- **`MainActivity.kt`**:
  - [MODIFY] Solicitar permisos de ubicación en tiempo de ejecución.
  - [MODIFY] Inicializar `osmdroid` y configurar la caché.
  - [MODIFY] Obtener la ubicación actual usando `FusedLocationProviderClient`.
  - [MODIFY] Dibujar el marcador del usuario y, tras consultar la API de Overpass, dibujar los marcadores de los POIs con sus respectivos títulos (nombres).

### 5. Documentación
- **`README.md`**:
  - [NEW] Creación de un README muy completo, visualmente atractivo ("varonil y animado"), con insignias (badges), diagrama de la arquitectura en Mermaid, estructura de carpetas, instrucciones de uso y apartados para colocar los enlaces a las capturas de pantalla y al APK.

## Verification Plan

### Automated Tests
- No se requieren pruebas unitarias estrictas para este reto básico, pero se verificará la compilación limpia del proyecto.

### Manual Verification
- Ejecutar la aplicación en un emulador o dispositivo real.
- Verificar que pida los permisos de GPS.
- Verificar que cargue el mapa de OSM centrado en la ubicación del dispositivo.
- Cambiar el radio en las preferencias y comprobar que la búsqueda de POIs se actualice visualmente en el mapa.
