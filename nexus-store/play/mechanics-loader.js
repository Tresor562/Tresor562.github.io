import m01 from './chunks/m01.js';
import m02 from './chunks/m02.js';
import m03 from './chunks/m03.js';
import m04 from './chunks/m04.js';
import m05 from './chunks/m05.js';
import m06 from './chunks/m06.js';
import m07 from './chunks/m07.js';
import m08 from './chunks/m08.js';
import m09 from './chunks/m09.js';
import m10 from './chunks/m10.js';
import m11 from './chunks/m11.js';
import m12 from './chunks/m12.js';
import m13 from './chunks/m13.js';
import m14 from './chunks/m14.js';
let cached;export async function loadMechanics(){if(cached)return cached;const b64=[m01,m02,m03,m04,m05,m06,m07,m08,m09,m10,m11,m12,m13,m14].join('');const bin=Uint8Array.from(atob(b64),c=>c.charCodeAt(0));const ds=new DecompressionStream('gzip');const text=await new Response(new Blob([bin]).stream().pipeThrough(ds)).text();const url=URL.createObjectURL(new Blob([text],{type:'text/javascript'}));try{cached=await import(url);return cached}finally{setTimeout(()=>URL.revokeObjectURL(url),1000)}}