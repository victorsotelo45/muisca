# Despliegue de MUISCA (Laboratorio y Producción)

## Entornos

| Entorno | Rama | Frontend | Backend (API) | Base de datos |
| --- | --- | --- | --- | --- |
| Producción | `main` | <https://muisca-unicauca-web.onrender.com> | <https://muisca-unicauca-api.onrender.com> | `muisca_prod` |
| Laboratorio | `lab` | <https://muisca-unicauca-web-lab.onrender.com> | <https://muisca-unicauca-api-lab.onrender.com> | `muisca_lab` |

| Pieza | Dónde |
| --- | --- |
| Código del backend | <https://github.com/victorsotelo45/muisca> |
| Código del frontend | <https://github.com/victorsotelo45/muisca_frontend> |
| Hosting | Render, plan gratuito: *Static Site* para el frontend (CDN, no se duerme) y *Web Service* Docker para el backend (512 MB, 0,1 CPU) |
| Base de datos | Aiven for MySQL, plan **Free** (`free-1-1gb`, 1 GB): servicio `muisca` con las bases `muisca_lab` y `muisca_prod` |

- **Laboratorio** muestra la insignia "LABORATORIO" en la barra superior.
- Cada entorno tiene su propio `JWT_SECRET` y su propio administrador inicial (`admin`).
- La configuración de los cuatro servicios está en [`render.yaml`](../render.yaml).

## Flujo de ramas: el merge despliega

```
feature/mi-cambio ──PR──▶ lab ──(merge ⇒ despliegue en Laboratorio)
                           │
                           └──PR──▶ main ──(merge ⇒ despliegue en Producción)
```

1. Cree `feature/...` desde `lab`, trabaje y abra un Pull Request hacia `lab`.
2. GitHub Actions compila y prueba. En el backend corre la suite completa (motores MDE + flujo de la
   API) y construye la imagen Docker; en el frontend, las pruebas unitarias y el build.
3. Al hacer merge en `lab` se despliega **Laboratorio**. Valide ahí y abra un PR `lab → main`; al
   hacer merge se despliega **Producción**. Si el CI falla, no se despliega nada.

| Repositorio | Quién despliega | Tiempo |
| --- | --- | --- |
| Backend | La app de GitHub de Render, cuando pasa el CI (`autoDeployTrigger: checksPass`) | ~5 min (build Docker) |
| Frontend | El job `deploy` del CI del propio repo, después de pruebas y build (secreto `RENDER_API_KEY`) | ~2 min |

Recomendado: en GitHub > Settings > Branches proteja `main` y `lab` en ambos repos exigiendo PR y
CI en verde.

## Base de datos: MySQL en Aiven

- **Conexión:** `jdbc:mysql://muisca-muisca.e.aivencloud.com:22783/<base>?sslMode=REQUIRED`, usuario
  `avnadmin`. La contraseña se ve en la consola de Aiven (servicio `muisca` > *Overview*).
- **Si cambia la contraseña** en Aiven, actualice `DB_PASSWORD` en los dos backends de Render.
- **Tablas:** se crean solas en el primer arranque, junto con el administrador y el catálogo inicial.
  En una base vacía el catálogo tarda un par de minutos en aparecer tras el primer arranque, porque
  compila los módulos ATL y Acceleo.
- **Restricción:** Aiven exige clave primaria en todas las tablas (`sql_require_primary_key`); todas
  las entidades de MUISCA la tienen. Téngalo en cuenta si agrega tablas nuevas.
- **Plan gratuito:** 1 GB compartido por las dos bases. Las ejecuciones guardan modelos y código;
  elimine las antiguas desde el historial si se acerca al límite. Revise en la página de precios de
  Aiven las condiciones vigentes del plan Free (por ejemplo, qué pasa con un servicio inactivo).

El backend también funciona con PostgreSQL sin cambiar código: el driver y el dialecto se deducen
de `DB_URL` (`jdbc:postgresql://...`).

## Primer ingreso

Usuario `admin`; las contraseñas iniciales de cada entorno se entregaron por fuera del repositorio.
Cámbielas en **Mi cuenta → Cambiar contraseña**. La variable `ADMIN_PASSWORD` solo se usa si no
existe ningún administrador.

## Google Sign-In

En Google Cloud Console → Credenciales → su cliente OAuth → *Orígenes de JavaScript autorizados*,
agregue `https://muisca-unicauca-web.onrender.com` y `https://muisca-unicauca-web-lab.onrender.com`.

## Limitaciones del plan gratuito de Render

- **El backend se duerme tras 15 minutos sin tráfico.** La primera petición lo despierta y puede
  tardar 1–3 minutos; la aplicación avisa que el servidor se está activando.
- **Horas de instancia:** Render da 750 horas gratis al mes por workspace, compartidas por los dos
  backends; como se duermen cuando no se usan, alcanzan para un prototipo.
- **Una ejecución a la vez** por backend, para no exceder 512 MB; las demás esperan en cola.

## Operación

- **Variables de entorno:** Render → servicio → *Environment* (al cambiarlas, use *Manual Deploy*).
- **Logs:** Render → servicio → *Logs*.
- **Salud del backend:** `https://<api>/health` debe responder `{"status":"UP"}`.
- **API key de Render:** si la cambia, actualice el secreto `RENDER_API_KEY` de `muisca_frontend`
  (GitHub > Settings > Secrets and variables > Actions).
- **Base PostgreSQL anterior:** el servicio `muisca-db` de Render ya no se usa; vence el 2026-11-11 y
  se puede eliminar desde Render sin afectar la aplicación.
