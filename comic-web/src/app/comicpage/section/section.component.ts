import {Component, ElementRef, inject, Input, OnDestroy, OnInit, signal} from '@angular/core';
import {Comic} from '../../dto/comic';
import {ComicService} from '../../comic.service';
import {DomSanitizer} from '@angular/platform-browser';
import {ImageDto} from '../../dto/image';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {CommonModule} from '@angular/common';
import {LoadingIndicatorComponent} from '../../shared/ui/loading-indicator/loading-indicator.component';
import {ErrorDisplayComponent} from '../../shared/ui/error-display/error-display.component';
import {ComicStateService} from '../../state/comic-state.service';
import {KeyboardService} from '../../shared/a11y/keyboard-service';
import {Subscription} from 'rxjs';

@Component({
    selector: 'app-section',
    templateUrl: 'section.component.html',
    styleUrls: ['section.component.css'],
    standalone: true,
    imports: [
        CommonModule,
        MatCardModule,
        MatButtonModule,
        LoadingIndicatorComponent,
        ErrorDisplayComponent
    ]
})
export class SectionComponent implements OnInit, OnDestroy {
    @Input() content: Comic;

    private comicService = inject(ComicService);
    private sanitizer = inject(DomSanitizer);
    private stateService = inject(ComicStateService);
    private keyboardService = inject(KeyboardService);

    imageDate: string;

    // UI state signals
    loading = signal(false);
    error = signal<string | null>(null);
    focusVisible = signal(false);
    avatarLoadFailed = signal(false);
    stripLoadFailed = signal(false);

    // Keep track of subscriptions for cleanup
    private subscriptions = new Subscription();

    constructor(private element: ElementRef) {}


    ngOnInit() {
        this.loadAvatar();
        this.onNavigateLast();

        // Set up keyboard shortcuts for navigation
        this.registerKeyboardShortcuts();

        // Listen for focus events
        this.element.nativeElement.addEventListener('focus', () => this.focusVisible.set(true));
        this.element.nativeElement.addEventListener('blur', () => this.focusVisible.set(false));
    }

    /**
     * Clean up subscriptions when component is destroyed
     */
    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    /**
     * Register keyboard shortcuts for comic navigation
     */
    private registerKeyboardShortcuts(): void {
        const shortcuts = this.keyboardService.registerComicNavigationShortcuts(
            () => this.onNavigateFirst(),
            () => this.onPrev(),
            () => this.onNext(),
            () => this.onNavigateLast()
        );
        this.subscriptions.add(shortcuts);
    }

    /**
     * Get the avatar URL, handling Python 'None' string literals
     */
    getAvatarUrl(): string | null {
        const avatar = this.content?.avatar;
        // Handle Python None as string, null, undefined, or empty string
        if (!avatar || avatar === 'None' || avatar === 'null' || avatar === 'undefined') {
            return null;
        }
        return avatar;
    }

    /**
     * Get the strip URL, handling Python 'None' string literals
     */
    getStripUrl(): string | null {
        const strip = this.content?.strip;
        // Handle Python None as string, null, undefined, or empty string
        if (!strip || strip === 'None' || strip === 'null' || strip === 'undefined') {
            return null;
        }
        return strip;
    }

    /**
     * Get sanitized avatar image for template binding
     */
    getAvatarImage() {
        const url = this.getAvatarUrl();
        return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
    }

    /**
     * Get sanitized comic strip image for template binding
     */
    getComicImage() {
        const url = this.getStripUrl();
        return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
    }

    /**
     * Handle avatar image load errors
     */
    onAvatarError(): void {
        this.avatarLoadFailed.set(true);
        console.warn(`Failed to load avatar for comic: ${this.content?.name}`);
    }

    /**
     * Handle comic strip image load errors
     */
    onStripError(): void {
        this.stripLoadFailed.set(true);
        console.warn(`Failed to load strip for comic: ${this.content?.name}`);
    }

    clearError() {
        this.error.set(null);
    }

    // Load avatar image for comic
    loadAvatar() {
        this.comicService.getAvatar(this.content.id).subscribe({
            next: (imagedto) => {
                if (imagedto && imagedto.mimeType) {
                    this.content.avatar = 'data:' + imagedto.mimeType + ';base64,' + imagedto.imageData;
                }
            },
            error: (err) => {
                console.error('Error loading avatar:', err);
                // Don't show error for avatar - non-critical
            }
        });
    }

    // Navigate to first comic strip
    onNavigateFirst() {
        this.loading.set(true);
        this.clearError();

        this.comicService.getEarliest(this.content.id).subscribe({
            next: (imagedto) => {
                this.setStrip(imagedto);
                this.loading.set(false);
            },
            error: (err) => {
                this.error.set(`Could not load first comic: ${err.message}`);
                this.loading.set(false);
            }
        });
    }

    // Navigate to previous comic strip
    onPrev() {
        this.loading.set(true);
        this.clearError();

        this.comicService.getPrev(this.content.id, this.imageDate).subscribe({
            next: (imagedto) => {
                this.setStrip(imagedto);
                this.loading.set(false);
            },
            error: (err) => {
                this.error.set(`Could not load previous comic: ${err.message}`);
                this.loading.set(false);
            }
        });
    }

    // Navigate to next comic strip
    onNext() {
        this.loading.set(true);
        this.clearError();

        this.comicService.getNext(this.content.id, this.imageDate).subscribe({
            next: (imagedto) => {
                this.setStrip(imagedto);
                this.loading.set(false);
            },
            error: (err) => {
                this.error.set(`Could not load next comic: ${err.message}`);
                this.loading.set(false);
            }
        });
    }

    // Navigate to latest comic strip
    onNavigateLast() {
        this.loading.set(true);
        this.clearError();

        this.comicService.getLatest(this.content.id).subscribe({
            next: (imagedto) => {
                this.setStrip(imagedto);
                this.loading.set(false);
            },
            error: (err) => {
                this.error.set(`Could not load latest comic: ${err.message}`);
                this.loading.set(false);
            }
        });
    }

    // Handle retrying the current operation
    onRetry() {
        // Determine which operation to retry based on context
        // For simplicity, we'll just retry the latest strip
        this.onNavigateLast();
    }

    /**
     * Set the current strip - CSS handles responsive sizing automatically
     */
    private setStrip(imagedto: ImageDto) {
        if (!imagedto || !imagedto.imageData) {
            this.error.set('No image data available');
            return;
        }

        try {
            this.content.strip = 'data:' + imagedto.mimeType + ';base64,' + imagedto.imageData;
            this.imageDate = imagedto.imageDate.valueOf();
        } catch (err) {
            this.error.set('Error processing image data');
            console.error('Error setting strip:', err);
        }
    }

}
