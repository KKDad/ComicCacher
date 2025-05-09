import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Comic } from '../dto/comic';

/**
 * Stub components for testing.
 * These components provide mock implementations of real components without
 * their actual behavior, making testing simpler and more isolated.
 */

@Component({
  selector: 'section',
  template: '<div>Mock Section Component</div>',
  standalone: true
})
export class SectionStubComponent {
  @Input() comicId: number;
  @Input() comicName: string;
  @Input() comicDescription: string;
  @Output() error = new EventEmitter<string>();
}

@Component({
  selector: 'loading-indicator',
  template: '<div class="loading">Loading...</div>',
  standalone: true
})
export class LoadingIndicatorStubComponent {}

@Component({
  selector: 'error-display',
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