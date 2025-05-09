# ComicViewer

Frontend application for the ComicCacher project. This project was originally generated with [Angular CLI](https://github.com/angular/angular-cli) version 7.2.3, and has been modernized to Angular 18.

## Features

- Modern Angular 18 application with standalone component architecture
- Signal-based state management
- Virtual scrolling for efficient rendering of many comics
- Responsive design that works well on mobile, tablet and desktop
- Improved accessibility with keyboard navigation and ARIA attributes
- Optimized build configuration for better performance
- Comprehensive test coverage with component interaction testing

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

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

1. **Angular Version**: Updated to Angular 18 with modern build system
2. **Standalone Components**: All components now use the standalone architecture
3. **Modern DI**: Services use the new dependency injection system with `providedIn: 'root'`
4. **Signals for State**: Implemented signal-based state management
5. **Improved HTTP**: Uses modern HTTP patterns with better error handling
6. **Virtual Scrolling**: Enhanced scroll virtualization with CDK virtual scroll
7. **UI Enhancements**: Added loading indicators and improved error handling
8. **Responsive Design**: Better support for various device sizes
9. **Accessibility**: Added ARIA attributes and keyboard navigation
10. **Build Optimization**: Modern bundling and optimized production builds
11. **Testing Framework**: Updated testing utilities for standalone components
12. **Test Coverage**: Expanded test coverage with component interaction testing

## Docker

To build a Docker image:
```bash
./build-docker.sh
```