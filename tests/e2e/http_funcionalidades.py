import json,urllib.request,urllib.error,time,statistics,os
from datetime import datetime, timezone
from pathlib import Path
BASE=os.environ.get('BIKEGO_TEST_URL','http://127.0.0.1:8083');checks=[]
def call(path,method='GET',data=None,token=None,expected=200):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 req=urllib.request.Request(BASE+path,method=method,headers=headers,data=json.dumps(data).encode() if data is not None else None)
 start=time.perf_counter()
 try:
  with urllib.request.urlopen(req,timeout=25) as res:status=res.status;raw=res.read();ct=res.headers.get('Content-Type','')
 except urllib.error.HTTPError as e:status=e.code;raw=e.read();ct=e.headers.get('Content-Type','')
 assert status==expected,(path,status,raw.decode()[:200])
 return (json.loads(raw) if raw and 'json' in ct else raw),time.perf_counter()-start

def check(name):checks.append({'verificacion':name,'resultado':'APROBADO'});print('APROBADO',name)
html,_=call('/');assert b'root' in html;check('Interfaz React incluida en JAR')
reg={'nombre':'QA Parcial Dos','documento':'QA20261019','correo':'qa.parcial2@bikego.local','telefono':'3001234567','fechaNacimiento':'2000-01-01','direccion':'Armenia prueba local','password':'PruebaParcialDos123','aceptaTerminos':True,'aceptaPrivacidad':True}
r,_=call('/api/auth/registro','POST',reg,expected=201);assert r['codigoDesarrollo'];check('Registro con cuenta pendiente y codigo local')
call('/api/auth/verificacion','POST',{'correo':reg['correo'],'codigo':r['codigoDesarrollo']},expected=204);check('Verificacion de cuenta')
a,_=call('/api/auth/login','POST',{'correo':reg['correo'],'password':reg['password']});t=a['token'];check('Inicio de sesion autenticada')
call('/api/usuarios/me/metodo-pago','PUT',{'numeroTarjeta':'4111111111111111','vencimiento':'12/29','titular':reg['nombre']},t);check('Tokenizacion y validacion de tarjeta de prueba')
call('/api/usuarios/me/validar-pago','POST',token=t);check('Validacion de pago sin deuda')
times=[]
for _ in range(20):
 b,sec=call('/api/bicicletas/cercanas?lat=4.5325&lng=-75.6736&radio=1000');times.append(sec)
assert len([x for x in times if x<=3])>=19
assert any(x['id']=='BG-001' for x in b['bicicletas']);assert all(x['bateria']>=20 for x in b['bicicletas']);check('Mapa 20 consultas HTTP y umbral local de tres segundos')
v,_=call('/api/alquileres','POST',{'bicicletaSeleccionadaId':'BG-001','codigoQr':'BIKEGO:BG-001'},t,201);vid=v['idViaje'];check('Inicio de alquiler con QR y candado simulado')
vs,_=call('/api/viajes',token=t);assert any(x['idViaje']==vid and x['estado']=='EN_CURSO' for x in vs);check('Recuperacion de viaje activo')
call(f'/api/viajes/{vid}/finalizar','POST',{'latitud':4.6,'longitud':-75.6,'precision':5},t,409);check('Rechazo de devolucion fuera de zona')
call(f'/api/viajes/{vid}/finalizar','POST',{'latitud':4.5325,'longitud':-75.6736,'precision':5},t);check('Devolucion con zona y confirmacion de bloqueo')
r,sec=call(f'/api/viajes/{vid}/pago','POST',token=t);assert r['estadoPago']=='PROCESADO';check('Pago y recibo definitivo')
r2,_=call(f'/api/viajes/{vid}/pago','POST',token=t);assert r2==r;check('Reintento de cobro idempotente')
ms,_=call('/api/mensajes',token=t);assert any(r['numero'] in m['contenido'] for m in ms);check('Entrega de recibo en bandeja privada')
call('/api/flota',token=t,expected=403);check('Cliente sin permiso de flota')
o,_=call('/api/auth/login','POST',{'correo':'operador@bikego.local','password':'BikeGo123!'});ot=o['token']
e,_=call('/api/mantenimientos','POST',{'bicicletaId':'BG-004','tipo':'Preventivo','descripcion':'Revision integral para sustentacion','latitud':4.5405,'longitud':-75.66655},ot);check('Registro de mantenimiento como operador')
call(f'/api/mantenimientos/{e["id"]}/cerrar','POST',{'bateria':15},ot,400);check('No permite cerrar mantenimiento con bateria 15 por ciento')
call(f'/api/mantenimientos/{e["id"]}/cerrar','POST',{'bateria':60},ot);check('Cierre de mantenimiento con bateria 60 por ciento')
call('/api/auth/logout','POST',token=t,expected=204);call('/api/viajes',token=t,expected=401);check('Revocacion efectiva de sesion')
out={'fecha':datetime.now(timezone.utc).isoformat(),'entorno':'JAR integrado; H2 temporal y adaptadores simulados', 'base':BASE,'verificaciones':checks,'resumen':{'total':len(checks),'fallos':0,'mapa_http_20_consultas_p95_ms':round(sorted(times)[18]*1000,2),'pago_local_ms':round(sec*1000,2)}}
Path(os.environ.get('BIKEGO_HTTP_OUTPUT','/tmp/bikego-qa-actual/Resultados_HTTP.json')).write_text(json.dumps(out,ensure_ascii=False,indent=2));print(out['resumen'])
