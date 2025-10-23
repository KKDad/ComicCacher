# comic-web

![Comic Viewer Banner](../assets/ComicViewer.png)

The frontend for ComicCacher - a web app that downloads and caches daily comic strips from multiple sources into one ad-free page. Built with Angular 19.

## What is this?

A personal project to enjoy my favorite daily comics without ads, pop-ups, or paywalls. Just comics.

The web app features:
- 📱 Responsive design for mobile, tablet, and desktop
- ⚡ Virtual scrolling for smooth performance
- 🎨 Modern glassmorphism UI
- ♿ Keyboard navigation and accessibility
- 🔄 Auto-refresh to fetch the latest strips

## Quick Start

Requirements: **Node.js 22 LTS** (specified in `.nvmrc`)

```bash
# Use the correct Node version
nvm use

# Install dependencies
npm install

# Start the dev server
npm start
```

Visit `http://localhost:4200` and you're good to go!

## Common Commands

```bash
# Run tests
npm test

# Run tests headless (for CI)
npm run test:headless

# Build for production
npm run buildProd

# Lint
npm run lint
```

## Development

For detailed development guidelines, architecture info, testing patterns, and build instructions, see [CLAUDE.md](../CLAUDE.md).

## Docker

Build and run as a container:

```bash
./build-docker.sh
```