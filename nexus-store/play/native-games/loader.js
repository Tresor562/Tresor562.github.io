const loaders=new Map([
  [28,()=>import('./28-four-in-a-row-duel.js')],
  [30,()=>import('./30-tic-tac-clash.js')],
  [52,()=>import('./52-pop-it-fever.js')],
  [55,()=>import('./55-color-sort-master.js')],
  [56,()=>import('./56-tower-stack-challenge.js')],
]);

export const nativeGameIds=new Set(loaders.keys());
export const isNativeGame=id=>nativeGameIds.has(Number(id));

export async function loadNativeGame(id){
  const load=loaders.get(Number(id));
  if(!load)return null;
  const mod=await load();
  if(typeof mod.createGame!=='function')throw new TypeError(`Native game ${id} does not export createGame()`);
  return mod.createGame;
}
