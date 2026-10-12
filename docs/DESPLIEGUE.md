# Despliegue gratuito de MUISCA (Laboratorio y Producción)

## Arquitectura

| Pieza | Servicio gratuito | Laboratorio (rama `lab`) | Producción (rama `main`) |
| --- | --- | --- | --- |
| Frontend Angular | Render Static Site (CDN, no se duerme) | `muisca-unicauca-web-lab` | `muisca-unicauca-web` |
| Backend Spring Boot | Render Web Service Docker (512 MB, 0,1 CPU) | `muisca-unicauca-api-lab` | `muisca-unicauca-api` |
| Base de datos | Aiven for MySQL, plan Free | base `muisca_lab` | base `muisca_prod` |

Todo está descrito en [`render.yaml`](../render.yaml) (Blueprint). Cada servicio se redespliega solo
cuando llega un commit a su rama **y** el CI de GitHub Actions pasa (`autoDeployTrigger: checksPass`).

## Flujo de ramas

```
feature/mi-cambio ──PR──▶ lab ──(merge = despliegue automático en Laboratorio)
                           │
                           └──PR──▶ main ──(merge = despliegue automático en Producción)
```

1. Cree una rama `feature/...` desde `lab` y trabaje ahí.
2. Abra un Pull Request hacia `lab`. El CI compila y prueba; al hacer merge se despliega Laboratorio.
3. Pruebe en Laboratorio. Cuando esté conforme, abra un PR de `lab` hacia `main`; al hacer merge se
   despliega Producción.

Recomendado: en GitHub > Settings > Branches, proteja `main` y `lab` exigiendo PR y CI en verde.

## Puesta en marcha (una sola vez, ~15 minutos)

### 1. Publicar las ramas

En cada repositorio (`muisca` y `muiscaFrontend`):

```bash
git push -u origin feature/prototipo-completo
git push -u origin lab
```

Luego, en GitHub, abra un PR de `lab` hacia `main` y haga merge: así Producción recibe el prototipo
completo. (Si `main` todavía no tiene el `Dockerfile`, el servicio de producción fallará hasta ese merge).

> El repositorio del frontend pertenece a `cristianbelal`. Para que Render pueda leerlo, ese usuario
> debe instalar la aplicación de Render en su cuenta, **o** usted debe hacer un *fork* en
> `victorsotelo45` y cambiar las dos líneas `repo:` del frontend en `render.yaml`.

### 2. Crear la base de datos (Aiven, gratis y sin tarjeta)

1. Cree una cuenta en <https://console.aiven.io/signup>.
2. *Create service* → **MySQL** → plan **Free** → cree el servicio.
3. En *Connection information* copie `Host`, `Port`, `User` (avnadmin) y `Password`.
4. Las URL JDBC quedan así (las bases se crean solas en el primer arranque):

```
Laboratorio: jdbc:mysql://HOST:PORT/muisca_lab?sslMode=REQUIRED&createDatabaseIfNotExist=true
Producción:  jdbc:mysql://HOST:PORT/muisca_prod?sslMode=REQUIRED&createDatabaseIfNotExist=true
```

### 3. Crear los cuatro servicios (Render Blueprint)

1. Cree una cuenta en <https://dashboard.render.com> e inicie sesión con GitHub.
2. *New* → **Blueprint** → elija el repositorio `victorsotelo45/muisca` (rama `main`).
3. Render lee `render.yaml` y pide los valores secretos (`sync: false`) de cada servicio:

| Variable | Valor |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Los de Aiven (base `muisca_lab` o `muisca_prod` según el servicio) |
| `JWT_SECRET` | 64+ caracteres aleatorios, **distintos** en lab y prod. Genere uno con `openssl rand -base64 64` |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Los del `.env` local (también `GOOGLE_CLIENT_ID` en los sitios web) |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Primer administrador de cada entorno |

4. *Apply*. El primer build del backend tarda ~10 minutos (descarga las dependencias de Eclipse).
   Al arrancar por primera vez, el backend crea el administrador y siembra el catálogo
   (3 metamodelos, 1 módulo ATL, 3 plantillas Acceleo y 4 transformaciones publicadas).

### 4. Verificar las URL

Render intenta usar los nombres del Blueprint (`https://muisca-unicauca-api.onrender.com`, etc.). Si
algún nombre estaba ocupado, Render agrega un sufijo. En ese caso actualice:

- `CORS_ALLOWED_ORIGINS` del backend con la URL real de su frontend.
- `API_URL` del sitio web con la URL real de su backend (y *Manual Deploy* del sitio).

Compruebe: `https://<api>/health` debe responder `{"status":"UP"}`.

### 5. Google Sign-In

En Google Cloud Console → Credenciales → su cliente OAuth → *Orígenes de JavaScript autorizados*,
agregue las URL de los dos sitios web. Sin esto, el botón de Google falla en la nube (el acceso con
usuario y contraseña funciona igual).

## Limitaciones del plan gratuito

- **El backend se duerme tras 15 minutos sin tráfico.** La primera petición después lo despierta y
  puede tardar 1–3 minutos (Java con 0,1 CPU). El frontend no se duerme.
- Render da 750 horas gratis al mes por cuenta, compartidas entre los dos backends; como se duermen
  cuando no se usan, alcanza para un prototipo.
- Aiven Free: 1 GB de almacenamiento. Las ejecuciones guardan los modelos y el código generado; si se
  llena, elimine ejecuciones antiguas desde el historial.
- Aiven puede apagar un servicio gratuito sin uso prolongado; se reactiva desde su consola.

## Alternativas gratuitas

| Opción | Ventaja | Inconveniente |
| --- | --- | --- |
| Oracle Cloud *Always Free* (VM ARM, hasta 24 GB) | Mucha más capacidad, no se duerme | Pide tarjeta para verificar; hay que administrar la VM |
| Koyeb (plan Free) | Docker con despliegue por rama | 1 servicio gratuito: no alcanza para dos entornos |
| Google Cloud Run | Escala a cero, arranque más rápido | Requiere cuenta de facturación |
| Vercel / Netlify / Cloudflare Pages | Alternativas para el frontend estático | Igual se necesita dónde correr el backend Java |
