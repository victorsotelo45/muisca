"""Despliega en Render los commits nuevos de las ramas lab y main del frontend.

El repositorio del frontend (cristianbelal/muiscaFrontend) no está conectado a la app de GitHub
de Render, así que Render no se entera de sus merges. Este script, ejecutado por el workflow
deploy-frontend.yml, revisa la punta de cada rama y, si su CI terminó en verde y ese commit aún
no está desplegado, pide el despliegue a Render. Si más adelante se conecta el repositorio a
Render, el commit ya aparecerá desplegado y el script no hará nada.

Variables de entorno: RENDER_API_KEY (secreto) y GITHUB_TOKEN (token del workflow).
"""
import json
import os
import sys
import urllib.request

FRONTEND_REPO = 'cristianbelal/muiscaFrontend'
SERVICES = {
    'lab': 'srv-db6lla8473hc73dpih50',   # muisca-unicauca-web-lab
    'main': 'srv-db6ll98m7kps73bbqotg',  # muisca-unicauca-web
}
FAILED = {'build_failed', 'update_failed', 'canceled', 'pre_deploy_failed'}


def call(url, token, method='GET', body=None):
    headers = {'Authorization': f'Bearer {token}', 'Accept': 'application/json'}
    data = None
    if body is not None:
        headers['Content-Type'] = 'application/json'
        data = json.dumps(body).encode()
    request = urllib.request.Request(url, data=data, method=method, headers=headers)
    with urllib.request.urlopen(request, timeout=30) as response:
        raw = response.read()
        return json.loads(raw) if raw else None


def main() -> int:
    render_key = os.environ.get('RENDER_API_KEY')
    github_token = os.environ.get('GITHUB_TOKEN', '')
    if not render_key:
        print('Falta el secreto RENDER_API_KEY; no se puede desplegar.')
        return 1

    for branch, service_id in SERVICES.items():
        head = call(f'https://api.github.com/repos/{FRONTEND_REPO}/commits/{branch}', github_token)['sha']
        runs = call(f'https://api.github.com/repos/{FRONTEND_REPO}/commits/{head}/check-runs',
                    github_token).get('check_runs', [])
        if not runs or any(r['status'] != 'completed' for r in runs):
            print(f'{branch} {head[:7]}: CI en curso o sin ejecutar; se revisará en la próxima corrida.')
            continue
        if any(r['conclusion'] != 'success' for r in runs):
            print(f'{branch} {head[:7]}: el CI falló; no se despliega.')
            continue

        deploys = call(f'https://api.render.com/v1/services/{service_id}/deploys?limit=10', render_key)
        same_commit = [d['deploy'] for d in deploys if (d['deploy'].get('commit') or {}).get('id') == head]
        if same_commit:
            state = same_commit[0]['status']
            detail = 'ya desplegado o en curso' if state not in FAILED else f'el despliegue anterior terminó en {state}'
            print(f'{branch} {head[:7]}: {detail}; no se repite.')
            continue

        deploy = call(f'https://api.render.com/v1/services/{service_id}/deploys', render_key, 'POST',
                      {'commitId': head})
        print(f'{branch} {head[:7]}: despliegue solicitado ({deploy.get("id")}, {deploy.get("status")}).')
    return 0


if __name__ == '__main__':
    sys.exit(main())
