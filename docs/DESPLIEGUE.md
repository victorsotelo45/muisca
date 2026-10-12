# Despliegue de MUISCA (Laboratorio y Producción)

## Entornos

| Entorno | Rama | Frontend | Backend (API) | Base de datos |
| --- | --- | --- | --- | --- |
| Producción | `main` | <https://muisca-unicauca-web.onrender.com> | <https://muisca-unicauca-api.onrender.com> | `muisca_prod` |
| Laboratorio | `lab` | <https://muisca-unicauca-web-lab.onrender.com> | <https://muisca-unicauca-api-lab.onrender.com> | `muisca_lab` |

- **Hosting:** Render, plan gratuito. El frontend es un *Static Site* (CDN, no se duerme). El backend
  es un *Web Service* Docker (512 MB, 0,1 CPU).
- **Base de datos:** una instancia *Render PostgreSQL Free* (`muisca-db`, PostgreSQL 16) con dos
  bases, una por entorno. Solo acepta conexiones internas desde los servicios de Render.
- **Laboratorio** muestra la insignia "LABORATORIO" en la barra superior.
- Cada entorno tiene su propio `JWT_SECRET` y su propio administrador inicial (`admin`).
- La configuración de los cuatro servicios está en [`render.yaml`](../render.yaml).

## Flujo de ramas: el merge despliega

```
feature/mi-cambio ──PR──▶ lab ──(merge ⇒ despliegue automático en Laboratorio)
                           │
                           └──PR──▶ main ──(merge ⇒ despliegue automático en Producción)
```

1. Cree `feature/...` desde `lab`, trabaje y abra un Pull Request hacia `lab`.
2. GitHub Actions compila y prueba (backend: motores MDE + flujo completo de la API; frontend:
   pruebas unitarias + build). Al hacer merge, Render despliega **Laboratorio**
   (`autoDeployTrigger: checksPass`: solo despliega si el CI pasa).
3. Valide en Laboratorio y abra un PR `lab → main`. Al hacer merge, Render despliega **Producción**.

Recomendado: en GitHub > Settings > Branches proteja `main` y `lab` exigiendo PR y CI en verde.

## ¿PostgreSQL o MySQL?

El backend funciona con **los dos sin cambiar código**. El driver y el dialecto de Hibernate se
deducen de `DB_URL`, y los tipos de columna son portables: `bytea`/`text` en PostgreSQL y
`longblob`/`longtext` en MySQL.

| | PostgreSQL | MySQL |
| --- | --- | --- |
| Uso actual | Despliegue en Render (lab y prod) | Desarrollo local (tu `.env`) |
| URL | `jdbc:postgresql://HOST:5432/BASE` | `jdbc:mysql://HOST:3306/BASE` |
| Gratis en la nube | Render (vence a los 30 días), Neon (sin vencimiento), Supabase | Aiven Free, TiDB Serverless |
| Ventaja | Se crea desde Render con la misma API que los servicios | Igual a tu entorno local |

**Recomendación:** usar PostgreSQL en la nube y MySQL en local; el CI prueba el código con una
base en memoria. **La base gratuita de Render vence el 2026-11-11** (Render da 14 días más para
pasarla a pago, ~USD 6/mes). Antes de esa fecha:

- **Sin costo:** cree un proyecto en [Neon](https://neon.tech) (PostgreSQL gratis sin vencimiento,
  con *branches* para lab y prod) y cambie `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` de los dos
  backends en Render (agregue `?sslmode=require` a la URL).
- **O con MySQL:** cree un MySQL en Aiven y use
  `jdbc:mysql://HOST:PORT/muisca_prod?sslMode=REQUIRED&createDatabaseIfNotExist=true`.

Las tablas se crean solas en el primer arranque (`ddl-auto=update`), junto con el administrador y
el catálogo inicial. Los datos del entorno anterior no se migran automáticamente.

## Primer ingreso

Usuario `admin`. Las contraseñas iniciales de cada entorno se entregaron por fuera del repositorio
(nunca se versionan). Cámbielas en **Mi cuenta → Cambiar contraseña** tras el primer ingreso; la
variable `ADMIN_PASSWORD` solo se usa si no existe ningún administrador.

## Google Sign-In

En Google Cloud Console → Credenciales → su cliente OAuth → *Orígenes de JavaScript autorizados*,
agregue `https://muisca-unicauca-web.onrender.com` y `https://muisca-unicauca-web-lab.onrender.com`.
Sin esto, el botón de Google falla en la nube; el acceso con usuario y contraseña funciona igual.

## Limitaciones del plan gratuito

- **El backend se duerme tras 15 minutos sin tráfico.** La primera petición lo despierta y puede
  tardar 1–3 minutos: Render retiene la petición mientras tanto y la pantalla de acceso avisa que
  el servidor se está activando.
- **Horas de instancia:** Render da 750 horas gratis al mes por workspace, compartidas por los dos
  backends; como se duermen cuando no se usan, alcanzan para un prototipo.
- **Base de datos:** 1 GB, compartido por las dos bases. Las ejecuciones guardan modelos y código;
  elimine las antiguas desde el historial si se acerca al límite.
- **Una ejecución a la vez:** cada backend procesa las transformaciones de una en una para no
  exceder 512 MB; las demás esperan en cola y el usuario ve el avance.

## Operación

- **Variables de entorno:** Render → servicio → *Environment*. Al cambiarlas, el servicio se
  redespliega.
- **Logs:** Render → servicio → *Logs*.
- **Despliegue manual:** Render → servicio → *Manual Deploy* (por ejemplo, tras cambiar `API_URL`).
- **Salud del backend:** `https://<api>/health` debe responder `{"status":"UP"}`.
