(() => {
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const header = document.querySelector('.site-header');
  const cursor = document.querySelector('.cursor');

  const splitText = (el) => {
    const text = el.textContent;
    el.setAttribute('aria-label', text.trim());
    el.textContent = '';
    [...text].forEach((ch, i) => {
      const span = document.createElement('span');
      span.className = 'char';
      span.style.setProperty('--i', i);
      span.setAttribute('aria-hidden', 'true');
      span.textContent = ch === ' ' ? '\u00a0' : ch;
      el.appendChild(span);
    });
  };

  document.querySelectorAll('[data-split]').forEach(splitText);
  requestAnimationFrame(() => document.documentElement.classList.add('is-ready'));

  const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        revealObserver.unobserve(entry.target);
      }
    });
  }, { threshold: .14, rootMargin: '0px 0px -6% 0px' });

  document.querySelectorAll('[data-reveal]').forEach((el) => revealObserver.observe(el));

  const words = [...document.querySelectorAll('.word-rotator span')];
  let current = 0;
  if (words.length) {
    words[0].classList.add('active');
    if (!reduceMotion) {
      setInterval(() => {
        const prev = words[current];
        prev.classList.remove('active');
        prev.classList.add('out');
        current = (current + 1) % words.length;
        const next = words[current];
        next.classList.remove('out');
        next.classList.add('active');
        setTimeout(() => prev.classList.remove('out'), 650);
      }, 2200);
    }
  }

  const onScroll = () => {
    const y = window.scrollY;
    if (header) header.classList.toggle('is-scrolled', y > 18);
    if (!reduceMotion) {
      document.querySelectorAll('[data-parallax]').forEach((el) => {
        const speed = Number(el.dataset.parallax || .06);
        const rect = el.getBoundingClientRect();
        const delta = (window.innerHeight / 2 - (rect.top + rect.height / 2)) * speed;
        el.style.transform = `translate3d(${delta}px,0,0)`;
      });
    }
  };
  onScroll();
  window.addEventListener('scroll', onScroll, { passive:true });

  if (cursor && !reduceMotion && window.matchMedia('(pointer:fine)').matches) {
    let x = -100, y = -100, tx = -100, ty = -100;
    const loop = () => {
      x += (tx - x) * .16;
      y += (ty - y) * .16;
      cursor.style.left = `${x}px`;
      cursor.style.top = `${y}px`;
      requestAnimationFrame(loop);
    };
    loop();
    window.addEventListener('mousemove', (e) => { tx = e.clientX; ty = e.clientY; cursor.classList.remove('hidden'); });
    document.documentElement.addEventListener('mouseleave', () => cursor.classList.add('hidden'));
    document.querySelectorAll('a,button,.project-row,.portrait-shell').forEach((el) => {
      el.addEventListener('mouseenter', () => cursor.classList.add('big'));
      el.addEventListener('mouseleave', () => cursor.classList.remove('big'));
    });
  }

  document.querySelectorAll('a[href^="#"]').forEach((link) => {
    link.addEventListener('click', (e) => {
      const target = document.querySelector(link.getAttribute('href'));
      if (!target) return;
      e.preventDefault();
      target.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block:'start' });
    });
  });
})();
