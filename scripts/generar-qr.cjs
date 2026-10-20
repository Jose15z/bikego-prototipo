// Generate machine-readable QR images from the same catalog used by the backend.
const fs = require('node:fs');
const path = require('node:path');
const {createRequire} = require('node:module');
const root = path.resolve(__dirname, '..');
const frontend = createRequire(path.join(root, 'frontend/package.json'));
const test = createRequire(path.join(root, 'tests/e2e/package.json'));
const React = frontend('react');
const {renderToStaticMarkup} = frontend('react-dom/server');
const {QRCodeSVG} = frontend('qrcode.react');
let sharp;
try { sharp = test('sharp'); } catch { sharp = require('sharp'); }
const {RGBLuminanceSource,HybridBinarizer,BinaryBitmap,QRCodeReader}=require(path.join(root,'frontend/node_modules/html5-qrcode/third_party/zxing-js.umd.js'));
const catalog = JSON.parse(fs.readFileSync(path.join(root,'backend/src/main/resources/demo/catalogo.json'),'utf8'));
const dirs = ['docs/qr-prueba','frontend/public/qr-prueba'].map(p=>path.join(root,p));
for(const dir of dirs) fs.mkdirSync(dir,{recursive:true});
(async()=>{
 for(const b of [...catalog.bicicletas,{id:'QR-AJENO',contenido:'https://ejemplo.com/cupon-123'}]){
  const value=b.contenido||`BIKEGO:${b.id}`;
  let png;
  // A valid QR can contain patterns that confuse a particular reader. Verify the
  // exact raster with the app's decoder and choose a readable encoding variant.
  for(const encoding of [{level:'M',boostLevel:true},{level:'H',boostLevel:false},{level:'M',boostLevel:false},{level:'L',boostLevel:false}]){
   const svg=renderToStaticMarkup(React.createElement(QRCodeSVG,{value,size:320,marginSize:4,...encoding,bgColor:'#ffffff',fgColor:'#000000'}));
   const modules=Number(svg.match(/viewBox="0 0 (\d+) /)[1]);
   const dimension=modules*12;
   const aligned=svg.replace('width="320"',`width="${dimension}"`).replace('height="320"',`height="${dimension}"`);
   const candidate=await sharp(Buffer.from(aligned)).png().toBuffer();
   const {data,info}=await sharp(candidate).removeAlpha().greyscale().raw().toBuffer({resolveWithObject:true});
   try {
    const decoded=new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(new Uint8ClampedArray(data),info.width,info.height)))).getText();
    if(decoded===value){png=candidate;break;}
   }catch{ /* Try the next standards-compliant encoding of the same content. */ }
  }
  if(!png)throw new Error(`No se pudo generar un QR legible para ${b.id}`);
  for(const dir of dirs) fs.writeFileSync(path.join(dir,`${b.id}.png`),png);
 }
 for(const dir of dirs) fs.writeFileSync(path.join(dir,'catalogo.json'),JSON.stringify(catalog,null,2)+'\n');
 const escape=s=>s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const html='<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>QR BikeGo por estación</title><style>body{font:16px system-ui;margin:32px;color:#000}section{break-before:auto}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:20px}figure{margin:0;padding:12px;border:1px solid #ddd;break-inside:avoid}img{width:160px;height:160px}figcaption{display:grid;gap:8px}a{color:#000}</style><h1>Códigos de bicicletas BikeGo</h1><p>'+catalog.bicicletas.length+' bicicletas, 10 por estación. Consulta la ubicación y disponibilidad actual en la aplicación.</p>'+catalog.estaciones.map(e=>`<section><h2>${escape(e.nombre)}</h2><div class="grid">`+catalog.bicicletas.filter(b=>b.estacionId===e.id).map(b=>`<figure><img src="${b.id}.png" alt="Código QR ${b.id}"><figcaption><strong>${b.id}</strong><a href="${b.id}.png" download>Descargar PNG</a></figcaption></figure>`).join('')+'</div></section>').join('')+'</html>';
 for(const dir of dirs) fs.writeFileSync(path.join(dir,'index.html'),html);
 fs.writeFileSync(path.join(dirs[0],'INDICE.csv'),'Bicicleta,Estacion,Contenido QR,Archivo\n'+catalog.bicicletas.map(b=>`${b.id},${catalog.estaciones.find(e=>e.id===b.estacionId).nombre},BIKEGO:${b.id},${b.id}.png`).join('\n')+'\n');
 console.log(`Generados ${catalog.bicicletas.length} QR de bicicleta y un QR ajeno, galería e índice.`);
})().catch(e=>{console.error(e);process.exit(1)});
