// Draws original placeholder strips and avatars as SVG, so the README
// screenshots never show real comic artwork.

const INK = '#1E1B16';
const PAPER = '#FFFDF8';
const SKIN = '#F4C9A0';

const PANEL = 280;
const GUTTER = 10;
const GROUND = 238;

// --- deterministic randomness ---------------------------------------------

function rng(seed) {
  let s = seed >>> 0 || 1;
  return () => {
    s ^= s << 13;
    s ^= s >>> 17;
    s ^= s << 5;
    return ((s >>> 0) % 10000) / 10000;
  };
}

const esc = (t) => t.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// --- faces -------------------------------------------------------------------

function eyes(x1, x2, y, r = 3.2) {
  return `<ellipse cx="${x1}" cy="${y}" rx="${r}" ry="${r * 1.25}" fill="${INK}"/>
    <ellipse cx="${x2}" cy="${y}" rx="${r}" ry="${r * 1.25}" fill="${INK}"/>`;
}

function mouth(x, y, mood, w = 10) {
  switch (mood) {
    case 'talk':
      return `<ellipse cx="${x}" cy="${y + 2}" rx="${w * 0.4}" ry="${w * 0.35}" fill="${INK}"/>`;
    case 'surprised':
      return `<circle cx="${x}" cy="${y + 3}" r="${w * 0.3}" fill="none" stroke="${INK}" stroke-width="2.5"/>`;
    case 'flat':
      return `<path d="M${x - w / 2} ${y + 2} h${w}" stroke="${INK}" stroke-width="2.5" stroke-linecap="round"/>`;
    default:
      return `<path d="M${x - w / 2} ${y} q${w / 2} ${w * 0.6} ${w} 0" fill="none" stroke="${INK}" stroke-width="2.5" stroke-linecap="round"/>`;
  }
}

const ln = (w = 3) => `stroke="${INK}" stroke-width="${w}" stroke-linejoin="round" stroke-linecap="round"`;

// --- characters: origin at the feet, facing right, about 150 tall -------------
// Each returns { svg, mouthY } where mouthY is where a bubble tail should point.

const CHARACTERS = {
  kid(mood, arm) {
    const armUp = arm ? 'M18 -72 l22 -30' : 'M18 -72 l14 26';
    return {
      top: -150,
      svg: `
      <path d="M-10 0 v-38 M10 0 v-38" stroke="${INK}" stroke-width="7" stroke-linecap="round"/>
      <rect x="-22" y="-88" width="44" height="54" rx="12" fill="#E4553F" ${ln()}/>
      <path d="M-22 -70 h44 M-22 -56 h44" stroke="#F2B32A" stroke-width="5"/>
      <rect x="-22" y="-88" width="44" height="54" rx="12" fill="none" ${ln()}/>
      <path d="M-18 -72 l-14 26 ${armUp}" ${ln()} fill="none"/>
      <circle cx="0" cy="-114" r="29" fill="${SKIN}" ${ln()}/>
      <path d="M-27 -122 l6 -24 l8 12 l6 -18 l7 15 l8 -16 l4 17 l9 -10 l-3 22 q-20 -8 -45 2 z" fill="#8A4B22" ${ln()}/>
      ${eyes(6, 18, -114)}
      ${mouth(12, -100, mood)}`,
    };
  },

  dog(mood) {
    return {
      top: -95,
      svg: `
      <path d="M-34 -22 q-24 -12 -18 -40" fill="none" ${ln(5)}/>
      <path d="M-26 0 v-18 M-10 0 v-18 M10 0 v-18 M26 0 v-18" stroke="${INK}" stroke-width="6" stroke-linecap="round"/>
      <ellipse cx="0" cy="-34" rx="40" ry="22" fill="#F0E2C8" ${ln()}/>
      <ellipse cx="-12" cy="-40" rx="10" ry="8" fill="#8A4B22"/>
      <circle cx="34" cy="-66" r="22" fill="#F0E2C8" ${ln()}/>
      <ellipse cx="22" cy="-62" rx="8" ry="16" fill="#8A4B22" ${ln()} transform="rotate(20 22 -62)"/>
      ${eyes(36, 46, -72, 2.8)}
      <circle cx="56" cy="-62" r="4.5" fill="${INK}"/>
      ${mouth(46, -54, mood, 8)}`,
    };
  },

  owl(mood, _arm, colors = ['#9C6B45', '#E7C9A0']) {
    return {
      top: -135,
      svg: `
      <path d="M-12 0 l-6 -8 M-12 0 l0 -8 M12 0 l6 -8 M12 0 l0 -8" stroke="#F2B32A" stroke-width="4" stroke-linecap="round"/>
      <path d="M0 -130 c-38 0 -46 40 -44 70 c2 40 20 56 44 56 c24 0 42 -16 44 -56 c2 -30 -6 -70 -44 -70 z" fill="${colors[0]}" ${ln()}/>
      <path d="M-28 -125 l-6 -18 l16 10 M28 -125 l6 -18 l-16 10" fill="${colors[0]}" ${ln()}/>
      <ellipse cx="0" cy="-40" rx="26" ry="30" fill="${colors[1]}"/>
      <circle cx="-15" cy="-96" r="15" fill="${PAPER}" ${ln()}/>
      <circle cx="15" cy="-96" r="15" fill="${PAPER}" ${ln()}/>
      <circle cx="${mood === 'flat' ? -15 : -12}" cy="-95" r="6" fill="${INK}"/>
      <circle cx="${mood === 'flat' ? 15 : 18}" cy="-95" r="6" fill="${INK}"/>
      ${mood === 'flat' ? `<path d="M-30 -99 h30 M0 -99 h30" stroke="${colors[0]}" stroke-width="10"/>` : ''}
      <path d="M-6 -78 l6 ${mood === 'talk' || mood === 'surprised' ? 16 : 11} l6 -${mood === 'talk' || mood === 'surprised' ? 16 : 11} z" fill="#F2B32A" ${ln()}/>`,
    };
  },

  owl2(mood, arm) {
    return CHARACTERS.owl(mood, arm, ['#6F7F92', '#D8DEE6']);
  },

  plant(mood, _arm, color = '#4E8F4A') {
    return {
      top: -150,
      svg: `
      <path d="M0 -60 q-8 -40 -40 -70 q30 10 40 50 M0 -60 q6 -50 30 -86 q-2 40 -24 80 M0 -60 q18 -30 52 -40 q-20 22 -48 44 M0 -60 q-20 -22 -54 -22 q26 10 50 26" fill="${color}" ${ln()}/>
      <path d="M-34 -62 h68 l-8 62 h-52 z" fill="#D9774B" ${ln()}/>
      <rect x="-38" y="-68" width="76" height="14" rx="3" fill="#C4623A" ${ln()}/>
      ${eyes(-10, 10, -34, 3)}
      ${mouth(0, -22, mood, 10)}`,
    };
  },

  plant2(mood, arm) {
    const p = CHARACTERS.plant(mood, arm, '#7BAA3F');
    return { ...p, svg: p.svg.replace('#D9774B', '#5F8FC4').replace('#C4623A', '#4A78AC') };
  },

  robot(mood, arm, color = '#9FB7CC') {
    const armR = arm ? 'M28 -70 l20 -28' : 'M28 -70 l14 24';
    return {
      top: -148,
      svg: `
      <path d="M-12 0 v-26 M12 0 v-26" stroke="${INK}" stroke-width="8" stroke-linecap="round"/>
      <path d="M-28 -70 l-14 24 ${armR}" ${ln(5)} fill="none"/>
      <rect x="-28" y="-86" width="56" height="60" rx="8" fill="${color}" ${ln()}/>
      <circle cx="0" cy="-56" r="9" fill="#F2B32A" ${ln()}/>
      <path d="M0 -118 v-18" ${ln()}/>
      <circle cx="0" cy="-138" r="6" fill="#E4553F" ${ln()}/>
      <rect x="-30" y="-122" width="60" height="38" rx="10" fill="${color}" ${ln()}/>
      <rect x="-22" y="-116" width="44" height="24" rx="5" fill="#2B3440"/>
      <rect x="-14" y="-110" width="7" height="${mood === 'flat' ? 3 : 9}" rx="2" fill="#7FE0C4"/>
      <rect x="8" y="-110" width="7" height="${mood === 'flat' ? 3 : 9}" rx="2" fill="#7FE0C4"/>
      ${mood === 'talk' || mood === 'surprised' ? '<rect x="-6" y="-98" width="12" height="3" fill="#7FE0C4"/>' : ''}`,
    };
  },

  robot2(mood, arm) {
    return CHARACTERS.robot(mood, arm, '#E3B7C8');
  },

  crab(mood) {
    return {
      top: -95,
      svg: `
      <path d="M-30 -14 l-16 14 M-24 -8 l-10 10 M30 -14 l16 14 M24 -8 l10 10" ${ln()}/>
      <path d="M-34 -30 l-18 -24 M34 -30 l18 -24" ${ln(5)}/>
      <path d="M-62 -56 a12 12 0 1 1 18 -6 l-10 -2 z" fill="#E4553F" ${ln()}/>
      <path d="M62 -56 a12 12 0 1 0 -18 -6 l10 -2 z" fill="#E4553F" ${ln()}/>
      <ellipse cx="0" cy="-26" rx="40" ry="24" fill="#E4553F" ${ln()}/>
      <path d="M-10 -46 v-26 M10 -46 v-26" ${ln()}/>
      <circle cx="-10" cy="-76" r="7" fill="${PAPER}" ${ln()}/>
      <circle cx="10" cy="-76" r="7" fill="${PAPER}" ${ln()}/>
      <circle cx="-9" cy="-76" r="3" fill="${INK}"/>
      <circle cx="11" cy="-76" r="3" fill="${INK}"/>
      ${mouth(0, -28, mood, 12)}`,
    };
  },

  gull(mood) {
    return {
      top: -110,
      svg: `
      <path d="M-6 0 v-16 M6 0 v-16" stroke="#F2B32A" stroke-width="4" stroke-linecap="round"/>
      <path d="M-34 -40 q10 -30 40 -30 q26 0 30 22 q-6 30 -40 32 q-24 0 -30 -24 z" fill="${PAPER}" ${ln()}/>
      <path d="M-20 -46 q16 -6 30 6 q-18 8 -30 -6 z" fill="#9AA3AD" ${ln()}/>
      <circle cx="26" cy="-82" r="18" fill="${PAPER}" ${ln()}/>
      <circle cx="30" cy="-86" r="3.5" fill="${INK}"/>
      <path d="M42 -84 l20 ${mood === 'talk' || mood === 'surprised' ? '-2 l-20 8' : '4 l-20 4'} z" fill="#F2B32A" ${ln()}/>`,
    };
  },

  keeper(mood, arm) {
    const armR = arm ? 'M22 -84 l24 -30' : 'M22 -84 l14 34';
    return {
      top: -170,
      svg: `
      <path d="M-12 0 v-44 M12 0 v-44" stroke="#2B3440" stroke-width="9" stroke-linecap="round"/>
      <path d="M-26 -104 h52 l6 66 h-64 z" fill="#F2B32A" ${ln()}/>
      <path d="M0 -100 v60" stroke="${INK}" stroke-width="2"/>
      <path d="M-22 -84 l-14 34 ${armR}" ${ln(5)} fill="none"/>
      <circle cx="0" cy="-128" r="24" fill="${SKIN}" ${ln()}/>
      <path d="M-22 -126 q0 34 22 34 q22 0 22 -34 q-10 10 -22 10 q-12 0 -22 -10 z" fill="${PAPER}" ${ln()}/>
      <path d="M-28 -142 q28 -30 56 0 z" fill="#2B3440" ${ln()}/>
      <path d="M-32 -142 h64" ${ln(5)}/>
      ${eyes(-6, 10, -134, 2.8)}
      ${mood === 'talk' || mood === 'surprised' ? `<ellipse cx="4" cy="-114" rx="4" ry="3" fill="${INK}"/>` : ''}`,
    };
  },

  dragon(mood, arm) {
    return {
      top: -130,
      svg: `
      <path d="M-30 -20 q-40 0 -44 -30 q14 14 30 8" fill="#6DB36A" ${ln()}/>
      <path d="M-14 0 v-14 M14 0 v-14" stroke="${INK}" stroke-width="8" stroke-linecap="round"/>
      <path d="M-20 -70 l-26 -26 l6 30 z" fill="#B7E0A8" ${ln()}/>
      <ellipse cx="0" cy="-40" rx="34" ry="34" fill="#6DB36A" ${ln()}/>
      <ellipse cx="6" cy="-34" rx="20" ry="22" fill="#D8EFC8"/>
      <path d="M-12 -106 l-4 -16 l12 8 M8 -108 l6 -16 l4 16" fill="#F2B32A" ${ln()}/>
      <path d="M-20 -82 q0 -30 30 -30 q30 0 38 22 q4 16 -12 20 q-20 6 -40 4 q-16 -2 -16 -16 z" fill="#6DB36A" ${ln()}/>
      ${eyes(12, 26, -94, 3)}
      <circle cx="44" cy="-86" r="2" fill="${INK}"/>
      ${mouth(30, -76, mood, 12)}
      ${arm ? `<path d="M20 -48 l20 -14" ${ln(5)}/>` : ''}`,
    };
  },
};

// --- scenes -----------------------------------------------------------------

const SCENES = {
  meadow: { sky: '#CFE3F5', ground: '#A9CF8A', props: ['sun', 'cloud'] },
  night: { sky: '#2E3A5C', ground: '#3F5A48', props: ['moon', 'stars', 'branch'] },
  window: { sky: '#F6E7CF', ground: '#C9A67E', props: ['window'] },
  kitchen: { sky: '#F8DCC4', ground: '#E7C7A3', props: ['tiles'] },
  shore: { sky: '#CDE7F4', ground: '#F1DDA8', props: ['sun', 'waves'] },
  park: { sky: '#DCEAF7', ground: '#B8D69A', props: ['tree', 'cloud'] },
  coast: { sky: '#D6E2EC', ground: '#8FA6A0', props: ['lighthouse', 'cloud'] },
  cave: { sky: '#E3DDF2', ground: '#B9A98F', props: ['rocks', 'gems'] },
};

function prop(name, r) {
  const cx = 30 + r() * 220;
  switch (name) {
    case 'sun':
      return `<circle cx="${200 + r() * 50}" cy="${46 + r() * 20}" r="20" fill="#F2B32A" ${ln()}/>`;
    case 'cloud': {
      const y = 40 + r() * 40;
      return `<path d="M${cx} ${y} a14 14 0 0 1 24 -8 a16 16 0 0 1 28 6 a11 11 0 0 1 2 22 h-54 a11 11 0 0 1 0 -20 z" fill="${PAPER}" ${ln()}/>`;
    }
    case 'moon':
      return `<path d="M232 36 a22 22 0 1 0 12 38 a18 18 0 1 1 -12 -38 z" fill="#F7E7A6" ${ln()}/>`;
    case 'stars':
      return [0, 1, 2, 3, 4]
        .map(() => `<circle cx="${20 + r() * 240}" cy="${20 + r() * 90}" r="${1.5 + r() * 1.5}" fill="#F7E7A6"/>`)
        .join('');
    case 'branch':
      return `<path d="M-4 ${GROUND - 30} q140 -14 290 6" stroke="#6B4A30" stroke-width="14" stroke-linecap="round" fill="none"/>`;
    case 'window':
      return `<rect x="150" y="26" width="100" height="120" rx="4" fill="#CFE3F5" ${ln()}/>
        <path d="M200 26 v120 M150 86 h100" ${ln()}/>`;
    case 'tiles':
      return [0, 1, 2, 3, 4, 5, 6]
        .map((i) => `<path d="M${i * 44 - 10} 30 v${GROUND - 60}" stroke="#EFC9A8" stroke-width="2"/>`)
        .join('');
    case 'waves':
      return `<path d="M0 ${GROUND - 26} q17 -10 35 0 t35 0 t35 0 t35 0 t35 0 t35 0 t35 0 t35 0 v26 h-280 z" fill="#7DB7D6" ${ln()}/>`;
    case 'tree':
      return `<path d="M${cx} ${GROUND} v-60" stroke="#6B4A30" stroke-width="10"/>
        <circle cx="${cx}" cy="${GROUND - 84}" r="32" fill="#6DAA55" ${ln()}/>`;
    case 'lighthouse':
      return `<path d="M214 ${GROUND} l8 -140 h24 l8 140 z" fill="${PAPER}" ${ln()}/>
        <path d="M218 ${GROUND - 50} h32 M216 ${GROUND - 94} h36" stroke="#E4553F" stroke-width="12"/>
        <rect x="220" y="${GROUND - 162} " width="28" height="22" fill="#F7E7A6" ${ln()}/>
        <path d="M216 ${GROUND - 162} l18 -14 l18 14 z" fill="#E4553F" ${ln()}/>`;
    case 'rocks':
      return `<path d="M0 ${GROUND} q30 -60 70 -40 q20 -30 50 0 z M190 ${GROUND} q30 -70 90 -30 v30 z" fill="#CFC3B0" ${ln()}/>`;
    case 'gems':
      return `<path d="M${cx} ${GROUND - 4} l8 -12 l8 12 z" fill="#F2B32A" ${ln()}/>`;
    default:
      return '';
  }
}

// --- speech bubbles -----------------------------------------------------------

function wrap(text, max = 15) {
  const words = text.toUpperCase().split(' ');
  const lines = [];
  let cur = '';
  for (const w of words) {
    if ((cur + ' ' + w).trim().length > max && cur) {
      lines.push(cur);
      cur = w;
    } else {
      cur = (cur + ' ' + w).trim();
    }
  }
  if (cur) lines.push(cur);
  return lines;
}

function bubble(text, speakerX, speakerTop) {
  const lines = wrap(text);
  const w = Math.max(...lines.map((l) => l.length)) * 10.6 + 28;
  const h = lines.length * 18 + 16;
  const x = Math.min(Math.max(speakerX - w / 2, 8), PANEL - w - 8);
  const y = 10;
  const tailX = Math.min(Math.max(speakerX, x + 16), x + w - 16);
  const tailY = Math.min(GROUND + speakerTop + 14, y + h + 30);
  const textSvg = lines
    .map((l, i) => `<text x="${x + w / 2}" y="${y + 23 + i * 18}" text-anchor="middle">${esc(l)}</text>`)
    .join('');
  return `
    <path d="M${tailX - 9} ${y + h - 2} L${tailX + 4} ${tailY} L${tailX + 10} ${y + h - 2} z" fill="${PAPER}" ${ln()}/>
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${h / 2.4}" fill="${PAPER}" ${ln()}/>
    <path d="M${tailX - 7} ${y + h - 1.5} h15" stroke="${PAPER}" stroke-width="4"/>
    ${textSvg}`;
}

// --- strips -----------------------------------------------------------------

function character(name, mood, arm, x, flip) {
  const c = CHARACTERS[name](mood, arm);
  return {
    top: c.top,
    svg: `<g transform="translate(${x} ${GROUND}) scale(${flip ? -1 : 1} 1)">${c.svg}</g>`,
  };
}

function panel(comic, beat, index, r) {
  const scene = SCENES[comic.scene];
  const speaker = beat ? beat[0] : -1;
  const isLast = index === 2;
  const xs = [70 + r() * 20, 200 + r() * 20];
  const cast = comic.cast.map((name, i) => {
    let mood = i === speaker ? 'talk' : 'happy';
    if (isLast && i !== speaker) mood = r() < 0.5 ? 'surprised' : 'flat';
    if (!beat) mood = r() < 0.5 ? 'flat' : 'happy';
    return character(name, mood, i === speaker && r() < 0.4, xs[i], i === 1);
  });
  const props = scene.props.map((p) => prop(p, r)).join('');
  return `
    <rect width="${PANEL}" height="${PANEL}" fill="${scene.sky}"/>
    ${props}
    <path d="M0 ${GROUND - 4} q70 -8 140 0 t140 0 V${PANEL} H0 z" fill="${scene.ground}"/>
    ${cast.map((c) => c.svg).join('')}
    ${beat ? bubble(beat[1], xs[speaker] + (speaker === 1 ? -14 : 14), cast[speaker].top) : ''}`;
}

export function stripSize(comic, fourPanel) {
  const n = fourPanel ? 4 : 3;
  return { width: GUTTER + n * (PANEL + GUTTER), height: PANEL + 2 * GUTTER, panels: n };
}

/** An original strip for one comic and day; `day` picks the gag and layout. */
export function stripSvg(comic, day, fourPanel) {
  const r = rng(comic.id * 7919 + day * 104729);
  const gag = comic.gags[day % comic.gags.length];
  const { width, height, panels } = stripSize(comic, fourPanel);
  // Four-panel strips add a silent beat before the punchline
  const beats = panels === 4 ? [gag[0], gag[1], null, gag[2]] : gag;
  const body = beats
    .map((beat, i) => {
      const x = GUTTER + i * (PANEL + GUTTER);
      return `<g transform="translate(${x} ${GUTTER})">
        <clipPath id="p${i}"><rect width="${PANEL}" height="${PANEL}" rx="4"/></clipPath>
        <g clip-path="url(#p${i})">${panel(comic, beat, i === panels - 1 ? 2 : Math.min(i, 1), r)}</g>
        <rect width="${PANEL}" height="${PANEL}" rx="4" fill="none" stroke="${INK}" stroke-width="3.5"/>
      </g>`;
    })
    .join('');
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
  <style>text{font:700 15px 'Comic Neue','Comic Sans MS','Chalkboard SE','DejaVu Sans',sans-serif;fill:${INK};letter-spacing:.5px}</style>
  <rect width="${width}" height="${height}" fill="${PAPER}"/>
  ${body}
</svg>`;
}

/** A round avatar: the comic's lead character on its scene color. */
export function avatarSvg(comic) {
  const scene = SCENES[comic.scene];
  const c = CHARACTERS[comic.cast[0]]('happy', false);
  // Head and shoulders: scale the character up and let its feet fall off the bottom
  const scale = 195 / -c.top;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
  <rect width="200" height="200" fill="${scene.sky}"/>
  <path d="M0 170 q50 -8 100 0 t100 0 V200 H0 z" fill="${scene.ground}"/>
  <g transform="translate(100 ${200 - c.top * scale * 0.22}) scale(${scale})">${c.svg}</g>
</svg>`;
}
