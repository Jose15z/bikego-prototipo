"""Checks access points against independently captured OSM venue/building polygons."""
import json, math
from pathlib import Path
root=Path(__file__).resolve().parents[2]
catalog=json.loads((root/'backend/src/main/resources/demo/catalogo.json').read_text())
polygons=json.loads((root/'tests/fixtures/recintos-osm.json').read_text())['recintos']
def inside(lat,lng,polygon):
    contained=False
    for (y1,x1),(y2,x2) in zip(polygon,polygon[1:]+polygon[:1]):
        if (y1>lat)!=(y2>lat) and lng<(x2-x1)*(lat-y1)/(y2-y1)+x1:
            contained=not contained
    return contained
checks=[]
for station in catalog['estaciones']:
    polygon=next((p['poligono'] for p in polygons if p['estacionId']==station['id']),None)
    if polygon is None:continue
    assert not inside(station['latitud'],station['longitud'],polygon),station['id']
    assert station['referencia'].strip(),station['id']
    for bike in [b for b in catalog['bicicletas'] if b['estacionId']==station['id']]:
        lat=station['latitud']+bike['deltaLatitud'];lng=station['longitud']+bike['deltaLongitud']
        assert not inside(lat,lng,polygon),bike['id']
        assert math.hypot(bike['deltaLatitud'],bike['deltaLongitud'])*111320<8,bike['id']
    checks.append({'estacion':station['id'],'nombre':station['nombre'],'resultado':'APROBADO','verificacion':'Punto y bicicletas fuera del recinto, bicicletas a menos de 8 m'})
# Validate new corridors against independent OSM road geometry and building footprints.
new=json.loads((root/'tests/fixtures/ampliacion-vias-osm.json').read_text())
def distance_to_road(lat,lng,line):
    cos=math.cos(math.radians(lat));best=float('inf')
    for a,b in zip(line,line[1:]):
        ax=(a['lon']-lng)*111320*cos;ay=(a['lat']-lat)*111320
        bx=(b['lon']-lng)*111320*cos;by=(b['lat']-lat)*111320
        dx,dy=bx-ax,by-ay;den=dx*dx+dy*dy
        if not den:continue
        t=max(0,min(1,-(ax*dx+ay*dy)/den));best=min(best,math.hypot(ax+t*dx,ay+t*dy))
    return best
for road in new['vias']:
    station=next(s for s in catalog['estaciones'] if s['id']==road['estacionId'])
    assert station['osm']==road['osm']
    assert 5<distance_to_road(station['latitud'],station['longitud'],road['geometry'])<20
    assert road['tags'].get('bridge')!='yes' and road['tags']['highway']!='construction'
    for b in [b for b in catalog['bicicletas'] if b['estacionId']==station['id']]:
        lat=station['latitud']+b['deltaLatitud'];lng=station['longitud']+b['deltaLongitud']
        assert math.hypot(b['deltaLatitud'],b['deltaLongitud'])*111320<8
        for building in new['edificios']:
            poly=[[g['lat'],g['lon']] for g in building['geometry']]
            assert not inside(lat,lng,poly),(b['id'],building['id'])
    checks.append({'estacion':station['id'],'nombre':station['nombre'],'resultado':'APROBADO','verificacion':'Junto al corredor correcto, fuera de edificios, bicicletas a menos de 8 m'})
# New landmark pickup points remain outside venue/building outlines and near the right access road.
landmarks=json.loads((root/'tests/fixtures/lugares-accesos-osm.json').read_text())['lugares']
for place in landmarks:
    station=next(s for s in catalog['estaciones'] if s['id']==place['id'])
    assert station['osm']==place['osm'] and station['referencia']
    road=place['road'];assert 5<distance_to_road(station['latitud'],station['longitud'],road['geometry'])<25
    assert road['tags'].get('bridge')!='yes' and road['tags']['highway']!='construction'
    anchor=place['anchor'];assert math.hypot(station['latitud']-anchor['latitud'],(station['longitud']-anchor['longitud'])*math.cos(math.radians(station['latitud'])))*111320<350
    for b in [b for b in catalog['bicicletas'] if b['estacionId']==station['id']]:
        lat=station['latitud']+b['deltaLatitud'];lng=station['longitud']+b['deltaLongitud']
        assert math.hypot(b['deltaLatitud'],b['deltaLongitud'])*111320<8
        if place['venue']:assert not inside(lat,lng,[[g['lat'],g['lon']] for g in place['venue']]),b['id']
        for pitch in place.get('sports',[]):assert not inside(lat,lng,[[g['lat'],g['lon']] for g in pitch]),b['id']
        for building in place['buildings']:assert not inside(lat,lng,[[g['lat'],g['lon']] for g in building['geometry']]),(b['id'],building['id'])
    checks.append({'estacion':station['id'],'nombre':station['nombre'],'resultado':'APROBADO','verificacion':'Junto al lugar solicitado y vía de acceso; bicicletas fuera del recinto y edificios'})
# The corridor stop lies between the bridge and Torres, with clearance from the bridge.
corridor=next(p for p in landmarks if p['id']=='EST-29');stop=next(s for s in catalog['estaciones'] if s['id']=='EST-29')
a=corridor['extremos']['puente'];b=corridor['extremos']['torres'];cos=math.cos(math.radians(stop['latitud']))
ax,ay=float(a['lon'])*cos,float(a['lat']);bx,by=float(b['lon'])*cos,float(b['lat']);x,y=stop['longitud']*cos,stop['latitud']
fraction=((x-ax)*(bx-ax)+(y-ay)*(by-ay))/((bx-ax)**2+(by-ay)**2)
assert .2<fraction<.85
for bike in [b for b in catalog['bicicletas'] if b['estacionId']=='EST-29']:
    lat=stop['latitud']+bike['deltaLatitud'];lng=stop['longitud']+bike['deltaLongitud']
    for bridge in corridor['bridges']:
        assert distance_to_road(lat,lng,bridge['geometry'])>15,bike['id']
        if bridge['geometry'][0]==bridge['geometry'][-1]:assert not inside(lat,lng,[[g['lat'],g['lon']] for g in bridge['geometry']])
assert len(checks)==52 and len(catalog['bicicletas'])==520
# Independent commune boundaries must contain at least one station per commune.
communes=json.loads((root/'tests/fixtures/comunas-osm.json').read_text())['comunas']
def in_boundary(station,geometry):
    polygons=[geometry['coordinates']] if geometry['type']=='Polygon' else geometry['coordinates']
    def in_ring(ring):return inside(station['latitud'],station['longitud'],[[y,x] for x,y in ring])
    return any(in_ring(p[0]) and not any(in_ring(h) for h in p[1:]) for p in polygons)
coverage=[]
assert sorted(c['numero'] for c in communes)==list(range(1,11))
for commune in communes:
    covered=[s['id'] for s in catalog['estaciones'] if in_boundary(s,commune['geometry'])]
    assert covered,commune['nombre']
    assert covered==commune['estaciones'],commune['nombre']
    coverage.append({'comuna':commune['numero'],'nombre':commune['nombre'],'estaciones':covered})
assert len({s['estacion'] for s in checks})==len(catalog['estaciones'])
assert len({b['id'] for b in catalog['bicicletas']})==520
assert all(sum(b['estacionId']==s['id'] for b in catalog['bicicletas'])==10 for s in catalog['estaciones'])
# Fill real gaps on the named avenue, not duplicate nearby existing stops.
for stop in [s for s in catalog['estaciones'] if s['id'] in ['EST-43','EST-44','EST-45','EST-46']]:
    place=next(p for p in landmarks if p['id']==stop['id'])
    assert place['road']['tags']['name']=='Avenida 19 de Enero'
    for previous in catalog['estaciones'][:42]:
        distance=math.hypot(stop['latitud']-previous['latitud'],(stop['longitud']-previous['longitud'])*math.cos(math.radians(stop['latitud'])))*111320
        assert distance>400,(stop['id'],previous['id'])
jardines=next(s for s in catalog['estaciones'] if s['id']=='EST-47')
assert math.hypot(jardines['latitud']-4.5072408,(jardines['longitud']+75.6906614)*math.cos(math.radians(jardines['latitud'])))*111320<30
print(json.dumps({'estaciones':checks,'cobertura_comunas':coverage,'bicicletas_verificadas':len(catalog['bicicletas']),'fallos':0},ensure_ascii=False,indent=2))
