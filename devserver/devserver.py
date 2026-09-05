#!/usr/bin/env python3
# pip install aiohttp
import asyncio, json, time, sys
from aiohttp import web, WSMsgType

clients = set()

async def ws_handler(request):
    ws = web.WebSocketResponse()
    await ws.prepare(request)
    clients.add(ws)
    await ws.send_str("auth_ok")
    print(f"[+] connected ({len(clients)} total)")
    async for msg in ws:
        if msg.type == WSMsgType.TEXT:
            print(f"[app] {msg.data}")
    clients.discard(ws)
    print(f"[-] disconnected ({len(clients)} total)")
    return ws

async def register(request):
    await request.read()
    print("[reg] /api/register")
    return web.Response(text="ok")

async def stdin_loop():
    loop = asyncio.get_event_loop()
    while True:
        line = await loop.run_in_executor(None, sys.stdin.readline)
        line = line.strip()
        if not line:
            continue
        parts = line.split(":", 2)
        type_, name, msg = [p.strip() for p in parts] if len(parts) == 3 else ("test", "test", line)
        payload = json.dumps({
            "entityFriendlyName": name,
            "type": type_,
            "msg": msg,
            "timestamp": int(time.time())
        })
        for ws in list(clients):
            await ws.send_str(payload)
        print(f"[>>] {len(clients)} client(s): {payload}")

async def main():
    app = web.Application()
    app.router.add_get("/ws/notifications", ws_handler)
    app.router.add_post("/api/register", register)
    runner = web.AppRunner(app)
    await runner.setup()
    await web.TCPSite(runner, "0.0.0.0", 8080).start()
    print("WS dev server :8080")
    print("Type: message  or  type:friendlyName:message")
    print("### To send heartbeat msg, aka ping, type: ws_ping:ping:ping")
    print("### To send move type: movement:movement:backyard")
    print("### To send gate opened type: gate:garage gate:entry")
    print("### To send gate opened type: gate:garage gate:closed")

    await stdin_loop()

asyncio.run(main())
