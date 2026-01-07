/**
 * Test setup file for Vitest with jsdom
 * Provides browser API mocks that jsdom doesn't include
 */

// Mock IntersectionObserver - not available in jsdom
class MockIntersectionObserver {
    readonly root: Element | null = null;
    readonly rootMargin: string = '';
    readonly thresholds: ReadonlyArray<number> = [];

    constructor(private callback: IntersectionObserverCallback) { }

    observe(_target: Element): void { }
    unobserve(_target: Element): void { }
    disconnect(): void { }
    takeRecords(): IntersectionObserverEntry[] {
        return [];
    }
}

globalThis.IntersectionObserver = MockIntersectionObserver as any;

// Mock ResizeObserver if needed
class MockResizeObserver {
    constructor(_callback: ResizeObserverCallback) { }
    observe(_target: Element): void { }
    unobserve(_target: Element): void { }
    disconnect(): void { }
}

globalThis.ResizeObserver = MockResizeObserver as any;
