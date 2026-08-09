const nativeFetch=window.fetch.bind(window);
window.fetch=(input,init)=>{
  if(typeof input==='string' && input.startsWith('./')) return nativeFetch(new URL(input,import.meta.url),init);
  return nativeFetch(input,init);
};
await import('./app-direct.js');