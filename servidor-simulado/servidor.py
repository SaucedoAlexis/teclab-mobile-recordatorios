"""
Servidor simulado para la app Recordatorios (Actividad 3).

Es un servicio REST muy simple, hecho solo con la librería estándar de Python
(no hay que instalar nada). Guarda los recordatorios en datos.json.

Uso:
    python servidor.py                 -> escucha en http://localhost:3000/
    python servidor.py --puerto 3001   -> otro puerto

Para que el emulador llegue a este servidor, en la PC se ejecuta una vez:
    adb reverse tcp:3000 tcp:3000
y la app usa http://localhost:3000/ (ver api.properties.example).

Endpoints (todos piden el encabezado X-Api-Key):
    GET    /recordatorios          lista completa
    GET    /recordatorios/{id}     uno solo (404 si no existe)
    PUT    /recordatorios/{id}     crea o reemplaza (idempotente: repetirlo no duplica)
    DELETE /recordatorios/{id}     elimina (204; 404 si no existía)

Para provocar errores y probar la app, abrir en el navegador de la PC:
    http://localhost:3000/_modo/normal       todo funciona
    http://localhost:3000/_modo/error500     todas las respuestas son 500 (error del servidor)
    http://localhost:3000/_modo/lento        tarda 15 segundos en responder (la app corta a los 10: timeout)
    http://localhost:3000/_modo/rechazar400  rechaza los PUT con 400 (solicitud inválida)
"""
import argparse
import json
import os
import threading
import time
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ARCHIVO_DATOS = Path(__file__).resolve().parent / "datos.json"
# La clave se puede cambiar con la variable de entorno RECORDATORIOS_API_KEY.
# El valor por defecto es solo para pruebas locales (no es una credencial real).
CLAVE_API = os.environ.get("RECORDATORIOS_API_KEY", "clave-de-prueba-local")
MODOS = ("normal", "error500", "lento", "rechazar400")
PRIORIDADES = ("HIGH", "MEDIUM", "LOW")

candado = threading.Lock()
modo_actual = "normal"


def ahora_ms():
    return int(time.time() * 1000)


def log(texto):
    print(f"[{datetime.now():%H:%M:%S}] {texto}", flush=True)


def datos_iniciales():
    """Recordatorios con los que arranca el servidor la primera vez."""
    base = ahora_ms() - 3 * 24 * 3600 * 1000
    ejemplos = [
        ("srv-1", "Entregar informe mensual", "Enviar al coordinador antes del viernes", "Trabajo", "HIGH", False),
        ("srv-2", "Turno con el dentista", None, "Salud", "MEDIUM", True),
        ("srv-3", "Pagar la factura de luz", "Vence el 15", "Hogar", "HIGH", False),
        ("srv-4", "Estudiar para el parcial", "Repasar Room y DataStore", "Estudio", "LOW", False),
    ]
    resultado = {}
    for i, (id_, titulo, descripcion, categoria, prioridad, hecho) in enumerate(ejemplos):
        momento = base + i * 3600 * 1000
        resultado[id_] = {
            "id": id_, "title": titulo, "description": descripcion, "category": categoria,
            "priority": prioridad, "done": hecho, "done_at": momento if hecho else None,
            "created_at": momento, "updated_at": momento,
        }
    return resultado


def cargar():
    if ARCHIVO_DATOS.exists():
        return json.loads(ARCHIVO_DATOS.read_text(encoding="utf-8"))
    datos = datos_iniciales()
    guardar(datos)
    return datos


def guardar(datos):
    ARCHIVO_DATOS.write_text(json.dumps(datos, ensure_ascii=False, indent=2), encoding="utf-8")


def validar(cuerpo, id_ruta):
    """Devuelve un texto con el problema, o None si el recordatorio es válido."""
    if not isinstance(cuerpo, dict):
        return "el cuerpo tiene que ser un objeto JSON"
    if cuerpo.get("id") != id_ruta:
        return "el id del cuerpo no coincide con el de la ruta"
    if not isinstance(cuerpo.get("title"), str) or not cuerpo["title"].strip():
        return "falta el título"
    if cuerpo.get("priority") not in PRIORIDADES:
        return "prioridad inválida"
    for campo in ("created_at", "updated_at"):
        if not isinstance(cuerpo.get(campo), int):
            return f"falta {campo}"
    return None


recordatorios = cargar()


class Manejador(BaseHTTPRequestHandler):

    # ---------- utilidades
    def responder(self, codigo, cuerpo=None):
        """Envía la respuesta. Devuelve False si la app ya había cortado la conexión."""
        datos = b"" if cuerpo is None else json.dumps(cuerpo, ensure_ascii=False).encode("utf-8")
        try:
            self.send_response(codigo)
            if cuerpo is not None:
                self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(datos)))
            self.end_headers()
            if datos:
                self.wfile.write(datos)
            return True
        except (BrokenPipeError, ConnectionAbortedError, ConnectionResetError):
            # La app ya cortó la conexión (por ejemplo, por timeout en el modo lento)
            log(f"{self.command} {self.path} -> la app cerró la conexión antes de recibir la respuesta (timeout)")
            return False

    def log_message(self, formato, *args):
        pass  # se usa log() propio, más corto

    def id_de_ruta(self):
        partes = self.path.strip("/").split("/")
        if len(partes) == 2 and partes[0] == "recordatorios" and partes[1]:
            return partes[1]
        return None

    def antes_de_responder(self):
        """Aplica la clave y el modo de simulación. Devuelve False si ya se respondió."""
        if self.headers.get("X-Api-Key") != CLAVE_API:
            self.responder(401, {"error": "clave inválida o ausente"})
            log(f"{self.command} {self.path} -> 401 (clave incorrecta)")
            return False
        if modo_actual == "lento":
            log(f"{self.command} {self.path} ... modo lento, esperando 15 s")
            time.sleep(15)
        if modo_actual == "error500":
            self.responder(500, {"error": "error simulado del servidor"})
            log(f"{self.command} {self.path} -> 500 (simulado)")
            return False
        return True

    # ---------- métodos HTTP
    def do_GET(self):
        global modo_actual
        if self.path.startswith("/_modo/"):
            nuevo = self.path.split("/")[-1]
            if nuevo in MODOS:
                modo_actual = nuevo
                log(f"*** MODO CAMBIADO A: {nuevo} ***")
                self.responder(200, {"modo": nuevo})
            else:
                self.responder(400, {"error": f"modos válidos: {', '.join(MODOS)}"})
            return
        if not self.antes_de_responder():
            return
        if self.path.rstrip("/") == "/recordatorios":
            with candado:
                lista = list(recordatorios.values())
            if self.responder(200, lista):
                log(f"GET /recordatorios -> 200 ({len(lista)} recordatorios)")
            return
        id_ = self.id_de_ruta()
        with candado:
            encontrado = recordatorios.get(id_) if id_ else None
        if encontrado:
            self.responder(200, encontrado)
            log(f"GET {self.path} -> 200")
        else:
            self.responder(404, {"error": "no existe"})
            log(f"GET {self.path} -> 404")

    def do_PUT(self):
        if not self.antes_de_responder():
            return
        id_ = self.id_de_ruta()
        if id_ is None:
            self.responder(404, {"error": "ruta desconocida"})
            return
        largo = int(self.headers.get("Content-Length", 0))
        try:
            cuerpo = json.loads(self.rfile.read(largo) or b"null")
        except json.JSONDecodeError:
            cuerpo = None
        problema = "rechazo simulado" if modo_actual == "rechazar400" else validar(cuerpo, id_)
        if problema:
            self.responder(400, {"error": problema})
            log(f"PUT {self.path} -> 400 ({problema})")
            return
        with candado:
            existente = recordatorios.get(id_)
            # Gana el cambio más reciente: si el servidor ya tiene una versión más nueva, la conserva
            if existente and existente["updated_at"] > cuerpo["updated_at"]:
                guardado, detalle = existente, "se conservó la versión del servidor (más reciente)"
            else:
                recordatorios[id_] = cuerpo
                guardado = cuerpo
                detalle = "actualizado (mismo id, no se duplica)" if existente else "creado"
            guardar(recordatorios)
        if self.responder(200, guardado):
            log(f"PUT {self.path} -> 200 {detalle}  [total: {len(recordatorios)}]")

    def do_DELETE(self):
        if not self.antes_de_responder():
            return
        id_ = self.id_de_ruta()
        with candado:
            existia = id_ in recordatorios
            if existia:
                del recordatorios[id_]
                guardar(recordatorios)
        if existia:
            self.responder(204)
            log(f"DELETE {self.path} -> 204 eliminado  [total: {len(recordatorios)}]")
        else:
            self.responder(404, {"error": "no existe"})
            log(f"DELETE {self.path} -> 404 (ya no existía)")


if __name__ == "__main__":
    argumentos = argparse.ArgumentParser(description="Servidor simulado de recordatorios")
    argumentos.add_argument("--puerto", type=int, default=3000)
    puerto = argumentos.parse_args().puerto
    # Solo escucha en la PC (127.0.0.1). El emulador llega con adb reverse.
    servidor = ThreadingHTTPServer(("127.0.0.1", puerto), Manejador)
    log(f"Servidor simulado en http://localhost:{puerto}/  ({len(recordatorios)} recordatorios en {ARCHIVO_DATOS.name})")
    log("Ctrl+C para detenerlo. Modos: " + ", ".join(f"/_modo/{m}" for m in MODOS))
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        log("Servidor detenido")
