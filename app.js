const reduced=matchMedia('(prefers-reduced-motion:reduce)').matches,loader=document.getElementById('loader'),hero=document.querySelector('.hero');
if(!reduced){
  const w=document.getElementById('loaderWord');
  w.innerHTML=[...w.textContent].map((c,i)=>`<span class="lchar" style="--i:${i}">${c}</span>`).join('');
  document.querySelectorAll('[data-split]').forEach(el=>{
    const base=Number(el.dataset.delay||0),t=el.textContent;
    el.innerHTML=[...t].map((c,i)=>`<span class="hchar" style="--i:${i};--base:${base}ms">${c===' '?'&nbsp;':c}</span>`).join('');
  });
  setTimeout(()=>loader.classList.add('out'),1450);
  setTimeout(()=>hero.classList.add('ready'),1700);
}else{
  loader?.remove();
  hero?.classList.add('ready');
}
const nav=document.getElementById('nav'),topbtn=document.getElementById('topbtn');
function scrollState(){nav?.classList.toggle('solid',scrollY>55);topbtn?.classList.toggle('show',scrollY>innerHeight*.7)}
addEventListener('scroll',scrollState,{passive:true});scrollState();
const io=new IntersectionObserver(es=>es.forEach(e=>{if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target)}}),{threshold:.12,rootMargin:'0px 0px -25px'});
document.querySelectorAll('.reveal,.scale').forEach(el=>io.observe(el));
const burger=document.getElementById('burger'),mobile=document.getElementById('mobile');
function menu(v){const open=typeof v==='boolean'?v:!mobile.classList.contains('open');mobile.classList.toggle('open',open);document.body.style.overflow=open?'hidden':''}
burger?.addEventListener('click',()=>menu());
mobile?.querySelectorAll('a').forEach(a=>a.addEventListener('click',()=>menu(false)));
topbtn?.addEventListener('click',()=>scrollTo({top:0,behavior:reduced?'auto':'smooth'}));