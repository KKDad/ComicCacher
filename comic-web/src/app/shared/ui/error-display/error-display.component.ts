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
    <div *ngIf="errorMessage" class="error-container" [ngClass]="{'dismissible': dismissible}">
      <mat-card appearance="outlined" class="error-card">
        <mat-card-content>
          <div class="error-content">
            <mat-icon color="warn">error</mat-icon>
            <p class="error-message">{{ errorMessage }}</p>
            <button 
              *ngIf="dismissible" 
              mat-icon-button 
              class="dismiss-button" 
              (click)="onDismiss()">
              <mat-icon>close</mat-icon>
            </button>
          </div>
        </mat-card-content>
        <mat-card-actions *ngIf="retryable">
          <button mat-button color="primary" (click)="onRetry()">RETRY</button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .error-container {
      margin: 10px 0;
      max-width: 100%;
    }
    
    .error-card {
      background-color: #fff8f8;
      border-left: 4px solid #f44336;
    }
    
    .error-content {
      display: flex;
      align-items: center;
    }
    
    .error-message {
      margin-left: 10px;
      flex-grow: 1;
      color: #444;
    }
    
    .dismissible {
      position: relative;
    }
    
    .dismiss-button {
      position: absolute;
      top: 5px;
      right: 5px;
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