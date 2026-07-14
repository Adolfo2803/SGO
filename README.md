[![CI](https://github.com/Adolfo2803/SGO/actions/workflows/ci.yml/badge.svg)](https://github.com/Adolfo2803/SGO/actions/workflows/ci.yml)

# SGO — Sistema de Gestión de Oficios

Sistema para registro, control y turnado de oficios recibidos y enviados
en la Dirección del Hospital General.

## Requisitos para desarrollo

- Java 21
- Docker (para la base de datos y las pruebas)
- Maven (incluido via wrapper: `./mvnw`)

## Cómo levantar el proyecto (desarrollo)

```bash
# 1. Base de datos
docker compose up -d db

# 2. Aplicación
./mvnw spring-boot:run

# 3. Pruebas
./mvnw test
```

La aplicación estará en `http://localhost:8080`.

## Integración continua

El workflow de GitHub Actions (`.github/workflows/ci.yml`) se ejecuta en cada **push a `main`**
y en cada **pull request contra `main`**. Realiza lo siguiente:

1. Compila el proyecto con Java 21 (Temurin).
2. Ejecuta **todas** las pruebas, incluidas las de integración con base de datos
   (Testcontainers levanta un PostgreSQL 16 automáticamente en el runner).
3. Empaqueta la aplicación (`./mvnw -B verify`).
4. Sube los reportes de Surefire como artefacto, incluso si el build falla,
   para poder diagnosticar sin reproducir localmente.

**Si el badge está en rojo** significa que el último push a `main` o el último PR
no compiló o tiene pruebas fallidas. Revisa la pestaña **Actions** del repositorio
para ver el detalle del error y descarga el artefacto `reportes-pruebas` si necesitas
los XML de Surefire.

## Despliegue en el hospital

### Requisitos del servidor

- Linux (Ubuntu 22.04+ o similar)
- Docker Engine 24+ y Docker Compose v2
- 2 GB de RAM mínimo, 4 GB recomendado
- Disco suficiente para los PDFs escaneados (~1 GB por cada 10,000 oficios)
- Acceso a un servidor SMTP para el envío de correos de turnado

### Instalación desde cero

```bash
# 1. Clonar el repositorio
git clone https://github.com/Adolfo2803/SGO.git
cd SGO

# 2. Crear el archivo de configuración
cp .env.example .env
nano .env   # editar TODAS las variables con valores reales

# 3. Construir y levantar
docker compose -f docker-compose.prod.yml up -d --build

# 4. Verificar que todo esté sano
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs app --tail 50
```

La aplicación estará en `https://<ip-del-servidor>`.

**Primer inicio de sesión:** usuario `admin`, contraseña la definida en `SGO_ADMIN_PASSWORD`.

### Certificado HTTPS

Caddy genera un certificado autofirmado (TLS interno). La primera vez que cada
usuaria acceda, el navegador mostrará una advertencia de seguridad. Para evitarla,
instale el certificado raíz de Caddy en los equipos cliente:

```bash
# En el servidor, extraer el certificado raíz
docker compose -f docker-compose.prod.yml exec caddy \
    sh -c 'cat /data/caddy/pki/authorities/local/root.crt' > caddy-root.crt

# Distribuir caddy-root.crt a los equipos de las secretarias e instalarlo:
# - Windows: doble clic → Instalar certificado → Equipo local →
#   Entidades de certificación raíz de confianza
# - Linux: sudo cp caddy-root.crt /usr/local/share/ca-certificates/ && sudo update-ca-certificates
```

### Definir el .env

Copie `.env.example` a `.env` y edite cada variable. Las más importantes:

| Variable | Descripción |
|---|---|
| `POSTGRES_PASSWORD` | Contraseña de la base de datos. Use una larga y aleatoria. |
| `SGO_ADMIN_PASSWORD` | Contraseña del usuario inicial. Solo se usa en el primer arranque. |
| `MAIL_HOST` | Servidor SMTP del hospital. |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Credenciales SMTP. |
| `SGO_CORREO_REMITENTE` | Dirección "De:" en los correos de turnado. |

### Salud de la aplicación (`/actuator/health`)

El endpoint `/actuator/health` solo evalúa lo que es indispensable para operar:
que la aplicación responda y que la base de datos esté accesible. **El correo
SMTP no forma parte del health check.** Un servidor de correo inalcanzable no debe
impedir que las secretarias registren y consulten oficios.

Si el SMTP falla, los turnados quedan en estado `FALLIDO` (visible en el detalle
de cada oficio). El job de reintentos los vuelve a intentar automáticamente.
Para diagnosticar problemas de correo, revise los turnados fallidos en la
interfaz o consulte los logs: `docker compose -f docker-compose.prod.yml logs -f app | grep FALLIDO`.

### Comandos de operación

```bash
# Levantar
docker compose -f docker-compose.prod.yml up -d

# Detener (conserva datos)
docker compose -f docker-compose.prod.yml down

# Ver logs en tiempo real
docker compose -f docker-compose.prod.yml logs -f app

# Ver estado de los contenedores
docker compose -f docker-compose.prod.yml ps
```

### Respaldo

El script `scripts/respaldo.sh` respalda la base de datos y los PDFs.
Conserva los últimos 30 días y elimina los anteriores. No requiere root.

Solo necesita que el contenedor `db` esté corriendo; la aplicación puede estar
detenida. El script carga `.env` automáticamente y valida todas las variables
requeridas antes de empezar. Si algo falla, aborta sin dejar archivos parciales.

**Importante:** el directorio de respaldos debe estar en un **disco distinto** al de los
datos de la aplicación. Un respaldo en el mismo disco no protege contra la falla más
común: que el disco muera. Idealmente, copie los respaldos fuera del servidor
(a un NAS, a otra máquina, o a almacenamiento en la nube del hospital).

La variable `SGO_RESPALDO_DIR` controla el destino (por defecto `$HOME/sgo-respaldos`).

```bash
# Respaldo manual (no requiere source .env, el script lo carga solo)
/ruta/al/SGO/scripts/respaldo.sh

# Agendar respaldo diario a las 2:00 AM con cron (usuario normal, sin root)
# Ejecutar: crontab -e
# Agregar la línea:
0 2 * * * /ruta/al/SGO/scripts/respaldo.sh >> /ruta/al/SGO/respaldo.log 2>&1
```

### Restaurar desde un respaldo

> **Entorno asumido:** servidor Linux con Docker Engine, o Git Bash en Windows
> con Docker Desktop (para pruebas). Los comandos usan `sh -c '...'` para que
> las rutas internas del contenedor (`/data`) no sean alteradas por la
> conversión de rutas de Git Bash. En Linux esto no tiene efecto; en Git Bash
> es indispensable.
>
> Si el servidor de destino es Windows sin Docker, este procedimiento no aplica.
> En ese caso la aplicación se ejecutaría como servicio (JAR + NSSM), PostgreSQL
> sería nativo, y la restauración se haría con `pg_restore.exe` y una copia
> directa de archivos.

```bash
# 0. Cargar las variables de configuración
cd /ruta/al/SGO
source .env

# 1. Detener la aplicación (la BD debe seguir corriendo)
docker compose -f docker-compose.prod.yml stop app

# 2. Restaurar la base de datos
gunzip -c ~/sgo-respaldos/sgo-db-2026-07-13_0200.sql.gz \
    | docker compose -f docker-compose.prod.yml exec -T db \
        psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"

# 3. Restaurar los PDFs (usa el volumen directamente, no necesita app)
#    Ajuste el nombre del volumen si su proyecto tiene otro prefijo.
gunzip -c ~/sgo-respaldos/sgo-archivos-2026-07-13_0200.tar.gz \
    | docker run --rm -i -v sgo_sgo-almacen://data alpine:3 \
        sh -c 'tar xf - -C /data'

# 4. Levantar la aplicación
docker compose -f docker-compose.prod.yml up -d app
```

### Actualizar a una versión nueva

```bash
cd /ruta/al/SGO

# 1. Respaldar antes de actualizar
./scripts/respaldo.sh

# 2. Obtener la nueva versión
git pull origin main

# 3. Reconstruir y reiniciar
docker compose -f docker-compose.prod.yml up -d --build

# Flyway aplicará las migraciones nuevas automáticamente al arrancar.
# Si algo falla, restaurar desde el respaldo del paso 1.
```
