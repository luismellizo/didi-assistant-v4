# DiDi Assistant 🚗👁️

**Intelligent Real-Time Trip Profiler for DiDi Drivers**

> Asistente inteligente que utiliza Visión Computacional (OCR) para analizar ofertas de viaje en tiempo real y determinar si un servicio es rentable antes de aceptarlo.

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/logo_hacker.png" width="200" alt="DiDi Assistant Logo"/>
</p>

<p align="center">
  <strong>Desarrollada por Luis Mellizo</strong><br/>
  <em>Premium Hacker Edition v4.1</em>
</p>

---

## 📋 Tabla de Contenidos

- [¿Qué es DiDi Assistant?](#-qué-es-didi-assistant)
- [¿Cómo Funciona?](#-cómo-funciona)
- [Capturas de Pantalla](#-capturas-de-pantalla)
- [Instalación](#-instalación)
- [Guía de Uso](#-guía-de-uso)
- [Configuración Avanzada](#-configuración-avanzada)
- [Arquitectura Técnica](#-arquitectura-técnica)
- [Compilar desde el Código Fuente](#-compilar-desde-el-código-fuente)
- [Preguntas Frecuentes](#-preguntas-frecuentes)
- [Descargo de Responsabilidad](#%EF%B8%8F-descargo-de-responsabilidad)

---

## 🧠 ¿Qué es DiDi Assistant?

DiDi Assistant es una aplicación Android diseñada exclusivamente para **conductores de DiDi** que buscan maximizar sus ganancias. La app analiza cada oferta de viaje antes de que la aceptes y te indica con un **indicador visual en pantalla** si el servicio vale la pena o no.

### Problema que Resuelve

Como conductor, recibes docenas de ofertas diarias. Algunas pagan bien, otras no cubren ni la gasolina. **Calcular mentalmente la rentabilidad de cada viaje mientras manejas es peligroso e ineficiente.** DiDi Assistant hace ese cálculo por ti en milésimas de segundo.

### Características Clave

| Característica | Descripción |
|---|---|
| 🔍 **OCR en Tiempo Real** | Lee la pantalla de DiDi usando visión computacional (ML Kit) |
| 💰 **Cálculo de Rentabilidad** | Calcula `$/km` considerando distancia de recogida + destino |
| 🟢🔴 **Indicador Visual** | Overlay con borde neón: verde = aceptar, rojo = rechazar |
| 💳 **Detección de Pago** | Identifica si el pago es Efectivo, Nequi o Tarjeta |
| 🚫 **Filtro de Usuarios Nuevos** | Opción para rechazar automáticamente pasajeros sin historial |
| 📏 **Filtro de Distancia** | Configura la distancia máxima de recogida aceptable |
| ⚙️ **100% Personalizable** | Define tu propia meta de ganancia por kilómetro |
| 🔒 **Privado** | Todo el procesamiento ocurre localmente en tu teléfono |

---

## 🔬 ¿Cómo Funciona?

DiDi Assistant utiliza tres tecnologías de Android combinadas:

```
┌─────────────────────────────────────────────────┐
│  1. MediaProjection API                         │
│     Captura la pantalla del teléfono            │
│     (lo que tú ves en DiDi)                     │
├─────────────────────────────────────────────────┤
│  2. Google ML Kit OCR (On-Device)               │
│     Convierte la imagen capturada en texto      │
│     Detecta: precio, distancia, arrendamientos  │
├─────────────────────────────────────────────────┤
│  3. OracleLogic Engine                          │
│     Procesa los datos: $COP / KM total          │
│     Aplica tus filtros personalizados           │
│     Decide: ✅ Aceptar  o  ❌ Rechazar           │
├─────────────────────────────────────────────────┤
│  4. Overlay Flotante                            │
│     Muestra el resultado sobre DiDi             │
│     Sin salir de la app de conducción           │
└─────────────────────────────────────────────────┘
```

### Ejemplo Real

Imagina que configuras una meta de **$1.350/km**:

1. 📱 Llega una oferta: **$15.000**
2. 📍 La recogida está a **883m** (0.88km) y el destino a **4.9km**
3. 🧮 Cálculo: `$15.000 ÷ 5.78km = $2.595/km`
4. ✅ **¡OK! $2,595/km** → El overlay aparece en **VERDE**

Otra oferta: **$6.300**
1. 📍 Recogida a **1.1km**, destino a **3.1km**
2. 🧮 Cálculo: `$6.300 ÷ 4.2km = $1.500/km`
3. ✅ **¡OK! $1,500/km** → **VERDE** (supera tu meta)

Si fuera **$5.000** con **6km** total:
1. 🧮 `$5.000 ÷ 6km = $833/km`
2. ❌ **RECHAZAR** → **ROJO** (no llega a tu meta de $1.350/km)

---

## 📸 Capturas de Pantalla

> *Las capturas muestran la interfaz "Dark Hack" con los indicadores en tiempo real sobre la app de DiDi.*

| Panel de Control | Viaje Rentable | Viaje No Rentable |
|:---:|:---:|:---:|
| Configuración de parámetros en estilo terminal hacker | Overlay verde con $/km favorable | Overlay rojo indicando rechazo |

---

## 📲 Instalación

### Opción 1: Instalar el APK directamente (Recomendado)

1. **Descarga** el archivo `app-debug.apk` desde la [sección de Releases](../../releases) de este repositorio
2. **Transfiere** el APK a tu teléfono Android (cable USB, WhatsApp, Drive, etc.)
3. **Abre** el APK en tu teléfono
4. Si aparece un aviso de "fuente desconocida", ve a **Ajustes → Seguridad → Permitir fuentes desconocidas** y acepta
5. **Instala** la aplicación

### Opción 2: Compilar desde código fuente

Ver sección [Compilar desde el Código Fuente](#-compilar-desde-el-código-fuente) más abajo.

### Requisitos del Dispositivo

| Requisito | Mínimo | Recomendado |
|---|---|---|
| **Android** | 10 (API 29) | 12+ (API 31+) |
| **RAM** | 3 GB | 4+ GB |
| **Procesador** | Gama media | Snapdragon 600+ |
| **Espacio** | 50 MB | 100 MB |

---

## 📖 Guía de Uso

### Paso 1: Configuración Inicial

Al abrir DiDi Assistant por primera vez verás el **Panel de Control** estilo terminal:

1. **`PARAM_GANANCIA_KM`** → Escribe tu meta de ganancia mínima por kilómetro
   - Ejemplo: `1350` (significa $1.350 COP por km)
   - Si un viaje paga menos de esto, el oráculo lo mostrará en rojo
   
2. **`PARAM_DISTANCIA_RECOGIDA`** → Distancia máxima que quieres recorrer para recoger al pasajero
   - Ejemplo: `1.0` (1 kilómetro máximo)
   - Si el pasajero está más lejos, el sistema lo rechaza automáticamente

3. **`REJECT_NEW_USERS`** → Marca esta casilla si quieres evitar usuarios sin historial (0 arrendamientos)

4. Presiona **`SAVE_CONFIG`** para guardar

### Paso 2: Conceder Permisos

1. Presiona **`GRANT_OVERLAY_PERMISSION`**
   - Se abrirá la pantalla de ajustes de Android
   - Activa "Permitir mostrar sobre otras aplicaciones" para DiDi Assistant
   - Regresa a la app

2. Presiona **`INITIALIZE_ORACLE`**
   - Aparecerá un aviso del sistema pidiendo permiso de grabación de pantalla
   - Acepta el permiso (la app **NO graba video**, solo lee los textos en pantalla)

### Paso 3: Usar con DiDi

1. **Abre la app de DiDi Conductor** normalmente
2. **Espera ofertas** de viaje como siempre
3. **Cuando llegue un servicio**, DiDi Assistant automáticamente:
   - Lee el precio, las distancias y la información del pasajero
   - Calcula la rentabilidad por kilómetro
   - Muestra un cartel flotante en la esquina superior derecha:

| Overlay | Significado |
|---|---|
| 🟢 **¡OK! $X/km** | El viaje supera tu meta. ¡Acéptalo! |
| 🔴 **[ RECHAZAR ]** | El viaje no es rentable. Mejor espera otro |

### Paso 4: Detener el Asistente

Para detener DiDi Assistant:
- Desliza la barra de notificaciones hacia abajo
- Busca la notificación "DiDi Oracle Activo"
- Toca para detener, o simplemente fuerza el cierre de la app

---

## ⚙️ Configuración Avanzada

### Parámetros de Decisión

| Parámetro | Descripción | Valor por Defecto | Rango Sugerido |
|---|---|---|---|
| Meta $/km | Ganancia mínima aceptable por km | $1,350 | $1,000 - $2,000 |
| Max Recogida | Distancia máxima de ida al pasajero | 1.0 km | 0.5 - 2.0 km |
| Rechazar Nuevos | Bloquear usuarios con 0 viajes | Activado | A tu criterio |

### Fórmula de Rentabilidad

```
Rentabilidad = Precio del Viaje ÷ (Distancia Recogida + Distancia Destino)

Ejemplo:
  Precio: $12.000 COP
  Recogida: 0.8 km
  Destino: 5.2 km
  ─────────────────
  Rentabilidad = $12.000 ÷ 6.0 km = $2.000/km ✅
```

### Detección de Método de Pago

El asistente detecta automáticamente el método de pago del pasajero:

| Ícono | Método | Detección |
|---|---|---|
| 💵 | Efectivo | Texto "efectivo" en pantalla |
| 🟣 | Nequi | Texto "nequi" en pantalla |
| 💳 | Tarjeta | Texto "tarjeta" en pantalla |

---

## 🏗️ Arquitectura Técnica

El proyecto está compuesto por tres archivos principales de Kotlin:

```
app/src/main/java/com/kepler/didioracle/
├── MainActivity.kt          # Panel de control y configuración
├── OracleLogic.kt           # Motor de decisión y parseo OCR
└── ScreenReaderService.kt   # Captura de pantalla + OCR + Overlay
```

### `MainActivity.kt`
- Interfaz de configuración estilo "terminal hacker"
- Gestión de permisos (Overlay + MediaProjection)
- Almacenamiento de preferencias con `SharedPreferences`

### `OracleLogic.kt`
- Parseo robusto de textos OCR fragmentados
- Regex inteligente con `negative lookahead` para distinguir metros de minutos
- Sistema de precios candidatos que filtra botones de sugerencia
- Validación con `sanity checks` en todos los valores numéricos

### `ScreenReaderService.kt`
- Servicio en primer plano con `MediaProjection` API
- Captura de pantalla a resolución reducida (50%) para rendimiento
- Procesamiento con **Google ML Kit Text Recognition** (on-device, sin internet)
- Overlay flotante con `WindowManager` y diseño Neon/Dark

### Dependencias

| Librería | Versión | Uso |
|---|---|---|
| ML Kit Text Recognition | 19.0.0 | OCR local en el dispositivo |
| AndroidX AppCompat | 1.6.1 | Compatibilidad de componentes |
| Material Components | 1.11.0 | Tema visual |

---

## 🔨 Compilar desde el Código Fuente

### Prerrequisitos

- **Java JDK 17** o superior
- **Android SDK** con API Level 34
- **Git** instalado
- Un dispositivo Android o emulador

### Pasos

```bash
# 1. Clonar el repositorio
git clone https://github.com/luismellizo/didi-assistant-v4.git
cd didi-assistant-v4

# 2. Configurar el SDK (crear local.properties)
echo "sdk.dir=/ruta/a/tu/android/sdk" > local.properties

# 3. Compilar el APK
./gradlew assembleDebug

# 4. El APK estará en:
# app/build/outputs/apk/debug/app-debug.apk

# 5. Instalar en dispositivo conectado por USB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Estructura del Proyecto

```
didi-assistant-v4/
├── app/
│   ├── build.gradle                    # Dependencias y configuración
│   └── src/main/
│       ├── AndroidManifest.xml         # Permisos y servicios
│       ├── java/com/kepler/didioracle/
│       │   ├── MainActivity.kt         # UI de configuración
│       │   ├── OracleLogic.kt          # Motor de análisis
│       │   └── ScreenReaderService.kt  # Servicio de captura
│       └── res/
│           ├── drawable-nodpi/         # Logo de la app
│           ├── values/colors.xml       # Paleta Hacker (Neon Green)
│           └── values/themes.xml       # Tema Dark Mode
├── build.gradle                        # Config global Gradle
├── settings.gradle                     # Módulos del proyecto
└── README.md                           # Este archivo
```

---

## ❓ Preguntas Frecuentes

<details>
<summary><strong>¿La app graba video de mi pantalla?</strong></summary>

**No.** La app captura frames individuales (fotos) de la pantalla, extrae el texto mediante OCR y los descarta inmediatamente de la memoria. No se almacena ni se transmite ningún dato visual. Todo el procesamiento es local.
</details>

<details>
<summary><strong>¿Funciona sin internet?</strong></summary>

**Sí.** El motor de OCR (ML Kit) funciona completamente offline después de la primera instalación. No necesitas conexión a internet para usar el asistente.
</details>

<details>
<summary><strong>¿Consume mucha batería?</strong></summary>

El consumo es moderado. La app captura la pantalla cada ~1 segundo y procesa el texto. En la práctica, consume aproximadamente un **5-8% adicional** de batería en una jornada de 8 horas.
</details>

<details>
<summary><strong>¿DiDi puede detectar que uso esta app?</strong></summary>

La app no interactúa con DiDi de ninguna forma. Solo "lee" lo que aparece en tu pantalla, igual que lo harías tú con los ojos. No modifica, inyecta código ni automatiza acciones dentro de DiDi.
</details>

<details>
<summary><strong>¿Funciona con otras apps (Uber, InDrive, etc.)?</strong></summary>

Actualmente está optimizada exclusivamente para **DiDi Conductor** en Colombia (pesos colombianos). Los regex de parseo están calibrados para el formato específico de DiDi. Para otras plataformas se necesitaría adaptar los patrones.
</details>

<details>
<summary><strong>¿Puedo cambiar los parámetros mientras conduzco?</strong></summary>

Sí. Puedes abrir la app, cambiar los valores y presionar SAVE_CONFIG. Los nuevos parámetros se aplican inmediatamente sin necesidad de reiniciar el servicio.
</details>

---

## ⚠️ Descargo de Responsabilidad

> **Esta aplicación es un asistente de apoyo visual.** El uso de la misma es responsabilidad exclusiva del conductor. Asegúrate de cumplir con los términos y condiciones de las plataformas de transporte en tu región. Esta herramienta no automatiza ni manipula ninguna funcionalidad de DiDi Conductor.

---

<p align="center">
  <strong>Desarrollada con ❤️ por Luis Mellizo</strong><br/>
  <em>Bucaramanga, Colombia 🇨🇴</em><br/><br/>
  <sub>Si te fue útil, ¡dale una ⭐ al repositorio!</sub>
</p>
