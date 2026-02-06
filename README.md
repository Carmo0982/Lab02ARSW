# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de programación concurrente: condiciones de carrera, sincronización y colecciones seguras.

---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

```bash
mvn clean verify
mvn -q -DskipTests exec:java -Dsnakes=4
```

- `-Dsnakes=N` → inicia el juego con **N** serpientes (por defecto 2).
- **Controles**:
  - **Flechas**: serpiente **0** (Jugador 1).
  - **WASD**: serpiente **1** (si existe).
  - **Espacio** o botón **Action**: Pausar / Reanudar.

---

## Reglas del juego (resumen)

- **N serpientes** corren de forma autónoma (cada una en su propio hilo).
- **Ratones**: al comer uno, la serpiente **crece** y aparece un **nuevo obstáculo**.
- **Obstáculos**: si la cabeza entra en un obstáculo hay **rebote**.
- **Teletransportadores** (flechas rojas): entrar por uno te **saca por su par**.
- **Rayos (Turbo)**: al pisarlos, la serpiente obtiene **velocidad aumentada** temporal.
- Movimiento con **wrap-around** (el tablero “se repite” en los bordes).

---

## Arquitectura (carpetas)

```
co.eci.snake
├─ app/                 # Bootstrap de la aplicación (Main)
├─ core/                # Dominio: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pausa/Reanudar)
├─ concurrency/         # SnakeRunner (lógica por serpiente con virtual threads)
└─ ui/legacy/           # UI estilo legado (Swing) con grilla y botón Action
```

---

# Actividades del laboratorio

## Parte I — (Calentamiento) `wait/notify` en un programa multi-hilo

1. Toma el programa [**PrimeFinder**](https://github.com/ARSW-ECI/wait-notify-excercise).
2. Modifícalo para que **cada _t_ milisegundos**:
   - Se **pausen** todos los hilos trabajadores.
   - Se **muestre** cuántos números primos se han encontrado.
   - El programa **espere ENTER** para **reanudar**.
3. La sincronización debe usar **`synchronized`**, **`wait()`**, **`notify()` / `notifyAll()`** sobre el **mismo monitor** (sin _busy-waiting_).
4. Entrega en el reporte de laboratorio **las observaciones y/o comentarios** explicando tu diseño de sincronización (qué lock, qué condición, cómo evitas _lost wakeups_).

> Objetivo didáctico: practicar suspensión/continuación **sin** espera activa y consolidar el modelo de monitores en Java.

---

## Parte II — SnakeRace concurrente (núcleo del laboratorio)

### 1) Análisis de concurrencia

- Explica **cómo** el código usa hilos para dar autonomía a cada serpiente.
- **Identifica** y documenta en **`el reporte de laboratorio`**:
  - Posibles **condiciones de carrera**.
  - **Colecciones** o estructuras **no seguras** en contexto concurrente.
  - Ocurrencias de **espera activa** (busy-wait) o de sincronización innecesaria.

### 2) Correcciones mínimas y regiones críticas

- **Elimina** esperas activas reemplazándolas por **señales** / **estados** o mecanismos de la librería de concurrencia.
- Protege **solo** las **regiones críticas estrictamente necesarias** (evita bloqueos amplios).
- Justifica en **`el reporte de laboratorio`** cada cambio: cuál era el riesgo y cómo lo resuelves.

### 3) Control de ejecución seguro (UI)

- Implementa la **UI** con **Iniciar / Pausar / Reanudar** (ya existe el botón _Action_ y el reloj `GameClock`).
- Al **Pausar**, muestra de forma **consistente** (sin _tearing_):
  - La **serpiente viva más larga**.
  - La **peor serpiente** (la que **primero murió**).
- Considera que la suspensión **no es instantánea**; coordina para que el estado mostrado no quede “a medias”.

### 4) Robustez bajo carga

- Ejecuta con **N alto** (`-Dsnakes=20` o más) y/o aumenta la velocidad.
- El juego **no debe romperse**: sin `ConcurrentModificationException`, sin lecturas inconsistentes, sin _deadlocks_.
- Si habilitas **teleports** y **turbo**, verifica que las reglas no introduzcan carreras.

> Entregables detallados más abajo.

---

## Entregables

1. **Código fuente** funcionando en **Java 21**.
2. Todo de manera clara en **`**el reporte de laboratorio**`** con:
   - Data races encontradas y su solución.
   - Colecciones mal usadas y cómo se protegieron (o sustituyeron).
   - Esperas activas eliminadas y mecanismo utilizado.
   - Regiones críticas definidas y justificación de su **alcance mínimo**.
3. UI con **Iniciar / Pausar / Reanudar** y estadísticas solicitadas al pausar.

---

## Criterios de evaluación (10)

- (3) **Concurrencia correcta**: sin data races; sincronización bien localizada.
- (2) **Pausa/Reanudar**: consistencia visual y de estado.
- (2) **Robustez**: corre **con N alto** y sin excepciones de concurrencia.
- (1.5) **Calidad**: estructura clara, nombres, comentarios; sin _code smells_ obvios.
- (1.5) **Documentación**: **`reporte de laboratorio`** claro, reproducible;

---

## Tips y configuración útil

- **Número de serpientes**: `-Dsnakes=N` al ejecutar.
- **Tamaño del tablero**: cambiar el constructor `new Board(width, height)`.
- **Teleports / Turbo**: editar `Board.java` (métodos de inicialización y reglas en `step(...)`).
- **Velocidad**: ajustar `GameClock` (tick) o el `sleep` del `SnakeRunner` (incluye modo turbo).

---

## Cómo correr pruebas

```bash
mvn clean verify
```

Incluye compilación y ejecución de pruebas JUnit. Si tienes análisis estático, ejecútalo en `verify` o `site` según tu `pom.xml`.

---

## Créditos

Este laboratorio es una adaptación modernizada del ejercicio **SnakeRace** de ARSW. El enunciado de actividades se conserva para mantener los objetivos pedagógicos del curso.

**Base construida por el Ing. Javier Toquica.**

## REPORTE DE LABORATORIO
### 1) Análisis de concurrencia
**Cómo los hilos dan autonimía a cada serpiente**

Exíte una clase llamada SnakeRunner que implementa runnable, eso quiere decir que la función de la clase
es decir qué tarea debe hacer el  hilo. Esta clase tiene 5 atributos: `snake`, `board`, `baseSleepMs`, `turboSleepMs` y `turboTicks`.

A continuación se mostrará lo que hace el hilo al iniciar:
1. Verifíca que el hilo no esté interrumpido.
2. Si no lo está entonces puede que la serpiente cambie de dirección en el tablero.
3. Si la serpiente choca contra un obstáculo, esta cambia de dirección de forma aleatoria.
4. Si la serpiente come un turbo entonces el tiempo en el que está dormido el hilo será menor por lo que la serpiente se mueve más rápido, pero con el tiempo vuelve a su velocidad normal.

Cabe resaltar que cada serpiente corre su método run() en paralelo con los demás.

**Condiciones carrera**

Hay tres candidatos posibles: `res`, `turboTicks` y `board.step(snake)`

- res: es una variable local dentro del hilo asi que cada hilo tiene un res diferente por lo que no sería una condición
  carrera.
- turboTicks: en cada interación turboTicks está disminuyendo, así que esta si sería una variable mutable. El problema es que
  la varaible no se comparte en cada hilo, es una variable "privada" de cada hilo.
- board.step(snake): puede ser una condición de carrera porque todas las serpientes, cada una manejada con un hilo, interactuan con un mismo tablero.

- La clase **snake**: La clase es accedida concurrentemente por múltiples threads sin sincronización.

**Colecciones o estructuras no seguras para hilos**

- HashSet: La clase Board usa esta colección para almacenar  `obstáculos`,  `ratones` y `turbos`.

¿Cuál es el inconveniente?

El problema es que HashSet no es una colección segura para hilos. Cuando varias serpientes interactua al mismo tiempo con estas colecciones, pueden ocurrir condiciones de carrera. En el caso de los ratones, si una serpiente se come un ratón, el ratón debe ser
eliminado del Hash y además se debe poner un nuevo ratón aleatoriamente en el tablero y se debe agregar a la colección. Si otra serpiente modifíca la colección al mismo tiempo, puede que halla un resultado incosistente.

- HashMap: la clase Board utiliza esta estructura de datos para guardar `teleports`.

HashMap tampoco es thread-safe. Si no se protegiera adecuadamente, accesos concurrentes podrían causar inconsistencias en los pares de teletransporte.


**Sincronización innecesaria**
![Captura de pantalla 2026-02-05 113609.png](src/img/Captura%20de%20pantalla%202026-02-05%20113609.png)

En la imagen podemos ver que estos métodos utilizan la palabra clave `synchronized`. Aunque estos métodos pueden ser llamados desde distintos hilos, no interactúan directamente con la lógica de
movimiento de las serpientes ni modifican el estado del tablero, por lo que su sincronización resulta innecesaria. 


### 2) Correcciones mínimas y regiones críticas

**Esperas Activas**

Para identificar esperas activas se analizó la presencia de bucles que evaluaran condiciones de forma continua sin liberar la CPU, así como la ausencia de mecanismos de bloqueo como `sleep()` o `wait()`.

En el código analizado existe un bucle while dentro del método run, sin embargo, en cada iteración el hilo ejecuta Thread.sleep(), lo que provoca que el hilo se bloquee voluntariamente y libere la CPU. Por esta razón, el bucle no constituye una espera activa.

Adicionalmente, durante la ejecución del programa se observó un consumo bajo de CPU, lo cual es consistente con un diseño que evita busy-wait.

**Conclusión:** No se identifican esperas activas en el sistema.

---
**Regiones Críticas y soluciones**

#### Región Crítica: Clase `Snake` (completa)


Durante la ejecución prolongada del programa se detectó el siguiente error:
```
Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException
    at java.base/java.util.ArrayDeque.copyElements(ArrayDeque.java:328)
    at co.eci.snake.core.Snake.snapshot(Snake.java:34)
```

**Análisis:**

La clase `Snake` es accedida concurrentemente por múltiples threads sin sincronización:
- **SnakeRunner:** modifica el estado llamando `advance()`, lee con `head()` y `direction()`.
- **UI:** lee el estado llamando `snapshot()` para dibujar la serpiente en la pantalla.


**El problema:**
El thread de UI invoca `snapshot()` para copiar `body` y simultáneamente, el thread de la serpiente ejecuta `advance()` modificando `body`. La copia del `ArrayDeque` falla al interactuar sobre una colección en modificación.

**Solución implementada:**
```java
public synchronized Direction direction() { 
    return direction; 
}

public synchronized void turn(Direction dir) {
    // Validación y modificación protegidas
    // ...
}

public synchronized Position head() { 
    return body.peekFirst(); 
}

public synchronized Deque<Position> snapshot() { 
    return new ArrayDeque<>(body); // Ahora thread-safe
}

public synchronized void advance(Position newHead, boolean grow) {
    body.addFirst(newHead);
    if (grow) maxLength++;
    while (body.size() > maxLength) body.removeLast();
}
```

**Justificación:**

Se sincronizaron **todos los métodos públicos** de `Snake` para hacer **Exclusión mutua**, eso genera una protección de `snapshot()` porque ahora no puede ejecutarse mientras `advance()` modifica.

---

### 3) Control de ejecución seguro (UI)

#### Implementación de Iniciar/Pausar/Reanudar

Se implementó un sistema completo de control de ejecución que permite **pausar y reanudar** el juego de forma segura, mostrando estadísticas consistentes sin _tearing_ (sin datos a medias).

#### **Componentes Implementados**

##### 1. **Botón de Control (UI)**

En `SnakeApp.java`:
```java
private final JButton actionButton = new JButton("Action");
private final JLabel statsLabel = new JLabel(" ");
```

El botón alterna entre tres estados:
- **"Action"** → Al presionar, pausa el juego
- **"Resume"** → Al presionar, reanuda el juego
- El texto del botón cambia dinámicamente según el estado

##### 2. **GameClock - Gestión de Estados**

En `GameClock.java` se agregaron métodos para consultar y controlar el estado:
```java
public GameState getState() { 
    return state.get(); 
}

public boolean isPaused() { 
    return state.get() == GameState.PAUSED; 
}
```

Estos métodos permiten que los hilos de las serpientes consulten si deben pausarse.

##### 3. **Pausa Real de los Hilos**

En `SnakeRunner.java`, el loop principal verifica constantemente el estado del juego:
```java
@Override
public void run() {
    try {
        while (!Thread.currentThread().isInterrupted() && !snake.isDead()) {
            // Check if paused and wait
            while (clock != null && clock.isPaused()) {
                Thread.sleep(50);  // Espera mientras está pausado
            }
            
            maybeTurn();
            var res = board.step(snake);
            // ... resto de la lógica ...
        }
    } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
    }
}
```

**Cómo funciona:**
- Cuando el juego se pausa, cada hilo de serpiente entra en un loop de espera
- Los hilos **NO avanzan** sus serpientes durante la pausa
- Al reanudar, todos los hilos salen del loop de espera y continúan desde donde estaban

##### 4. **Estadísticas Consistentes**

En `SnakeApp.java`, el método `showPauseStats()` calcula y muestra las estadísticas:

```java
private void showPauseStats() {
    // Serpiente viva más larga
    Snake longestAlive = snakes.stream()
        .filter(s -> !s.isDead())
        .max(Comparator.comparingInt(Snake::length))
        .orElse(null);
    
    // Primera serpiente en morir (peor serpiente)
    Snake firstDead = snakes.stream()
        .filter(Snake::isDead)
        .min(Comparator.comparingLong(Snake::getDeathTime))
        .orElse(null);
    
    // Construir mensaje HTML con colores
    StringBuilder sb = new StringBuilder("<html>");
    if (longestAlive != null) {
        sb.append("<font color='green'>Serpiente viva más larga: #")
          .append(longestAlive.getId())
          .append(" (longitud: ").append(longestAlive.length()).append(")</font>");
    } else {
        sb.append("No hay serpientes vivas");
    }
    sb.append(" | ");
    if (firstDead != null) {
        sb.append("<font color='red'>Peor serpiente (primera en morir): #")
          .append(firstDead.getId()).append("</font>");
    } else {
        sb.append("Ninguna ha muerto aún");
    }
    sb.append("</html>");
    
    statsLabel.setText(sb.toString());
}
```

##### 5. **Tracking de Muerte de Serpientes**

En `Snake.java` se agregaron campos para rastrear el estado vital:

```java
private volatile boolean dead = false;
private volatile long deathTime = Long.MAX_VALUE;
private final int id;
private static int nextId = 0;
private volatile int hits = 0;  // Contador de choques

public void markDead() {
    if (!dead) {
        dead = true;
        deathTime = System.currentTimeMillis();
    }
}

public boolean isDead() { return dead; }
public long getDeathTime() { return deathTime; }
public int getId() { return id; }
```

**Lógica de muerte por 3 choques:**
```java
// En SnakeRunner.java
if (res == Board.MoveResult.HIT_OBSTACLE) {
    snake.addHit();
    randomTurn();  // Girar para evitar chocar de nuevo
    if (snake.getHits() >= 3) {
        snake.markDead();
        break;  // Terminar el hilo
    }
}
```

#### **Flujo de Pausa/Reanudar**

1. **Usuario presiona "Action" o SPACE:**
   ```java
   private void togglePause() {
       if ("Action".equals(actionButton.getText())) {
           actionButton.setText("Resume");
           clock.pause();
           // Esperar que los hilos se detengan, luego mostrar stats
           new Thread(() -> {
               try {
                   Thread.sleep(100);  // Dar tiempo a que se detengan
               } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
               }
               SwingUtilities.invokeLater(this::showPauseStats);
           }).start();
       } else {
           actionButton.setText("Action");
           statsLabel.setText(" ");
           clock.resume();
       }
   }
   ```

2. **GameClock cambia su estado a PAUSED**

3. **Todos los hilos de serpientes detectan el cambio y se pausan**

4. **Después de 100ms, se calculan y muestran las estadísticas**

5. **Usuario presiona "Resume":**
   - Se limpia el label de estadísticas
   - GameClock cambia a RUNNING
   - Todos los hilos salen de su loop de espera
   - Las serpientes continúan desde su posición actual

#### 🛡**Prevención de Tearing (Consistencia)**

**¿Qué es el tearing?**
Es cuando los datos mostrados están "a medias" - por ejemplo, mostrar estadísticas mientras las serpientes siguen moviéndose, causando que los números cambien o sean inconsistentes.

**Cómo lo prevenimos:**

1. **Espera de 100ms**: Damos tiempo a que todos los hilos entren en pausa antes de calcular estadísticas
2. **Uso de `volatile`**: Los campos `dead`, `deathTime`, `hits` usan `volatile` para garantizar visibilidad entre hilos
3. **Método `synchronized`**: `Snake.length()` y `Snake.snapshot()` están sincronizados
4. **Copia inmutable**: Las estadísticas se calculan una vez y se muestran en un label (no se recalculan continuamente)

####  **Capturas de Pantalla**

##### Imagen 1: Juego en Ejecución
![Juego en Ejecución](src/img/juego_ejecutando.png)


##### Imagen 2: Juego Pausado con Estadísticas
![Juego Pausado](src/img/juego_pausado.png)

##### Imagen 3: Juego Pausado con Serpiente Muerta
![Juego Pausado](src/img/juego_serpiente.png)


