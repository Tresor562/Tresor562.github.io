import fs from 'node:fs';
import path from 'node:path';
import { hydrateNexCanalEmojiPalette, customEmojiIdForText, movingCustomEmojiId } from './dist/src/app/rich-ui.js';

const DIR='/var/lib/nex/data/promo/hontavyr-mods-exact-20261110';
const STATE='/var/lib/nex/data/promo/hontavyr-mods-20261110/state.json';
const LOCK='/var/lib/nex/data/promo/hontavyr-mods-20261110/publish.lock';
const DATA='hontavyr:publish:nov10v2';
const CHANNEL='@thenexusorigin';
const JOIN_URL='https://t.me/thenexusorigin';
const PICS=[1,2,3,4,5].map(i=>path.join(DIR,`slide-${i}.jpg`));
const SHA=['18713b80d2218efd5bf12212032d01ec24d37ac6893f304896932fce30c503f6','9864a99055f1ebd2b3032c0d6d2b432a54666e1efdaec58aa6fb1144ea87f134','1fb2bf57d0f5df544261595fcc1b90987995200ef3b4d83698e1c2f8cc6e7a2c','f971904c1341d68d2733bb9125837ea45600551d44acd9765090b874e43da791','792a7a44aae3cba55aaa9d401c59ec5a5f2c58a20afe492fb036c03da2e5b5c3'];
const FALLBACK_FLAGS={'🇫🇷':'5460765816934901342','🇬🇧':'5467887736000093669'};
let emoji={};
const state=()=>{try{return JSON.parse(fs.readFileSync(STATE,'utf8'));}catch{return {};}};
const write=s=>fs.writeFileSync(STATE,JSON.stringify(s,null,2),{mode:0o600});
const bold=text=>({type:'bold',text});
const italic=text=>({type:'italic',text});
const heading=(size,text)=>({type:'heading',size,text});
const para=text=>({type:'paragraph',text});
const flag=glyph=>({type:'custom_emoji',custom_emoji_id:emoji[glyph],alternative_text:glyph});
const E=glyph=>{if(!emoji[glyph])throw Error('Missing custom emoji '+glyph);return flag(glyph);};

export async function prepareEmoji(api){
  await hydrateNexCanalEmojiPalette(api);
  const glyphs=['👑','🔥','💎','🎮','🚀','⚡','✨'];
  const ids=[];
  for(const glyph of glyphs){
    const id=customEmojiIdForText(glyph);
    if(!id||!movingCustomEmojiId(id))throw Error('Custom animated emoji unavailable: '+glyph);
    emoji[glyph]=id;ids.push(id);
  }
  for(const [k,v] of Object.entries(FALLBACK_FLAGS)){emoji[k]=v;ids.push(v);}
  const stickers=await api.call('getCustomEmojiStickers',{custom_emoji_ids:ids});
  const byId=new Map(stickers.map(s=>[String(s.custom_emoji_id),s]));
  for(const [glyph,id] of Object.entries(emoji)){
    const s=byId.get(String(id));
    if(!s||s.emoji!==glyph)throw Error('Mismatched custom emoji '+glyph);
    if(!FALLBACK_FLAGS[glyph]&&!s.is_animated&&!s.is_video)throw Error('Non-animated custom emoji '+glyph);
  }
  return emoji;
}

export function rich(mode){
  const blocks=[
    heading(2,[E('👑'),bold('  ׄ 𖦹᳟  𝖧ᦅ𝗇𝗍𝖺𝗏𝗒𝗋  𝖬ᦅ𝖽𝗌  𓏼𖹭')]),
    para([E('🔥'),bold(' TES APPLIS MÉRITENT PLUS — YOUR APPS DESERVE MORE')]),
    {type:'slideshow',blocks:PICS.map((_,i)=>({type:'photo',photo:{type:'photo',media:`attach://hm_original_${i+1}`}}))},
    para([E('✨'),italic(' Fais glisser les images / Swipe through the five highlights')]),
    {type:'divider'},
    heading(3,[E('🇫🇷'),bold(' POURQUOI TE CONTENTER DES VERSIONS CLASSIQUES ?')]),
    para([E('🔥'),' Ton appli préférée peut parfois t’offrir bien plus. Ne passe pas à côté des bonnes découvertes !']),
    para([E('💎'),bold(' APK MODS sélectionnés : '),'options Premium, fonctionnalités supplémentaires et versions sans publicité, selon les applications.']),
    para([E('🎮'),' Jeux, applications et outils : de nouvelles pépites et des mises à jour à découvrir régulièrement.']),
    para([E('⚡'),bold(' Plus de fonctionnalités. Moins de limites. Une nouvelle expérience à explorer.')]),
    {type:'divider'},
    heading(3,[E('🇬🇧'),bold(' WHY SETTLE FOR BASIC VERSIONS?')]),
    para([E('🔥'),' Your favourite apps could offer so much more. Discover what you have been missing!']),
    para([E('💎'),bold(' Explore selected modded APKs: '),'Premium options, extra features and ad-free versions, where available.']),
    para([E('🎮'),' Games, apps and useful tools — fresh discoveries and updates worth checking out.']),
    para([E('⚡'),bold(' More features. Fewer limits. More ways to enjoy your apps.')]),
    {type:'divider'},
    heading(3,[E('🚀'),bold(' TA PROCHAINE PÉPITE T’ATTEND • YOUR NEXT FAVORITE APP AWAITS')]),
    {type:'footer',text:[E('✨'),bold(' HONTAVYR MODS — BEYOND THE ORDINARY')]}
  ];
  return {blocks,skip_entity_detection:true};
}
export function markup(mode){
  const publish={text:'PUBLIER SUR LE CANAL',callback_data:DATA,style:'success',icon_custom_emoji_id:emoji['🚀']};
  const join={text:'REJOINDRE LE CANAL • JOIN NOW',url:JOIN_URL,style:'success',icon_custom_emoji_id:emoji['💎']};
  if(mode==='preview')return {inline_keyboard:[[publish],[{...join,style:'primary'}]]};
  return {inline_keyboard:[[join]]};
}
function validateResult(res,mode){
  if(!res||!Number.isInteger(res.message_id))throw Error('Telegram response not confirmed');
  const carousel=res.rich_message?.blocks?.find(b=>b.type==='slideshow');
  if(!carousel||carousel.blocks?.length!==5)throw Error('Telegram did not confirm five-slide slideshow');
  const button=res.reply_markup?.inline_keyboard;
  if(!button?.length||button.flat().length!==(mode==='preview'?2:1))throw Error('Telegram did not return expected inline buttons');
  if(mode!=='preview'&&button[0][0].url!==JOIN_URL)throw Error('Missing channel join URL');
  return {message_id:res.message_id,chat_id:res.chat?.id,photos:carousel.blocks.length,buttons:button.flat().map(x=>({text:x.text,style:x.style||'default',url:!!x.url,callback:!!x.callback_data}))};
}
function files(){return PICS.map((p,i)=>({field:`hm_original_${i+1}`,bytes:fs.readFileSync(p),filename:`hontavyr-original-${i+1}.jpg`,mimeType:'image/jpeg'}));}
export async function send(api,dest,mode){
  const payload={chat_id:String(dest),rich_message:JSON.stringify(rich(mode)),reply_markup:JSON.stringify(markup(mode))};
  const res=await api.callMultipart('sendRichMessage',payload,files());
  return validateResult(res,mode);
}
export async function edit(api,dest,messageId,mode){
  const payload={chat_id:String(dest),message_id:String(messageId),rich_message:JSON.stringify(rich(mode)),reply_markup:JSON.stringify(markup(mode))};
  const res=await api.callMultipart('editMessageText',payload,files());
  return validateResult(res,mode);
}
export async function replacePrivatePreview(api,owner){
  const me=await api.call('getChat',{chat_id:String(owner)});
  if(me.type!=='private'||me.username!=='tresor20001')throw Error('Unexpected owner destination');
  if(PICS.some(x=>!fs.existsSync(x)))throw Error('Original image files missing');
  await prepareEmoji(api);
  const previous=state();
  if(previous.preview_version==='exact-v3'&&previous.preview_id)return {already_sent:true,preview_id:previous.preview_id};
  let delivery=null;let editError=null;
  if(previous.preview_id){
    try{delivery=await edit(api,owner,previous.preview_id,'preview');}
    catch(e){editError=String(e.message||e).slice(0,260);console.log('EXISTING_RICH_EDIT_FAILED',editError);}
  }
  if(!delivery){
    delivery=await send(api,owner,'preview');
    write({...previous,owner:String(owner),preview_id:delivery.message_id,preview_version:'exact-v3',preview_at:new Date().toISOString(),preview_delivery:delivery});
    if(previous.preview_id&&previous.preview_id!==delivery.message_id){
      try{await api.call('deleteMessage',{chat_id:String(owner),message_id:previous.preview_id});console.log('OLD_PREVIEW_REMOVED',previous.preview_id);}catch(e){console.log('OLD_PREVIEW_DELETE_FAILED',String(e.message).slice(0,160));}
    }
  }else write({...previous,owner:String(owner),preview_id:delivery.message_id,preview_version:'exact-v3',preview_at:new Date().toISOString(),preview_delivery:delivery});
  return {...delivery,edited_existing:!editError&&previous.preview_id===delivery.message_id,previous_id:previous.preview_id};
}
export async function publishFromCallback(bot,q){
  const s=state();
  const owner=String(process.env.NEXUS_OWNER_TELEGRAM_ID||'').split(',')[0].trim();
  const allowed=String(q.from?.id)===owner&&String(q.message?.chat?.id)===owner&&q.message?.chat?.type==='private'&&Number(q.message?.message_id)===Number(s.preview_id)&&s.preview_version==='exact-v3';
  if(!allowed){try{await bot.api.answerCallbackQuery(q.id,'Action non autorisée.',true);}catch{}return;}
  if(s.channel_id){try{await bot.api.answerCallbackQuery(q.id,'Déjà publié sur le canal.',true);}catch{}return;}
  let fd;
  try{fd=fs.openSync(LOCK,'wx',0o600);}catch{try{await bot.api.answerCallbackQuery(q.id,'Publication en cours : aucun doublon.',true);}catch{}return;}
  try{
    await prepareEmoji(bot.api);
    const done=await send(bot.api,CHANNEL,'public');
    write({...state(),channel_id:done.message_id,channel_at:new Date().toISOString(),channel_delivery:done});
    fs.closeSync(fd);fs.unlinkSync(LOCK);
    try{await bot.api.answerCallbackQuery(q.id,'Publication HONTAVYR réussie !',true);}catch{}
    console.log('HONTAVYR_EXACT_RICH_PUBLISHED',JSON.stringify(done));
  }catch(e){
    fs.closeSync(fd);
    if(Number(e.errorCode)===400){try{fs.unlinkSync(LOCK);}catch{}}
    try{await bot.api.answerCallbackQuery(q.id,'Publication non confirmée. Aucune répétition automatique.',true);}catch{}
    console.error('HONTAVYR_EXACT_RICH_PUBLISH_ERROR',String(e.message||e).slice(0,300));
  }
}
export {DATA,CHANNEL,JOIN_URL,PICS,SHA};