const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const out = __dirname;
const ink = '#111C23';
const cyan = '#6EE7E0';
const yellow = '#FFC96B';
const cream = '#F5F5ED';

function duck(body = cyan, details = ink, bill = yellow) {
  return `<g>
    <path fill="${body}" d="M111 273 C133 284 148 294 166 291 C182 288 191 277 191 260 L191 204 C191 154 227 118 276 118 C321 118 354 149 354 191 C354 225 333 249 303 263 L303 286 C315 316 310 346 291 368 C270 393 234 407 199 401 C146 393 105 352 105 303 C105 290 107 280 111 273Z"/>
    <path fill="${bill}" d="M347 178 L397 197 Q408 201 404 211 Q402 216 394 218 L346 232 Q337 234 335 225 L335 188 Q335 179 347 178Z"/>
    <circle cx="298" cy="181" r="10.5" fill="${details}"/>
    <path d="M164 312 L189 334 L164 356" fill="none" stroke="${details}" stroke-width="16" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M219 356 H248" fill="none" stroke="${details}" stroke-width="16" stroke-linecap="round"/>
  </g>`;
}

function svg(content, width = 512, height = 512, label = 'DuckPort logo concept') {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${label}">${content}</svg>`;
}

const logo = svg(duck());
const mono = svg(duck(ink, cream, ink));
const icon = svg(`<rect width="512" height="512" rx="116" fill="${ink}"/>${duck()}`);
const lightIcon = svg(`<rect width="512" height="512" rx="116" fill="${cream}"/>${duck(ink,cream,yellow)}`);

const preview = svg(`
  <rect width="1440" height="1080" fill="${cream}"/>
  <style>text{font-family:'Segoe UI',Arial,sans-serif}.label{font-size:15px;font-weight:700;letter-spacing:2px}.muted{fill:#667373}</style>
  <text x="56" y="60" class="label" fill="${ink}">A NEW IDENTITY FOR YOUR ANDROID TOOLKIT</text>
  <text x="1384" y="60" class="label muted" text-anchor="end">CONCEPT 01</text>
  <rect x="40" y="92" width="1360" height="562" rx="32" fill="${ink}"/>
  <path d="M532 150V595" stroke="#2B3B42"/>
  <g transform="translate(63 138) scale(.91)">${duck()}</g>
  <text x="600" y="246" class="label" fill="${cyan}">MEET YOUR NEW DEVICE SIDEKICK.</text>
  <text x="593" y="365" font-size="114" font-weight="700" letter-spacing="-5" fill="${cream}">DuckPort</text>
  <text x="600" y="424" font-size="30" fill="#AABABC">Connect. Control. Explore.</text>
  <g font-size="16" font-weight="600" fill="${cream}">
    <rect x="600" y="489" width="92" height="42" rx="21" fill="#26353C"/><text x="646" y="516" text-anchor="middle">ADB</text>
    <rect x="704" y="489" width="118" height="42" rx="21" fill="#26353C"/><text x="763" y="516" text-anchor="middle">MIRROR</text>
    <rect x="834" y="489" width="135" height="42" rx="21" fill="#26353C"/><text x="901" y="516" text-anchor="middle">FASTBOOT</text>
  </g>
  <text x="56" y="715" class="label" fill="${ink}">ONE MARK. EVERY SIZE.</text>
  <g transform="translate(56 751) scale(.34)"><rect width="512" height="512" rx="116" fill="${ink}"/>${duck()}</g>
  <g transform="translate(258 751) scale(.34)"><circle cx="256" cy="256" r="256" fill="${ink}"/>${duck()}</g>
  <g transform="translate(460 751) scale(.34)"><rect x="1" y="1" width="510" height="510" rx="116" fill="#EBEDE6"/>${duck(ink,cream,yellow)}</g>
  <g transform="translate(680 804) scale(.105)"><rect width="512" height="512" rx="116" fill="${ink}"/>${duck()}</g>
  <g font-size="16" fill="#586769" text-anchor="middle">
    <text x="143" y="958">App icon</text><text x="345" y="958">Round mask</text><text x="547" y="958">Light edition</text><text x="707" y="958">Small size</text>
  </g>
  <path d="M786 744V974" stroke="#D8DEDA"/>
  <text x="835" y="780" font-size="26" font-weight="700" fill="${ink}">A duck with a command wing.</text>
  <text x="835" y="820" font-size="19" fill="#586769">A friendly silhouette. A familiar terminal cue.</text>
  <text x="835" y="850" font-size="19" fill="#586769">Built to stay clear on a small launcher icon.</text>
  <g transform="translate(835 886)"><circle cx="17" cy="17" r="17" fill="${ink}"/><circle cx="63" cy="17" r="17" fill="${cyan}"/><circle cx="109" cy="17" r="17" fill="${yellow}"/><text x="143" y="24" font-size="17" fill="#586769">Charcoal / Mint / Marigold</text></g>
  <path d="M56 1010H1384" stroke="#D8DEDA"/>
  <text x="56" y="1048" font-size="15" fill="#667373">DUCKPORT / NAME &amp; LOGO EXPLORATION</text>
  <text x="1384" y="1048" font-size="15" fill="#667373" text-anchor="end">Original vector artwork</text>
`,1440,1080,'DuckPort brand concept with duck terminal logo and Android icon previews');

async function main() {
  for (const [name,source] of Object.entries({
    'duckport-logo':logo,
    'duckport-mono':mono,
    'duckport-app-icon':icon,
    'duckport-app-icon-light':lightIcon,
    'duckport-brand-preview':preview,
  })) {
    fs.writeFileSync(path.join(out,`${name}.svg`),source);
    await sharp(Buffer.from(source)).resize(name.includes('brand-preview') ? 1440 : 1024).png().toFile(path.join(out,`${name}.png`));
  }
  await sharp(Buffer.from(icon)).resize(48,48).png().toFile(path.join(out,'duckport-icon-48.png'));
  console.log('Created DuckPort concept SVG and PNG assets in ' + out);
}
main().catch(error=>{console.error(error);process.exitCode=1;});
