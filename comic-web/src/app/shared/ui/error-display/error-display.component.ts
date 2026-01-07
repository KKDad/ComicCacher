import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';

@Component({
  selector: 'app-error-display',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    @if (errorMessage) {
      <div class="error-container" [ngClass]="{'dismissible': dismissible}">
        <mat-card appearance="outlined" class="error-card">
          <mat-card-content>
            <div class="error-content">
              <mat-icon color="warn">error</mat-icon>
              <p class="error-message">{{ errorMessage }}</p>
              @if (dismissible) {
                <button
                  mat-icon-button
                  class="dismiss-button"
                  (click)="onDismiss()">
                  <mat-icon>close</mat-icon>
                </button>
              }
            </div>
          </mat-card-content>
          @if (retryable) {
            <mat-card-actions>
              <button mat-button color="primary" (click)="onRetry()">RETRY</button>
            </mat-card-actions>
          }
        </mat-card>
      </div>
    }
    `,
  styles: [`
    .error-container {
      margin: var(--spacing-sm) 0;
      max-width: 100%;
    }

    .error-card {
      background: rgba(255, 182, 193, 0.1);
      border-left: 4px solid var(--accent-color);
      border-radius: var(--border-radius-md);
    }

    .error-content {
      display: flex;
      align-items: center;
    }

    .error-message {
      margin-left: var(--spacing-sm);
      flex-grow: 1;
      color: var(--text-color);
    }

    .dismissible {
      position: relative;
    }

    .dismiss-button {
      position: absolute;
      top: var(--spacing-xs);
      right: var(--spacing-xs);
    }
  `]
})
export class ErrorDisplayComponent {
  @Input() errorMessage: string | null = null;
  @Input() dismissible = true;
  @Input() retryable = false;
  
  @Output() dismiss = new EventEmitter<void>();
  @Output() retry = new EventEmitter<void>();
  
  onDismiss(): void {
    this.dismiss.emit();
  }
  
  onRetry(): void {
    this.retry.emit();
  }
}