const ROWS=6,COLS=7;
const createBoard=()=>Array.from({length:ROWS},()=>Array(COLS).fill(0));
const directions=[[1,0],[0,1],[1,1],[1,-1]];
function winner(board,row,col,player){
 for(const [dr,dc] of directions){
  let total=1;
  for(const sign of [-1,1]){for(let n=1;n<4;n++){const r=row+dr*n*sign,c=col+dc*n*sign;if(r<0||r>=ROWS||c<0||c>=COLS||board[r][c]!==player)break;total++;}}
  if(total>=4)return true;
 }
 return false;
}
export async function createGame(env){
 const {canvas,ctx,sfx}=env;let board,turn,over,raf;
 const reset=()=>{board=createBoard();turn=1;over=false};
 const resize=()=>{const r=canvas.getBoundingClientRect(),d=Math.min(globalThis.devicePixelRatio||1,2);if(canvas.width!==Math.floor(r.width*d)||canvas.height!==Math.floor(r.height*d)){canvas.width=Math.floor(r.width*d);canvas.height=Math.floor(r.height*d);ctx.setTransform(d,0,0,d,0,0)}return r};
 const layout=r=>{const boardW=Math.min(r.width-28,560),cell=boardW/COLS,boardH=cell*ROWS;return{x:(r.width-boardW)/2,y:96,w:boardW,h:boardH,cell}};
 const drop=col=>{if(over||col<0||col>=COLS)return false;for(let row=ROWS-1;row>=0;row--){if(board[row][col]===0){board[row][col]=turn;sfx?.tap?.();if(winner(board,row,col,turn)){over=true;sfx?.score?.();const who=turn;setTimeout(()=>env.end?.(true,`Player ${who} wins`),220)}else if(board.every(r=>r.every(Boolean))){over=true;setTimeout(()=>env.end?.(true,'Draw'),220)}else turn=turn===1?2:1;return true}}return false};
 const down=e=>{const r=canvas.getBoundingClientRect(),p={x:e.clientX-r.left,y:e.clientY-r.top},l=layout(r);if(p.x<l.x||p.x>l.x+l.w||p.y<l.y-32||p.y>l.y+l.h)return;drop(Math.min(COLS-1,Math.max(0,Math.floor((p.x-l.x)/l.cell))))};
 const render=()=>{const r=resize(),l=layout(r);ctx.clearRect(0,0,r.width,r.height);const bg=ctx.createLinearGradient(0,0,r.width,r.height);bg.addColorStop(0,'#0b1020');bg.addColorStop(1,'#211742');ctx.fillStyle=bg;ctx.fillRect(0,0,r.width,r.height);ctx.textAlign='center';ctx.fillStyle='#fff';ctx.font='800 22px system-ui';ctx.fillText('FOUR IN A ROW DUEL',r.width/2,34);ctx.font='600 13px system-ui';ctx.fillStyle='#b9c4e8';ctx.fillText(over?'Round complete':`Player ${turn} · choose a column`,r.width/2,58);ctx.fillStyle='#3156d8';ctx.fillRect(l.x,l.y,l.w,l.h);for(let row=0;row<ROWS;row++)for(let col=0;col<COLS;col++){const cx=l.x+col*l.cell+l.cell/2,cy=l.y+row*l.cell+l.cell/2,rad=l.cell*.36;ctx.beginPath();ctx.arc(cx,cy,rad,0,Math.PI*2);ctx.fillStyle=board[row][col]===1?'#ffca3a':board[row][col]===2?'#ff595e':'#10172c';ctx.fill();ctx.strokeStyle='rgba(255,255,255,.16)';ctx.lineWidth=2;ctx.stroke()}raf=requestAnimationFrame(render)};
 reset();canvas.addEventListener('pointerdown',down);raf=requestAnimationFrame(render);return{restart:reset,drop,getState:()=>({board:board.map(r=>[...r]),turn,over}),destroy(){cancelAnimationFrame(raf);canvas.removeEventListener('pointerdown',down)}};
}
export default{createGame};
