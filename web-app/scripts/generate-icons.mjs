import sharp from 'sharp';
import fs from 'fs';
import path from 'path';

const input = path.join('public', 'favicon.png');
const outputDir = path.join('public', 'icons');
const sizes = [72, 96, 128, 144, 152, 192, 384, 512];

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

async function generate() {
  try {
    const image = sharp(input);
    const metadata = await image.metadata();
    console.log(`Source: ${metadata.width}x${metadata.height}`);

    for (const size of sizes) {
      const out = path.join(outputDir, `icon-${size}.png`);
      await image
        .resize(size, size, { fit: 'contain', background: { r: 26, g: 26, b: 26, alpha: 1 } })
        .png()
        .toFile(out);
      console.log(`✓ ${size}x${size}`);
    }

    console.log('All icons generated in public/icons/');
  } catch (err) {
    console.error('Error:', err);
    process.exit(1);
  }
}

generate();