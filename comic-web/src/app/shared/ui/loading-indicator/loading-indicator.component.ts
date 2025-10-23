import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-indicator',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div *ngIf="loading" class="loading-container" [ngClass]="{'overlay': overlay}">
      <mat-spinner [diameter]="diameter" color="primary"></mat-spinner>
      <p *ngIf="loadingText" class="loading-text">{{ loadingText }}</p>
    </div>
  `,
  styles: [`
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--spacing-lg);
    }

    .overlay {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: var(--glass-background);
      backdrop-filter: blur(var(--glass-blur));
      -webkit-backdrop-filter: blur(var(--glass-blur));
      z-index: 10;
    }

    .loading-text {
      margin-top: var(--spacing-sm);
      font-size: var(--font-size-md);
      color: var(--text-secondary-color);
    }
  `]
})
export class LoadingIndicatorComponent {
  @Input() loading = false;
  @Input() overlay = true;
  @Input() diameter = 50;
  @Input() loadingText?: string;
}