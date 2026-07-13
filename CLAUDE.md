# Sistema de Gestión de Oficios (SGO)

Sistema para registro, control y turnado de oficios recibidos y enviados en la Dirección del Hospital General.

**Usuarias principales:** secretarias de Dirección (3–5 personas).

---

## 1. Objetivo del MVP

Reemplazar el control manual (libreta / carpeta de Excel) de oficios por un sistema que:

1. Registre oficios de **entrada** y **salida** con su PDF escaneado.
2. Permita **turnar** un oficio por correo a una o varias áreas del hospital, con el archivo adjunto.
3. Lleve **trazabilidad**: qué se turnó, a quién, cuándo, y si el envío fue exitoso.
4. Permita **consultar y filtrar** el histórico.

### Fuera de alcance (v2 — NO implementar ahora)

- Acuses de recibo firmados
- Firma electrónica
- Hilos de respuesta encadenados (oficio → respuesta → contrarrespuesta)
- Roles múltiples / permisos granulares
- Notificaciones push, dashboard de métricas

> Si Claude Code propone algo de esta lista, rechazarlo. El MVP debe llegar a producción antes de crecer.

---

## 2. Stack técnico

| Componente | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 3.3.x |
| Persistencia | Spring Data JPA + Hibernate | — |
| Base de datos | PostgreSQL | 16 |
| Migraciones | Flyway | — |
| Vistas | Thymeleaf + Bootstrap 5 | — |
| Seguridad | Spring Security (form login) | — |
| Correo | Spring Mail (SMTP) | — |
| Build | Maven | — |
| Pruebas | JUnit 5, Mockito, Testcontainers, MockMvc | — |
| Contenedores | Docker + Docker Compose | — |
| CI | GitHub Actions | — |

### Reglas técnicas innegociables

- **`spring.jpa.hibernate.ddl-auto=validate`**. El esquema lo maneja **Flyway**, nunca Hibernate.
- **Los PDFs NO se guardan en la base de datos.** Se guardan en filesystem; en la BD solo la ruta relativa.
- **El envío de correo es asíncrono.** El endpoint de turnado responde de inmediato y el correo se procesa aparte, con reintentos.
- **El folio tiene restricción `UNIQUE` a nivel de base de datos**, no solo validación en el servicio.
- Sin Lombok. Código explícito (facilita la revisión y la enseñanza).

---

## 3. Modelo de dominio

### `Area`
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | PK |
| nombre | String | ej. "Recursos Humanos" |
| correo | String | destino del turnado |
| activa | boolean | soft-delete |

### `Oficio`
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | PK |
| folio | String | **UNIQUE** |
| direccion | enum | `ENTRADA` \| `SALIDA` |
| fecha | LocalDate | fecha del oficio |
| remitente | String | quién lo emite |
| destinatario | String | a quién va dirigido |
| asunto | String | |
| estado | enum | `RECIBIDO` \| `TURNADO` \| `ARCHIVADO` |
| rutaArchivo | String | ruta relativa del PDF |
| creadoPor | String | usuario |
| creadoEn | Instant | auditoría |

### `Turnado`
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | PK |
| oficio | `@ManyToOne` Oficio | |
| area | `@ManyToOne` Area | |
| estado | enum | `PENDIENTE` \| `ENVIADO` \| `FALLIDO` |
| intentos | int | contador de reintentos |
| enviadoEn | Instant | nullable |
| ultimoError | String | nullable |

**Relaciones:** `Oficio 1—N Turnado N—1 Area`

---

## 4. Casos de uso

### CU-01 · Registrar oficio recibido
- **Actor:** Secretaria
- **Precondición:** sesión iniciada
- **Flujo principal:**
  1. Captura folio, fecha, remitente, destinatario, asunto
  2. Adjunta el PDF escaneado
  3. Guarda → estado `RECIBIDO`
- **Alternos:**
  - Folio duplicado → rechaza con mensaje claro
  - Archivo no es PDF o excede 10 MB → rechaza

### CU-02 · Turnar oficio a áreas
- **Flujo principal:**
  1. Selecciona un oficio existente
  2. Elige una o varias áreas activas
  3. Sistema crea un `Turnado` en estado `PENDIENTE` por cada área
  4. Responde inmediato; el envío ocurre en segundo plano
  5. Al enviarse con éxito → `Turnado.estado = ENVIADO`, `Oficio.estado = TURNADO`
- **Alternos:**
  - Fallo de SMTP → `Turnado.estado = FALLIDO`, incrementa `intentos`, registra `ultimoError`
  - Un job programado reintenta los `FALLIDO`/`PENDIENTE` con menos de 3 intentos

### CU-03 · Registrar oficio enviado
- Igual que CU-01, pero `direccion = SALIDA`.
- El folio de salida lo **genera el sistema** como consecutivo: `HG/DIR/{consecutivo}/{año}`

### CU-04 · Consultar oficios
- **Filtros:** rango de fechas, área turnada, estado, dirección, texto en asunto/remitente
- Paginado, ordenado por fecha descendente
- Permite descargar el PDF adjunto

---

## 5. Estructura del proyecto

```
sgo/
├── src/main/java/mx/gob/hospital/sgo/
│   ├── SgoApplication.java
│   ├── config/          # SecurityConfig, AsyncConfig, StorageConfig
│   ├── oficio/          # Oficio, OficioRepository, OficioService, OficioController
│   ├── area/            # Area, AreaRepository, AreaService, AreaController
│   ├── turnado/         # Turnado, TurnadoRepository, TurnadoService, EnvioCorreoJob
│   ├── storage/         # AlmacenArchivos (interfaz) + AlmacenArchivosLocal
│   └── common/          # excepciones, validadores
├── src/main/resources/
│   ├── db/migration/    # V1__esquema_inicial.sql, V2__datos_areas.sql
│   ├── templates/       # Thymeleaf
│   ├── static/
│   └── application.yml
├── src/test/java/...
├── docker-compose.yml
├── Dockerfile
└── .github/workflows/ci.yml
```

Paquetes **por funcionalidad**, no por capa técnica. Nada de `controllers/`, `services/`, `repositories/` a nivel raíz.

---

## 6. Estrategia de pruebas

| Nivel | Herramienta | Qué se prueba |
|---|---|---|
| Unitaria | JUnit 5 + Mockito | Lógica de servicios: generación de folio, validación de duplicados, transiciones de estado |
| Repositorio | `@DataJpaTest` + Testcontainers | Queries personalizados, restricción UNIQUE de folio |
| Web | `@WebMvcTest` + MockMvc | Controllers: validación de formularios, códigos de respuesta |
| Integración | `@SpringBootTest` + Testcontainers + GreenMail | Flujo completo: registrar → turnar → verificar correo enviado |

**No se persigue un porcentaje de cobertura.** Se persigue que toda regla de negocio tenga una prueba que falle si se rompe.

---

## 7. Cómo levantar el proyecto

```bash
# 1. Base de datos
docker compose up -d db

# 2. Aplicación (desde IntelliJ o CLI)
./mvnw spring-boot:run

# 3. Pruebas
./mvnw test

# 4. Todo en contenedores
docker compose up --build
```

App en `http://localhost:8080`. Usuario semilla definido en `V2__datos_areas.sql`.

---

## 8. Roadmap de implementación

Ejecutar **en este orden**. No avanzar a la siguiente etapa sin que la anterior compile y sus pruebas pasen.

- [ ] **E1 — Andamiaje.** Proyecto Maven, `docker-compose.yml` con Postgres, `application.yml` con perfiles `dev`/`test`/`prod`. Verificación: la app arranca y conecta a la BD.
- [ ] **E2 — Esquema.** `V1__esquema_inicial.sql` con las tres tablas, constraints, índices. Entidades JPA con `ddl-auto=validate`. Verificación: `@DataJpaTest` que guarda y recupera un `Oficio`.
- [ ] **E3 — CU-01.** Registro de oficio de entrada con carga de PDF. `AlmacenArchivos` con implementación local. Pruebas unitarias del servicio + `@WebMvcTest` del controller.
- [ ] **E4 — CU-04.** Listado con filtros y paginación. Descarga del PDF.
- [ ] **E5 — CU-02.** Turnado asíncrono con correo. `EnvioCorreoJob` con reintentos. Prueba de integración con GreenMail.
- [ ] **E6 — CU-03.** Oficio de salida con folio consecutivo generado.
- [ ] **E7 — Seguridad.** Spring Security con form login, usuarios en BD, contraseñas con BCrypt.
- [ ] **E8 — CI.** GitHub Actions: `build → test`. Que Testcontainers corra en el runner.
- [ ] **E9 — Despliegue.** `Dockerfile` multi-etapa, `docker-compose.prod.yml`, reverse proxy con Caddy.
- [ ] **E10 — CD.** Extender el workflow: al hacer merge a `main`, construir imagen, publicar en GHCR, desplegar por SSH.

---

## 9. Convenciones de trabajo

**Ramas:** `main` (protegida) ← PR ← `feature/E3-registro-oficio`

**Commits (Conventional Commits):**
```
feat(oficio): registrar oficio de entrada con PDF adjunto
fix(turnado): reintentar envío cuando SMTP responde 4xx
test(oficio): cubrir validación de folio duplicado
chore(ci): agregar workflow de build y test
```

**Regla:** cada etapa del roadmap es una rama, un PR, y no se mergea sin pruebas verdes.

---

## 10. Instrucciones para Claude Code

Este README es el contrato del proyecto. Antes de escribir código:

1. **Lee este archivo completo.** No propongas tecnologías, entidades ni funcionalidades que no estén aquí.
2. **Trabaja una etapa del roadmap a la vez.** Al terminar, detente y reporta qué se hizo y cómo verificarlo. No encadenes etapas por iniciativa propia.
3. **Escribe la prueba junto con el código, no después.**
4. **Si detectas una ambigüedad o una decisión de diseño no cubierta aquí, pregunta antes de asumir.** Es preferible una pregunta a una implementación que haya que deshacer.
5. **Respeta las reglas innegociables de la sección 2.** Si crees que alguna está equivocada, argumenta el porqué, pero no la cambies sin autorización.
6. **No agregues dependencias** que no estén en la tabla del stack sin consultarlo.
7. **El código, los comentarios y los mensajes de la interfaz van en español.** Los nombres de clases y métodos también (es un sistema de dominio local en español).
