const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const root = path.resolve(__dirname, '../../..');
const res = path.join(root, 'app/src/main/res');
const ink = '#111C23';
const body = 'M111 273 C133 284 148 294 166 291 C182 288 191 277 191 260 L191 204 C191 154 227 118 276 118 C321 118 354 149 354 191 C354 225 333 249 303 263 L303 286 C315 316 310 346 291 368 C270 393 234 407 199 401 C146 393 105 352 105 303 C105 290 107 280 111 273Z';
const bill = 'M347 178 L397 197 Q408 201 404 211 Q402 216 394 218 L346 232 Q337 234 335 225 L335 188 Q335 179 347 178Z';
const eye = 'M308.5 181 A10.5 10.5 0 1 1 287.5 181 A10.5 10.5 0 1 1 308.5 181Z';
const chevronHole = 'M158.72 306 Q164 300 169.28 306 L194.28 328 Q201 334 194.28 340 L169.28 362 Q163 368 158 362 Q152 356 158.72 350 L176.9 334 L158.72 318 Q152 312 158.72 306Z';
const underscoreHole = 'M219 348 H248 A8 8 0 0 1 248 364 H219 A8 8 0 0 1 219 348Z';

const fullColor = `
    <path android:fillColor="#6EE7E0" android:pathData="${body}" />
    <path android:fillColor="#FFC96B" android:pathData="${bill}" />
    <path android:fillColor="${ink}" android:pathData="${eye}" />
    <path android:fillColor="@android:color/transparent" android:strokeColor="${ink}" android:strokeWidth="16" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M164 312 L189 334 L164 356" />
    <path android:fillColor="@android:color/transparent" android:strokeColor="${ink}" android:strokeWidth="16" android:strokeLineCap="round" android:pathData="M219 356 H248" />`;
const monochrome = `
    <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="${body} ${eye} ${chevronHole} ${underscoreHole}" />
    <path android:fillColor="#FFFFFFFF" android:pathData="${bill}" />`;

function vector(paths, compact = false) {
  const viewport = compact ? 320 : 512;
  const content = compact ? `<group android:translateX="-95" android:translateY="-100">${paths}\n    </group>` : paths;
  return `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="${compact ? 36 : 108}dp" android:height="${compact ? 36 : 108}dp"
    android:viewportWidth="${viewport}" android:viewportHeight="${viewport}">
    ${content}
</vector>
`;
}
function write(relative, value) {
  const destination = path.join(res, relative);
  fs.mkdirSync(path.dirname(destination), {recursive:true});
  fs.writeFileSync(destination, value);
}

async function main() {
  const logoSvg = fs.readFileSync(path.join(__dirname, 'duckport-logo.svg'), 'utf8');
  const logoInner = logoSvg.replace(/^<svg[^>]*>/, '').replace(/<\/svg>$/, '');
  const wrap = content => `<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">${content}${logoInner}</svg>`;
  const square = wrap(`<rect width="512" height="512" rx="116" fill="${ink}"/>`);
  const round = wrap(`<circle cx="256" cy="256" r="256" fill="${ink}"/>`);
  const store = wrap(`<rect width="512" height="512" fill="${ink}"/>`);
  for (const [density,size] of Object.entries({mdpi:48,hdpi:72,xhdpi:96,xxhdpi:144,xxxhdpi:192})) {
    await sharp(Buffer.from(square)).resize(size,size).png().toFile(path.join(res,`mipmap-${density}/ic_launcher.png`));
    await sharp(Buffer.from(round)).resize(size,size).png().toFile(path.join(res,`mipmap-${density}/ic_launcher_round.png`));
  }
  await sharp(Buffer.from(store)).png().toFile(path.join(res,'mipmap-xxxhdpi/ic_launcher_512.png'));
  for (const asset of ['ic_launcher_foreground.png','ic_splash_logo.png']) {
    await sharp(Buffer.from(logoSvg)).resize(1024,1024).png().toFile(path.join(res,`drawable/${asset}`));
  }
  write('drawable/duckport_launcher_foreground.xml',vector(fullColor));
  write('drawable/duckport_mark.xml',vector(fullColor,true));
  write('drawable/duckport_launcher_monochrome.xml',vector(monochrome));
  write('drawable/duckport_notification.xml',vector(monochrome,true));
  write('values/colors.xml',`<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">${ink}</color>\n</resources>\n`);
  write('drawable/ic_launcher_background.xml',`<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n    <solid android:color="@color/ic_launcher_background" />\n</shape>\n`);
  for (const version of [26,33]) {
    const adaptive = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/duckport_launcher_foreground" />
${version >= 33 ? '    <monochrome android:drawable="@drawable/duckport_launcher_monochrome" />\n' : ''}</adaptive-icon>
`;
    for (const name of ['ic_launcher','ic_launcher_round']) write(`mipmap-anydpi-v${version}/${name}.xml`,adaptive);
  }
  for (const folder of fs.readdirSync(res).filter(name => name.startsWith('values'))) {
    const stringsFile = path.join(res,folder,'strings.xml');
    if (fs.existsSync(stringsFile)) {
      const original = fs.readFileSync(stringsFile,'utf8');
      const renamed = original.replaceAll('Dioxamine','DuckPort');
      if (original !== renamed) fs.writeFileSync(stringsFile,renamed);
    }
  }
  fs.mkdirSync(path.join(root,'deliverables'),{recursive:true});
  await sharp(Buffer.from(store)).png().toFile(path.join(root,'deliverables/DuckPort-play-store-icon.png'));
  console.log('Updated DuckPort launcher, splash, localized app names, and native vector logo resources.');
}
main().catch(error=>{console.error(error);process.exitCode=1;});
