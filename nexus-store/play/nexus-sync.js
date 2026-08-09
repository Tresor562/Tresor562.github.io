const GUEST_KEY='nexus.games.guest.v1';
const SESSION_KEY='nexus.auth.session.v1';
const CFG=()=>window.NEXUS_CONFIG||{};
const baseState=()=>({coins:0,stats:{},theme:'system',sound:true});
const parse=(v,f)=>{try{return JSON.parse(v)||f}catch{return f}};
const normalize=s=>({ ...baseState(), ...(s||{}), stats:{...((s||{}).stats||{})} });
const authHeaders=(token='')=>{const c=CFG();return{'content-type':'application/json','apikey':c.anonKey||'',...(token?{authorization:`Bearer ${token}`}:{})}};
const configured=()=>Boolean(CFG().url&&CFG().anonKey);

export class NexusSync{
  constructor(){this.listeners=new Set();this.mode='guest';this.user=null;this.state=this.loadGuest();this.session=null;}
  onChange(fn){this.listeners.add(fn);return()=>this.listeners.delete(fn)}
  emit(){const payload={mode:this.mode,user:this.user,state:normalize(this.state)};for(const fn of this.listeners)try{fn(payload)}catch{};window.dispatchEvent(new CustomEvent('nexus:state',{detail:payload}))}
  loadGuest(){return normalize(parse(localStorage.getItem(GUEST_KEY),baseState()))}
  saveGuest(){localStorage.setItem(GUEST_KEY,JSON.stringify(this.state))}
  pendingKey(uid){return`nexus.games.pending.${uid}`}
  loadPending(uid){return parse(localStorage.getItem(this.pendingKey(uid)),[])}
  savePending(uid,q){localStorage.setItem(this.pendingKey(uid),JSON.stringify(q))}
  async init(){
    this.session=parse(localStorage.getItem(SESSION_KEY),null);
    if(this.session&&configured()){
      try{await this.ensureSession();await this.enterAccount();return this.snapshot()}catch{localStorage.removeItem(SESSION_KEY);this.session=null;}
    }
    this.mode='guest';this.user=null;this.state=this.loadGuest();return this.snapshot();
  }
  snapshot(){return{mode:this.mode,user:this.user,state:normalize(this.state)}}
  async request(path,{method='GET',body,token=this.session?.access_token}={}){const c=CFG();if(!configured())throw new Error('Nexus Cloud non configuré');const r=await fetch(`${c.url}${path}`,{method,headers:authHeaders(token),body:body===undefined?undefined:JSON.stringify(body)});const txt=await r.text();let data=null;try{data=txt?JSON.parse(txt):null}catch{data=txt}if(!r.ok)throw new Error(data?.msg||data?.message||data?.error_description||`Erreur ${r.status}`);return data}
  saveSession(session){this.session=session;localStorage.setItem(SESSION_KEY,JSON.stringify(session))}
  async ensureSession(){if(!this.session)throw new Error('Session absente');const expires=(this.session.expires_at||0)*1000;if(expires>Date.now()+60000)return this.session;if(!this.session.refresh_token)throw new Error('Session expirée');const data=await this.request('/auth/v1/token?grant_type=refresh_token',{method:'POST',body:{refresh_token:this.session.refresh_token},token:''});this.saveSession(data);return data}
  async me(){await this.ensureSession();return this.request('/auth/v1/user')}
  async signIn(email,password){const data=await this.request('/auth/v1/token?grant_type=password',{method:'POST',body:{email,password},token:''});this.saveSession(data);await this.enterAccount();return this.snapshot()}
  async signUp(email,password,displayName=''){const data=await this.request('/auth/v1/signup',{method:'POST',body:{email,password,data:{display_name:displayName}},token:''});if(data?.access_token){this.saveSession(data);await this.enterAccount();return this.snapshot()}return{needsEmailConfirmation:true}}
  async signOut(){try{if(this.session&&configured())await this.request('/auth/v1/logout',{method:'POST'})}catch{}localStorage.removeItem(SESSION_KEY);this.session=null;this.mode='guest';this.user=null;this.state=this.loadGuest();this.emit();return this.snapshot()}
  async enterAccount(){const user=await this.me();this.mode='account';this.user={id:user.id,email:user.email,displayName:user.user_metadata?.display_name||user.email?.split('@')[0]||'Nexus'};const remote=await this.request('/rest/v1/rpc/nexus_get_game_state',{method:'POST',body:{}});this.state=normalize(remote);await this.flushPending();this.emit()}
  savePrefs({theme,sound}){this.state.theme=theme||'system';this.state.sound=sound!==false;if(this.mode==='guest'){this.saveGuest();return}if(!this.user)return;this.request('/rest/v1/rpc/nexus_set_game_preferences',{method:'POST',body:{p_theme:this.state.theme,p_sound:this.state.sound}}).catch(()=>{})}
  optimistic({gameId,won}){const id=String(gameId),s={played:0,wins:0,level:1,best:0,stars:0,...(this.state.stats[id]||{})};s.played++;if(won){s.wins++;this.state.coins=Number(this.state.coins||0)+10;s.level=Math.max(s.level||1,1+Math.floor(s.wins/3));s.stars=Math.max(s.stars||0,Math.min(3,1+Math.floor((s.wins%3))))}this.state.stats[id]=s;}
  async recordResult(evt){if(this.mode==='guest'||!this.user){this.optimistic(evt);this.saveGuest();this.emit();return this.snapshot()}
    this.optimistic(evt);this.emit();const payload={p_game_id:evt.gameId,p_won:!!evt.won,p_match_id:evt.matchId,p_level:this.state.stats[String(evt.gameId)]?.level||1,p_score:0};
    try{const remote=await this.request('/rest/v1/rpc/nexus_record_game_result',{method:'POST',body:payload});this.state=normalize(remote);this.emit()}catch{const q=this.loadPending(this.user.id);if(!q.some(x=>x.p_match_id===payload.p_match_id)){q.push(payload);this.savePending(this.user.id,q)}}return this.snapshot()}
  async flushPending(){if(this.mode!=='account'||!this.user)return;const q=this.loadPending(this.user.id);if(!q.length)return;const remain=[];for(const evt of q){try{const remote=await this.request('/rest/v1/rpc/nexus_record_game_result',{method:'POST',body:evt});this.state=normalize(remote)}catch{remain.push(evt)}}this.savePending(this.user.id,remain)}
}