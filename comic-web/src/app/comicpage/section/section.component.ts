import {ChangeDetectionStrategy, Component, ElementRef, inject, Input, OnDestroy, OnInit, signal} from '@angular/core';
import {Comic} from '../../dto/comic';
import {ComicService} from '../../comic.service';
import {DomSanitizer} from '@angular/platform-browser';
import {ImageDto} from '../../dto/image';
import {ComicNavigationResult} from '../../dto/comic-navigation-result';
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
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
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

    // Track if images have been loaded
    private imagesLoaded = false;

    // IntersectionObserver for lazy loading
    private intersectionObserver: IntersectionObserver | null = null;

    constructor(private element: ElementRef) {}


    ngOnInit() {
        // Initialize imageDate to prevent undefined being passed to API
        if (this.content?.newest) {
            this.imageDate = this.content.newest;
        }

        this.setupLazyLoading();
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

        // Clean up IntersectionObserver
        if (this.intersectionObserver) {
            this.intersectionObserver.disconnect();
            this.intersectionObserver = null;
        }
    }

    /**
     * Set up lazy loading with IntersectionObserver
     * Images are only loaded when the comic card enters the viewport
     */
    private setupLazyLoading(): void {
        // Create IntersectionObserver with a threshold
        // Load when 10% of the card is visible
        const options = {
            root: null, // Use viewport as root
            rootMargin: '200px', // Start loading 200px before entering viewport
            threshold: 0.1 // Trigger when 10% visible
        };

        this.intersectionObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                // Load images when card becomes visible
                if (entry.isIntersecting && !this.imagesLoaded) {
                    this.imagesLoaded = true;
                    this.loadImagesLazily();

                    // Stop observing once images are loaded
                    if (this.intersectionObserver) {
                        this.intersectionObserver.unobserve(entry.target);
                    }
                }
            });
        }, options);

        // Observe this component's element
        this.intersectionObserver.observe(this.element.nativeElement);
    }

    /**
     * Load avatar and comic strip images lazily
     * Called when the comic card becomes visible in the viewport
     */
    private loadImagesLazily(): void {
        this.loadAvatar();
        this.onNavigateLast();
    }

    /**
     * Register keyboard shortcuts for comic strip navigation
     * Arrow keys navigate between strips (dates), PageUp/PageDown scroll between comics
     */
    private registerKeyboardShortcuts(): void {
        const shortcuts = this.keyboardService.registerComicStripNavigationShortcuts(
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
            next: (result) => {
                if (result.found && result.image) {
                    this.setStrip(result.image);
                } else {
                    this.handleNavigationBoundary(result);
                }
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
        // Defensive check: ensure imageDate is initialized before API call
        if (!this.imageDate) {
            console.warn('imageDate not initialized, falling back to newest date');
            this.imageDate = this.content?.newest || '';
            if (!this.imageDate) {
                this.error.set('Cannot navigate: comic date not available');
                return;
            }
        }

        this.loading.set(true);
        this.clearError();

        this.comicService.getPrev(this.content.id, this.imageDate).subscribe({
            next: (result) => {
                if (result.found && result.image) {
                    this.setStrip(result.image);
                } else {
                    this.handleNavigationBoundary(result);
                }
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
        // Defensive check: ensure imageDate is initialized before API call
        if (!this.imageDate) {
            console.warn('imageDate not initialized, falling back to newest date');
            this.imageDate = this.content?.newest || '';
            if (!this.imageDate) {
                this.error.set('Cannot navigate: comic date not available');
                return;
            }
        }

        this.loading.set(true);
        this.clearError();

        this.comicService.getNext(this.content.id, this.imageDate).subscribe({
            next: (result) => {
                if (result.found && result.image) {
                    this.setStrip(result.image);
                } else {
                    this.handleNavigationBoundary(result);
                }
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
            next: (result) => {
                if (result.found && result.image) {
                    this.setStrip(result.image);
                } else {
                    this.handleNavigationBoundary(result);
                }
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
     * Handle navigation boundary when no image is found
     * Displays a helpful message based on the reason
     */
    private handleNavigationBoundary(result: ComicNavigationResult): void {
        let message = '';

        switch (result.reason) {
            case 'AT_END':
                if (result.nearestPreviousDate) {
                    message = `You're viewing the latest comic (${result.nearestPreviousDate}). Check back tomorrow!`;
                } else {
                    message = 'No more comics available in this direction.';
                }
                break;
            case 'AT_BEGINNING':
                if (result.nearestNextDate) {
                    message = `You're viewing the oldest comic (${result.nearestNextDate}).`;
                } else {
                    message = 'No more comics available in this direction.';
                }
                break;
            case 'NO_COMICS_AVAILABLE':
                message = 'No comics available for this comic strip.';
                break;
            case 'ERROR':
            default:
                message = 'Unable to load comic. Please try again.';
                break;
        }

        this.error.set(message);
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
