import {Component, EventEmitter, Input, Output} from '@angular/core';

/**
 * Stub components for testing.
 * These components provide mock implementations of real components without
 * their actual behavior, making testing simpler and more isolated.
 */

@Component({
  selector: 'app-section',
  template: '<div>Mock Section Component</div>',
  standalone: true
})
export class SectionStubComponent {
  @Input() comicId: number;
  @Input() comicName: string;
  @Input() comicDescription: string;
  @Output() sectionError = new EventEmitter<string>();
}

@Component({
  selector: 'app-loading-indicator',
  template: '<div class="loading">Loading...</div>',
  standalone: true
})
export class LoadingIndicatorStubComponent {}

@Component({
  selector: 'app-error-display',
  template: '<div class="error">{{error}}</div>',
  standalone: true
})
export class ErrorDisplayStubComponent {
  @Input() error: string | null = null;
  @Output() clearError = new EventEmitter<void>();
}

@Component({
  selector: 'cdk-virtual-scroll-viewport',
  template: '<div class="virtual-scroll"><ng-content></ng-content></div>',
  standalone: true
})
export class VirtualScrollViewportStubComponent {
  @Input() itemSize: number | ((index: number) => number);
}