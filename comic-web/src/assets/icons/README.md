# App Icons

This directory is reserved for optional PWA and mobile app icons in PNG format.

## Current Implementation (2025)

The application currently uses a modern **SVG favicon** located at `/Users/agilbert/kkdad/ComicCacher/comic-web/src/favicon.svg`.

SVG favicons work in all modern browsers (Chrome, Firefox, Safari, Edge) and scale perfectly at any size without needing multiple PNG files.

## Optional: Generate PNG Icons for PWA Support

If you want full Progressive Web App (PWA) support for older devices or want app icons for mobile home screens, you can generate PNG files from the SVG:

### Required Icons:

#### Apple Touch Icon
- `apple-touch-icon.png` - 180x180px

#### Android/PWA Icons
- `icon-72x72.png` - 72x72px
- `icon-96x96.png` - 96x96px
- `icon-128x128.png` - 128x128px
- `icon-144x144.png` - 144x144px
- `icon-152x152.png` - 152x152px
- `icon-192x192.png` - 192x192px
- `icon-384x384.png` - 384x384px
- `icon-512x512.png` - 512x512px

### How to Generate

Use ImageMagick to convert the SVG to PNG at various sizes:

```bash
cd /Users/agilbert/kkdad/ComicCacher/comic-web/src
magick favicon.svg -resize 72x72 assets/icons/icon-72x72.png
magick favicon.svg -resize 96x96 assets/icons/icon-96x96.png
magick favicon.svg -resize 128x128 assets/icons/icon-128x128.png
magick favicon.svg -resize 144x144 assets/icons/icon-144x144.png
magick favicon.svg -resize 152x152 assets/icons/icon-152x152.png
magick favicon.svg -resize 180x180 assets/icons/apple-touch-icon.png
magick favicon.svg -resize 192x192 assets/icons/icon-192x192.png
magick favicon.svg -resize 384x384 assets/icons/icon-384x384.png
magick favicon.svg -resize 512x512 assets/icons/icon-512x512.png
```

After generating PNG files, update `manifest.json` to reference them.

## Design

The icon features a **golden smiling face** design:
- Vectorized from original favicon.ico
- Golden yellow color (#FFD700)
- Cheerful, friendly, recognizable at any size
- Scalable SVG format (3KB vs 79KB PNG)
