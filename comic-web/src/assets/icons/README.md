# App Icons

This directory should contain PWA and mobile app icons in PNG format.

## Required Icons for Full PWA Support:

### Apple Touch Icon
- `apple-touch-icon.png` - 180x180px

### Android/PWA Icons
- `icon-72x72.png` - 72x72px
- `icon-96x96.png` - 96x96px
- `icon-128x128.png` - 128x128px
- `icon-144x144.png` - 144x144px
- `icon-152x152.png` - 152x152px
- `icon-192x192.png` - 192x192px
- `icon-384x384.png` - 384x384px
- `icon-512x512.png` - 512x512px

## How to Generate

Use the `/Users/agilbert/kkdad/ComicCacher/comic-web/src/favicon.svg` as the source and generate PNG files at the sizes listed above.

You can use online tools like:
- https://realfavicongenerator.net/
- https://www.favicon-generator.org/
- ImageMagick: `convert favicon.svg -resize 192x192 icon-192x192.png`

## Design

The icon features:
- Blue gradient background (#5B9BD5 to #7BAEE0)
- Stylized comic book pages with panels
- Pink speech bubble with exclamation mark accent
- Clean, modern, recognizable at small sizes
