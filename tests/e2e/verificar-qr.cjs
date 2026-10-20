const fs=require('node:fs'),path=require('node:path');
const {createRequire}=require('node:module');
const root=process.env.BIKEGO_ROOT||path.resolve(__dirname,'../..');
const local=createRequire(path.join(root,'tests/e2e/package.json'));
let PNG;try{PNG=local('pngjs').PNG}catch{PNG=require('pngjs').PNG}
const {RGBLuminanceSource,HybridBinarizer,BinaryBitmap,QRCodeReader}=require(path.join(root,'frontend/node_modules/html5-qrcode/third_party/zxing-js.umd.js'));
const catalog=JSON.parse(fs.readFileSync(path.join(root,'backend/src/main/resources/demo/catalogo.json')));
const checks=[];
for(const dir of ['docs/qr-prueba','frontend/public/qr-prueba']){
 for(const b of [...catalog.bicicletas,{id:'QR-AJENO',contenido:'https://ejemplo.com/cupon-123'}]){
  const img=PNG.sync.read(fs.readFileSync(path.join(root,dir,`${b.id}.png`)));
  const gray=new Uint8ClampedArray(img.width*img.height);
  for(let i=0;i<gray.length;i++)gray[i]=(img.data[i*4]+2*img.data[i*4+1]+img.data[i*4+2])/4;
  let read;try {read=new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(gray,img.width,img.height)))).getText();} catch(e){throw new Error(`${dir}/${b.id}: ${e.constructor.name}`)}
  if(read!==(b.contenido||`BIKEGO:${b.id}`))throw new Error(dir+'/'+b.id+' contenido incorrecto');
  checks.push({archivo:dir+'/'+b.id+'.png',contenido:read,resultado:'APROBADO'});
 }
}
fs.writeFileSync(process.env.BIKEGO_QR_OUTPUT||'/tmp/bikego-qr-resultados.json',JSON.stringify({total:checks.length,fallos:0,checks},null,2));
console.log(`${checks.length} imágenes QR decodificadas correctamente: ${catalog.bicicletas.length} bicicletas y un QR ajeno en cada ubicación.`);
