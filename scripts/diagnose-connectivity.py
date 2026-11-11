"""Read-only MySQL and HTTP probes; no business writes, retries, or server changes."""

import argparse
import concurrent.futures
import contextlib
import datetime as dt
import json
import os
from pathlib import Path
import socket
import socketserver
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

import pymysql


def arguments():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--database', default='supermarket_inventory_test')
    parser.add_argument('--backend-url', default='http://127.0.0.1:8080')
    parser.add_argument('--samples', type=int, default=60)
    parser.add_argument('--interval', type=float, default=1)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--echo', action='store_true',
                        help='Also probe a temporary independent loopback TCP echo server')
    args = parser.parse_args()
    url = urllib.parse.urlsplit(args.backend_url)
    if (url.scheme not in ('http', 'https') or not url.hostname or url.username
            or url.password or url.query or url.fragment or url.path not in ('', '/')):
        parser.error('--backend-url must be an HTTP(S) origin without credentials')
    if not 1 <= args.samples <= 300 or not 0.1 <= args.interval <= 10:
        parser.error('samples must be 1..300 and interval must be 0.1..10 seconds')
    if not os.environ.get('DB_PASSWORD'):
        parser.error('Set DB_PASSWORD in the local environment; do not pass passwords as arguments')
    return args


def mysql_probe(args):
    started = time.perf_counter()
    phase = 'connect'
    result = {'kind': 'mysql'}
    try:
        # A fresh connection distinguishes handshake failures from pooled-session expiry.
        with pymysql.connect(
            host=os.environ.get('DB_HOST', '127.0.0.1'),
            port=int(os.environ.get('DB_PORT', '3306')),
            user=os.environ.get('DB_USER', 'root'), password=os.environ['DB_PASSWORD'],
            database=args.database, connect_timeout=3, read_timeout=3, write_timeout=3,
        ) as connection:
            phase = 'query'
            with connection.cursor() as cursor:
                cursor.execute('SELECT 1')
                if cursor.fetchone() != (1,):
                    raise RuntimeError('Unexpected SELECT 1 result')
                cursor.execute("SHOW GLOBAL STATUS WHERE Variable_name IN "
                               "('Threads_connected','Threads_running','Aborted_connects',"
                               "'Connection_errors_max_connections')")
                result['status'] = dict(cursor.fetchall())
        result['ok'] = True
    except Exception as error:
        result.update(ok=False, phase=phase, error=type(error).__name__)
        # No passwords, SQL parameters, user rows, tokens, or provider settings in reports.
        if isinstance(error, pymysql.MySQLError) and error.args:
            result['mysqlCode'] = error.args[0]
    result['ms'] = round((time.perf_counter() - started) * 1000, 2)
    return result


def http_probe(args):
    started = time.perf_counter()
    result = {'kind': 'http'}
    try:
        # Ignore system HTTP proxies: this measures the specified backend's transport.
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        try:
            with opener.open(args.backend_url.rstrip('/') + '/api/v1/auth/me', timeout=3) as response:
                response.read()
                result['status'] = response.status
        except urllib.error.HTTPError as response:
            with response:
                response.read()
                result['status'] = response.code
        result['ok'] = result['status'] == 401
    except Exception as error:
        result.update(ok=False, phase='http', error=type(error).__name__)
    result['ms'] = round((time.perf_counter() - started) * 1000, 2)
    return result


class EchoHandler(socketserver.BaseRequestHandler):
    def handle(self):
        self.request.settimeout(3)
        try:
            while data := self.request.recv(64):
                self.request.sendall(data)
        except (TimeoutError, OSError):
            pass  # A disconnected diagnostic client must not leave a worker blocked.


class EchoServer(socketserver.ThreadingTCPServer):
    daemon_threads = True


def echo_probe(args):
    started = time.perf_counter()
    phase = 'tcp'
    result = {'kind': 'independentEcho'}
    try:
        with socket.create_connection(('127.0.0.1', args.echo_port), timeout=3) as connection:
            phase = 'echo'
            connection.sendall(b'inventory-connectivity')
            received = b''
            while len(received) < len(b'inventory-connectivity'):
                part = connection.recv(64)
                if not part:
                    break
                received += part
            if received != b'inventory-connectivity':
                raise RuntimeError('Unexpected echo response')
        result['ok'] = True
    except Exception as error:
        result.update(ok=False, phase=phase, error=type(error).__name__)
    result['ms'] = round((time.perf_counter() - started) * 1000, 2)
    return result


def main():
    args = arguments()
    samples = []
    probes = [mysql_probe, http_probe]
    with contextlib.ExitStack() as cleanup:
        if args.echo:
            server = cleanup.enter_context(EchoServer(('127.0.0.1', 0), EchoHandler))
            threading.Thread(target=server.serve_forever, daemon=True).start()
            cleanup.callback(server.shutdown)
            args.echo_port = server.server_address[1]
            probes.append(echo_probe)
        executor = cleanup.enter_context(concurrent.futures.ThreadPoolExecutor(max_workers=len(probes)))
        for index in range(args.samples):
            timestamp = dt.datetime.now().astimezone().isoformat()
            pending = [executor.submit(probe, args) for probe in probes]
            for future in pending:
                sample = {'index': index, 'at': timestamp, **future.result()}
                samples.append(sample)
                if not sample['ok']:
                    print(json.dumps(sample), flush=True)
            if index + 1 < args.samples:
                time.sleep(args.interval)
    summary = {}
    kinds = sorted({sample['kind'] for sample in samples})
    for kind in kinds:
        items = [sample for sample in samples if sample['kind'] == kind]
        summary[kind] = {'samples': len(items), 'failed': sum(not item['ok'] for item in items),
                         'maxMs': max(item['ms'] for item in items)}
    connected = [int(sample['status']['Threads_connected']) for sample in samples
                 if sample['kind'] == 'mysql' and sample['ok']]
    if connected:
        summary['threadsConnectedPeak'] = max(connected)
    output = args.output or Path('.local/diagnostics') / (
        'connectivity-' + dt.datetime.now().strftime('%Y%m%d-%H%M%S') + '.json')
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({'summary': summary, 'samples': samples}, indent=2), encoding='utf-8')
    print(json.dumps({'summary': summary, 'report': str(output)}))
    return int(any(summary[kind]['failed'] for kind in kinds))


if __name__ == '__main__':
    raise SystemExit(main())
