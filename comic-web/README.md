# ComicViewer

Frontend application for the ComicCacher project. This project was originally generated with [Angular CLI](https://github.com/angular/angular-cli) version 7.2.3, and has been modernized to Angular 19.

## Features

- Modern Angular 19 application with standalone component architecture
- Signal-based state management
- Virtual scrolling for efficient rendering of many comics
- Responsive design that works well on mobile, tablet and desktop
- Improved accessibility with keyboard navigation and ARIA attributes
- Optimized build configuration for better performance
- Comprehensive test coverage with component interaction testing

## Requirements

- **Node.js 22 LTS** (or later) - The project uses `.nvmrc` to specify the required Node version
- npm 10.x or later

## Development server

If you're using nvm, first ensure you're on the correct Node version:
```bash
nvm use
```

Then run the development server:
```bash
ng serve
# or
npm start
```

Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Running tests

Run `npm test` to execute the unit tests via [Karma](https://karma-runner.github.io).

For headless testing (useful in CI/CD pipelines), use:
```bash
npm test -- --no-watch --browsers=ChromeHeadless
```

To run tests for specific components or services:
```bash
# Run tests for a specific component
npm test -- --include=src/app/comicpage/comicpage.component.spec.ts --no-watch

# Run tests for a specific service
npm test -- --include=src/app/comic.service.spec.ts --no-watch
```

## Test Coverage

The application has comprehensive unit tests with a focus on:

- **Service Tests**: Complete coverage of all service methods, including error handling and edge cases
- **Component Tests**: Testing both standalone functionality and component interactions
- **State Management Tests**: Verification of signal-based state updates
- **Event Handling Tests**: Testing of event propagation between components
- **Mock Objects**: Proper mocking of dependencies for isolated testing

## Build

Run `npm run build` to build the project. The build artifacts will be stored in the `dist/` directory.

For production builds with optimization:
```bash
npm run buildProd
```

## Modernization

The application has been modernized with the following improvements:

1. **Angular Version**: Updated to Angular 19.2 with latest build system
2. **Node.js**: Upgraded to Node 22 LTS for improved performance and security
3. **TypeScript**: Updated to TypeScript 5.8 for better type safety
4. **Standalone Components**: All components use the standalone architecture (no NgModules)
5. **Modern DI**: Services use dependency injection with `providedIn: 'root'`
6. **Signals for State**: Implemented signal-based state management (Angular 19 feature)
7. **Improved HTTP**: Uses modern HTTP patterns with better error handling
8. **Virtual Scrolling**: Enhanced scroll virtualization with CDK virtual scroll
9. **UI Enhancements**: Loading indicators and improved error handling with Material Design
10. **Responsive Design**: Mobile-first design supporting all device sizes
11. **Accessibility**: Full ARIA attributes and keyboard navigation support
12. **Build Optimization**: Modern esbuild-based bundler for faster builds
13. **Testing Framework**: Updated to latest Jasmine and Karma
14. **Test Coverage**: Comprehensive unit tests with component interaction testing

## Docker

To build a Docker image:
```bash
./build-docker.sh
```