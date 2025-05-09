import { Component, OnInit, ElementRef, Input, inject, signal } from '@angular/core';
import { Comic } from '../../dto/comic';
import { ComicService } from '../../comic.service';
import { DomSanitizer} from '@angular/platform-browser';
import { ImageDto } from '../../dto/image';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { LoadingIndicatorComponent } from '../../shared/ui/loading-indicator/loading-indicator.component';
import { ErrorDisplayComponent } from '../../shared/ui/error-display/error-display.component';
import { ComicStateService } from '../../state/comic-state.service';

@Component({
    selector: 'section',
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
export class SectionComponent implements OnInit {
    @Input() content: Comic;

    private comicService = inject(ComicService);
    private sanitizer = inject(DomSanitizer);
    private stateService = inject(ComicStateService);

    max_width = 900;

    width: number;
    height: number;
    imageDate: string;

    // UI state signals
    loading = signal(false);
    error = signal<string | null>(null);

    constructor(private element: ElementRef) {}


    ngOnInit() {
        this.loadAvatar();
        this.onNavigateLast();
    }

    getAvatarImage() {
        return this.sanitizer.bypassSecurityTrustResourceUrl(this.content.avatar);
    }

    getComicImage() {
        return this.sanitizer.bypassSecurityTrustResourceUrl(this.content.strip);
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

    // Set the current strip with proper scaling
    private setStrip(imagedto: ImageDto) {
        if (!imagedto || !imagedto.imageData) {
            this.error.set('No image data available');
            return;
        }

        try {
            this.content.strip = 'data:' + imagedto.mimeType + ';base64,' + imagedto.imageData;

            if (imagedto.width > this.max_width) {
                this.width = this.max_width;
                const scale_factor = imagedto.width.valueOf() / this.max_width;
                this.height = imagedto.height.valueOf() / scale_factor;
            } else {
                this.width = imagedto.width.valueOf();
                this.height = imagedto.height.valueOf();
            }

            this.imageDate = imagedto.imageDate.valueOf();
        } catch (err) {
            this.error.set('Error processing image data');
            console.error('Error setting strip:', err);
        }
    }

}
