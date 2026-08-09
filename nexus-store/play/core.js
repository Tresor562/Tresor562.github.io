export const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
export const lerp=(a,b,t)=>a+(b-a)*t;
export const rand=(a,b)=>a+Math.random()*(b-a);
export const dist=(a,b)=>Math.hypot(a.x-b.x,a.y-b.y);
export const hit=(a,b,r=20)=>dist(a,b)<r;
export const TAU=Math.PI*2;

const safeParse=(raw,fallback)=>{try{return JSON.parse(raw)||fallback}catch{return fallback}};
const emptyState=()=>({coins:0,stats:{},theme:'system',sound:true});

export class Store {
  constructor(sync){this.sync=sync;this.data=emptyState();this.mode='guest';this.user=null;this.listeners=new Set();}
  async init(){
    const initial=await this.sync.init();
    this.mode=initial.mode;this.user=initial.user;this.data={...emptyState(),...initial.state};
    this.sync.onChange(({mode,user,state})=>{this.mode=mode;this.user=user;this.data={...emptyState(),...state};this.emit()});
    return this;
  }
  onChange(fn){this.listeners.add(fn);return()=>this.listeners.delete(fn)}
  emit(){for(const fn of this.listeners)try{fn(this)}catch{}}
  savePrefs(){this.sync.savePrefs({theme:this.data.theme,sound:this.data.sound});}
  coins(){return Number(this.data.coins||0)}
  stat(id){return this.data.stats?.[id]||{played:0,wins:0,level:1,best:0,stars:0}}
  async result(id,won,matchId){
    const next=await this.sync.recordResult({gameId:id,won,matchId});
    this.mode=next.mode;this.user=next.user;this.data={...emptyState(),...next.state};this.emit();return this.data;
  }
  setTheme(theme){this.data.theme=theme;this.savePrefs();this.emit()}
  setSound(v){this.data.sound=!!v;this.savePrefs();this.emit()}
}

export class Sfx {
  constructor(store){this.store=store;this.ctx=null}
  get on(){return this.store.data.sound!==false}
  toggle(){this.store.setSound(!this.on);return this.on}
  beep(freq=440,dur=.06,type='sine',gain=.055){if(!this.on)return;try{this.ctx ||= new (window.AudioContext||window.webkitAudioContext)();const o=this.ctx.createOscillator(),g=this.ctx.createGain();o.type=type;o.frequency.value=freq;g.gain.setValueAtTime(gain,this.ctx.currentTime);g.gain.exponentialRampToValueAtTime(.0001,this.ctx.currentTime+dur);o.connect(g);g.connect(this.ctx.destination);o.start();o.stop(this.ctx.currentTime+dur)}catch{}}
  tap(){this.beep(280,.035,'sine',.035)} score(){this.beep(620,.055,'triangle',.05);setTimeout(()=>this.beep(830,.05,'triangle',.04),45)}
  win(){this.beep(523,.08,'triangle',.06);setTimeout(()=>this.beep(659,.08,'triangle',.06),90);setTimeout(()=>this.beep(784,.13,'triangle',.06),180)} fail(){this.beep(190,.12,'sawtooth',.035)}
}
export function vibrate(ms=20){try{navigator.vibrate?.(ms)}catch{}}
export function rr(ctx,x,y,w,h,r=12){r=Math.min(r,w/2,h/2);ctx.beginPath();ctx.roundRect?.(x,y,w,h,r);if(!ctx.roundRect)ctx.rect(x,y,w,h)}
export function text(ctx,t,x,y,size=18,color='#fff',align='center',weight=800){ctx.save();ctx.fillStyle=color;ctx.textAlign=align;ctx.textBaseline='middle';ctx.font=`${weight} ${size}px system-ui,-apple-system,Segoe UI,sans-serif`;ctx.fillText(t,x,y);ctx.restore()}
export function circle(ctx,x,y,r,c){ctx.beginPath();ctx.arc(x,y,r,0,TAU);ctx.fillStyle=c;ctx.fill()}
export function line(ctx,x1,y1,x2,y2,c='#fff',w=3){ctx.beginPath();ctx.moveTo(x1,y1);ctx.lineTo(x2,y2);ctx.strokeStyle=c;ctx.lineWidth=w;ctx.lineCap='round';ctx.stroke()}
export function bg(ctx,w,h,a='#171b2a',b='#2b2552'){const g=ctx.createLinearGradient(0,0,w,h);g.addColorStop(0,a);g.addColorStop(1,b);ctx.fillStyle=g;ctx.fillRect(0,0,w,h)}
export function burst(scene,x,y,color='#ffd95a',n=16){scene.particles ||= [];for(let i=0;i<n;i++){const a=Math.random()*TAU,s=rand(45,150);scene.particles.push({x,y,vx:Math.cos(a)*s,vy:Math.sin(a)*s,life:rand(.35,.8),color})}}
export function particles(scene,ctx,dt){for(const p of scene.particles||[]){p.x+=p.vx*dt;p.y+=p.vy*dt;p.vy+=70*dt;p.life-=dt;ctx.globalAlpha=Math.max(0,p.life);circle(ctx,p.x,p.y,3,p.color)}ctx.globalAlpha=1;scene.particles=(scene.particles||[]).filter(p=>p.life>0)}
export function difficulty(opt){return opt.difficulty==='easy'?.62:opt.difficulty==='hard'?1.25:1}
export function makeEnv(canvas,store,sfx,options,onEnd){const ctx=canvas.getContext('2d',{alpha:false});let ended=false,raf=0,last=performance.now(),scene=null;const env={canvas,ctx,store,sfx,options,get w(){return canvas.width/devicePixelRatio},get h(){return canvas.height/devicePixelRatio},end(win,msg=''){if(ended)return;ended=true;onEnd(win,msg)},score(){sfx.score();vibrate(12)},burst(x,y,c,n){burst(scene,x,y,c,n)},restart(){ended=false;scene?.reset?.()}};const resize=()=>{const r=canvas.getBoundingClientRect(),d=Math.min(devicePixelRatio||1,2);canvas.width=Math.floor(r.width*d);canvas.height=Math.floor(r.height*d);ctx.setTransform(d,0,0,d,0,0);scene?.resize?.(r.width,r.height)};const loop=now=>{const dt=Math.min(.033,(now-last)/1000);last=now;if(!ended){scene?.update?.(dt);scene?.render?.(ctx,env.w,env.h,dt);particles(scene,ctx,dt)}raf=requestAnimationFrame(loop)};const point=e=>{const r=canvas.getBoundingClientRect();return{x:e.clientX-r.left,y:e.clientY-r.top,id:e.pointerId}};canvas.onpointerdown=e=>{canvas.setPointerCapture?.(e.pointerId);scene?.down?.(point(e),e);sfx.tap()};canvas.onpointermove=e=>scene?.move?.(point(e),e);canvas.onpointerup=e=>scene?.up?.(point(e),e);window.addEventListener('resize',resize);return{env,setScene(s){scene=s;resize();scene?.reset?.();raf=requestAnimationFrame(loop)},destroy(){cancelAnimationFrame(raf);window.removeEventListener('resize',resize);canvas.onpointerdown=canvas.onpointermove=canvas.onpointerup=null}}}

globalThis.NexusCore={clamp,lerp,rand,dist,hit,TAU,rr,text,circle,line,bg,difficulty};