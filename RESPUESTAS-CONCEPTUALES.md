# RESPUESTAS CONCEPTUALES

## Pregunta 1: Base de Datos por Servicio vs Base de Datos Compartida
**¿Cuáles son las ventajas y desventajas de implementar el patrón "Base de Datos por Servicio" frente a "Base de Datos Compartida" en una arquitectura de microservicios?**

### Ventajas de Base de Datos por Servicio
1. **Acoplamiento Débil**: Los microservicios son totalmente independientes a nivel de almacenamiento. Cambiar el esquema de una tabla en un servicio no impacta ni rompe a los demás.
2. **Persistencia Políglota**: Permite elegir el motor de base de datos más adecuado según el dominio (ej. PostgreSQL para transacciones de clientes y MongoDB o Neo4j para catálogos o recomendaciones).
3. **Escalabilidad Independiente**: Es posible escalar de forma aislada la base de datos que sufra más carga transaccional sin afectar el presupuesto o recursos de otras bases de datos.

### Desventajas de Base de Datos por Servicio
1. **Dificultad en Consultas complejas**: No se pueden realizar `JOIN` directos de SQL entre bases de datos separadas. Requiere agregación de datos a nivel de aplicación (API Composition) o patrones CQRS.
2. **Consistencia Eventual**: Mantener la consistencia entre servicios requiere implementar transacciones distribuidas (patrón Saga) en lugar de transacciones ACID locales.

---

## Pregunta 2: Transacciones Distribuidas y Consistencia Eventual
**Explique el patrón SAGA y cómo ayuda a resolver el problema de la consistencia eventual en un sistema distribuido.**

El patrón **Saga** es una secuencia de transacciones locales. Cada transacción local actualiza los datos dentro de un único microservicio y publica un mensaje o evento para desencadenar la siguiente transacción en la secuencia. 

Si una de las transacciones locales falla (por ejemplo, por falta de stock o tarjeta rechazada), la saga ejecuta **transacciones compensatorias** en sentido inverso para deshacer los cambios realizados por las transacciones locales previas, garantizando que el sistema regrese a un estado consistente.

### Orquestación vs Coreografía
- **Coreografía**: Cada servicio produce y escucha eventos sin un controlador centralizado. Es simple pero difícil de trazar cuando la cantidad de servicios crece.
- **Orquestación**: Un coordinador centralizado (orquestador) le indica a cada participante qué transacción local debe ejecutar. Facilita la trazabilidad y la gestión de la lógica de negocio.

---

## Pregunta 3: Seguridad Perimetral vs Seguridad de Confianza Cero
**¿Qué diferencias existen entre asegurar los microservicios usando seguridad centralizada en el Gateway versus seguridad en cada microservicio (Confianza Cero)?**

### Seguridad Centralizada en el Gateway
- **Mecanismo**: El Gateway actúa como el único filtro de autenticación perimetral (puerta de enlace). Valida el token JWT del cliente una sola vez, deniega accesos no autorizados y propaga headers identificatorios (`X-User-Email`, `X-User-Role`) a la red interna.
- **Ventajas**: Menos complejidad y duplicación de código en los microservicios individuales; desarrollo más rápido.
- **Desventajas**: Si un atacante vulnera la red interna, puede suplantar identidades libremente llamando directamente a los microservicios sin autenticarse (ya que confían ciegamente en los headers recibidos).

### Seguridad de Confianza Cero (Zero Trust)
- **Mecanismo**: Cada microservicio actúa bajo la premisa de que la red es hostil. Por lo tanto, cada uno valida el JWT de forma autónoma y verifica los permisos/roles del usuario en cada petición, sin importar de dónde provenga.
- **Ventajas**: Alta seguridad y defensa en profundidad. Romper el Gateway no expone de forma directa la lógica interna de los servicios.
- **Desventajas**: Mayor sobrecarga de procesamiento (múltiples validaciones criptográficas de firma del token) y redundancia de configuración de seguridad.

---

## Pregunta 4: Patrones de Resiliencia y Tolerancia a Fallos
**Defina el Circuit Breaker y explique detalladamente cómo sus estados (Closed, Open, Half-Open) protegen a los microservicios ante fallos en cascada.**

El patrón **Circuit Breaker** (Disyuntor) actúa como un interruptor de seguridad en la comunicación inter-servicio:
1. **Estado CERRADO (Closed)**: El circuito está cerrado y todas las peticiones fluyen normalmente. El disyuntor monitorea la tasa de fallos y el tiempo de respuesta. Si el porcentaje de errores supera un umbral definido en una ventana temporal, el circuito pasa al estado **ABIERTO**.
2. **Estado ABIERTO (Open)**: El circuito está abierto y corta el flujo inmediato. Todas las peticiones fallan inmediatamente (falla rápido) o se redirigen a un método de **fallback** degradado, evitando saturar al servicio receptor que ya está caído y protegiendo al emisor de quedarse sin hilos en espera. El circuito permanece abierto durante un tiempo preestablecido (`waitDurationInOpenState`).
3. **Estado MITAD ABIERTO (Half-Open)**: Tras expirar el tiempo de espera, el disyuntor permite que un número limitado de peticiones de prueba pasen al servicio remoto. Si estas peticiones tienen éxito, el disyuntor asume que el servicio se recuperó y vuelve a **CERRADO**. Si vuelven a fallar, el disyuntor regresa de inmediato a **ABIERTO**.

---

## Pregunta 5: Configuración Centralizada y Recarga en Caliente
**¿Por qué es importante centralizar la configuración en un ecosistema de microservicios y cómo ayuda Actuator a recargar propiedades en caliente?**

### Importancia de la Configuración Centralizada
En sistemas distribuidos con decenas de instancias de microservicios, modificar un valor de configuración (como una URL de BD, credenciales de API externa o valores de negocio) en archivos locales obligaría a realizar commits, recompilaciones y redespliegues individuales de cada aplicación. Centralizar la configuración (con Spring Cloud Config) permite almacenar los perfiles en un solo lugar (ej. repositorio Git) compartido por todo el ecosistema.

### Actuator y `@RefreshScope`
Cuando una propiedad cambia en el servidor central:
1. Las clases de Spring anotadas con `@RefreshScope` en el microservicio cliente marcan sus instancias de beans como obsoletas.
2. Al disparar una petición POST al endpoint `/actuator/refresh` expuesto por Spring Boot Actuator, el microservicio vuelve a consultar al Config Server, descarga las propiedades actualizadas y reconstruye dinámicamente los beans afectados con los nuevos valores de configuración **en caliente**, sin interrumpir el servicio ni requerir un reinicio del proceso.
