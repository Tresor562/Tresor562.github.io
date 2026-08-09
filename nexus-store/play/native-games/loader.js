const bespoke=new Map([
  [28,()=>import('./28-four-in-a-row-duel.js')],
  [30,()=>import('./30-tic-tac-clash.js')],
  [31,()=>import('./31-melon-target-challenge.js')],
  [32,()=>import('./32-find-the-ball-duel.js')],
  [33,()=>import('./33-cash-grab-duel.js')],
  [34,()=>import('./34-lily-pad-hop-duel.js')],
  [42,()=>import('./42-memory-flip-clash.js')],
  [49,()=>import('./49-nail-pull-puzzle.js')],
  [50,()=>import('./50-hoop-pup-challenge.js')],
  [51,()=>import('./51-bee-dodge-garden.js')],
  [52,()=>import('./52-pop-it-fever.js')],
  [53,()=>import('./53-block-blast-quest.js')],
  [54,()=>import('./54-night-alone-escape.js')],
  [55,()=>import('./55-color-sort-master.js')],
  [56,()=>import('./56-tower-stack-challenge.js')],
  [58,()=>import('./58-save-the-line-puzzle.js')],
  [64,()=>import('./64-blow-it-up-chain.js')],
]);
const genericLoader=id=>()=>import('./universal-native.js').then(mod=>({createGame:env=>mod.createConfiguredNativeGame(env,id)}));
const loaders=new Map(bespoke);
for(let id=1;id<=64;id++)if(!loaders.has(id))loaders.set(id,genericLoader(id));
export const nativeGameIds=new Set(loaders.keys());
export const isNativeGame=id=>nativeGameIds.has(Number(id));
export async function loadNativeGame(id){const load=loaders.get(Number(id));if(!load)return null;const mod=await load();if(typeof mod.createGame!=='function')throw new TypeError(`Native game ${id} does not export createGame()`);return mod.createGame}
