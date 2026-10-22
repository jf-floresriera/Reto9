<div align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Android_robot.svg/200px-Android_robot.svg.png" alt="Android Logo" width="100"/>
  <h1>🚜 Reto 9: Accediendo al GPS y Puntos de Interés (Sector Agroindustrial) 🌾</h1>
  <p><strong>Desarrollo de Aplicaciones para Dispositivos Móviles</strong></p>

  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
  [![Android Studio](https://img.shields.io/badge/Android%20Studio-Jellyfish-green.svg?style=for-the-badge&logo=android-studio)](https://developer.android.com/studio)
  [![OSMDroid](https://img.shields.io/badge/Maps-OSMDroid-orange.svg?style=for-the-badge)](https://osmdroid.github.io/osmdroid/)
  [![Overpass](https://img.shields.io/badge/API-Overpass-lightgrey.svg?style=for-the-badge)](https://wiki.openstreetmap.org/wiki/Overpass_API)
</div>

<hr/>

## 📖 Descripción del Proyecto

Esta aplicación de Android demuestra cómo **acceder a la ubicación GPS** del dispositivo y presentar en un mapa los **Puntos de Interés (POIs)** cercanos dentro de un radio configurable en kilómetros. 

De acuerdo con el desafío planteado, esta solución utiliza **OpenStreetMap (OSMDroid)** y la **Overpass API**, ofreciendo una alternativa robusta, libre y sin costes de facturación en comparación con Google Maps.

¡Hemos llevado este reto mucho más allá! Pensando en un **Sistema de Información Geográfica (GIS) Agroindustrial**, la aplicación ahora permite:
- 🌱 **Puntos de Interés Agro:** Buscar cultivos, fábricas de procesamiento y silos dentro de un radio establecido.
- 🛰️ **Múltiples Capas Satelitales:** Cambiar el mapa base entre el Topográfico de OpenStreetMap, y vistas satelitales de alta resolución de *Esri*, *USGS* y *Microsoft Planetary Computer*.
- ✍️ **Herramientas de Dibujo:** Capacidad para tocar el mapa y dibujar **Líneas** (para trazar caminos) o **Polígonos** (para delimitar lotes o parcelas agrícolas).
- 💾 **Gestor Offline Inteligente:** ¿Estás en medio del campo sin señal? No hay problema. Un botón dedicado permite descargar y guardar en la memoria del celular **únicamente la cuadrícula de mapa visible en tu pantalla**, optimizando la memoria y permitiéndote navegar sin internet.

<br/>

## 🛠️ Tecnologías Utilizadas

- **Lenguaje:** Kotlin
- **Mapas:** [OSMDroid](https://github.com/osmdroid/osmdroid) (Librería open-source para visualizar mapas OSM en Android).
- **Consumo de API:** [Retrofit](https://square.github.io/retrofit/) y [Gson](https://github.com/google/gson) para parsear los datos geográficos de Overpass API.
- **Ubicación:** `play-services-location` (Fused Location Provider) para una obtención de coordenadas rápida y precisa.
- **Persistencia:** `SharedPreferences` para guardar el radio de búsqueda seleccionado por el usuario.

<br/>

## 🏗️ Estructura y Arquitectura

```mermaid
graph TD
    A[Usuario / MainActivity] -->|1. Solicita Ubicación| B(Fused Location API)
    A -->|2. Configura Radio y Filtro| C[(SharedPreferences)]
    A -->|3. Dibuja Mapa| D(OSMDroid MapView)
    A -->|4. Consulta POIs| E[RetrofitClient / OverpassApi]
    E -->|Realiza Query Overpass| F((OpenStreetMap API))
    F -->|Devuelve Nodos y Ways JSON| E
    E -->|Parsea a Modelos Kotlin| A
    A -->|5. Dibuja Marcadores| D
```

### 📁 Estructura del Código
```text
com.example.reto9
 ┣ 📂 data
 ┃ ┣ 📄 OverpassApi.kt (Interfaz de Endpoints)
 ┃ ┣ 📄 OverpassResponse.kt (Modelos de Datos - POJOs)
 ┃ ┗ 📄 RetrofitClient.kt (Instancia Singleton de Retrofit)
 ┣ 📂 utils
 ┃ ┗ 📄 PreferencesManager.kt (Gestión del Radio y Filtros)
 ┗ 📄 MainActivity.kt (Actividad Principal con UI del Mapa)
```

<br/>

## 🔗 Repositorios Anteriores
Puedes revisar el progreso del desarrollo de los retos anteriores aquí:
- [Reto 7](https://github.com/jf-floresriera/Reto7)
- [Reto 8](https://github.com/jf-floresriera/Reto8)
- **[Reto 9 (Actual)](https://github.com/jf-floresriera/Reto9)**

<br/>

## 🚀 Instalación y Uso

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/jf-floresriera/Reto9.apk
   ```
2. **Abrir en Android Studio:** Asegúrate de tener conexión a Internet para que Gradle descargue las dependencias (OSMDroid, Retrofit, Play Services).
3. **Sincronizar Gradle** y ejecutar el proyecto en un emulador o dispositivo físico.
4. **Al abrir la app:** 
   - Concede el permiso de ubicación y almacenamiento.
   - El mapa se centrará en tu posición actual.
   - Usa el **Spinner superior** para buscar Puntos de Interés agroindustriales (Cultivos, Silos, etc).
   - Usa los **Botones Flotantes (Derecha)** para:
     - 🗺️ **Capas**: Cambiar a mapa satelital (Microsoft, Esri, USGS).
     - 💾 **Descargar**: Guardar el mapa de tu pantalla actual para uso Offline.
     - ✍️ **Dibujar**: Trazar rutas o delimitar polígonos.
     - 🗑️ **Limpiar**: Borrar los dibujos realizados.
   - Usa el **botón de Ajustes (⚙️)** superior para cambiar el radio de búsqueda.

*(Nota: En áreas muy urbanizadas o muy rurales extremas, los resultados de la etiqueta `landuse=farmland` pueden variar, depende del mapeo comunitario local de OSM).*

<br/>

<div align="center">
  <b>Desarrollado con 💪 por Jesús Flores</b>
</div>
