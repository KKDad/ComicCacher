import {AfterViewInit, Component, EventEmitter, Input, OnDestroy, OnInit, Output, signal, ViewChild} from '@angular/core';

import {Comic} from '../../dto/comic';
import {ComicService} from '../../comic.service';
import {CdkScrollable, CdkVirtualScrollViewport, ScrollDispatcher, ScrollingModule} from '@angular/cdk/scrolling';
import {CommonModule} from '@angular/common';
import {SectionComponent} from '../section/section.component';
import {LoadingIndicatorComponent} from '../../shared/ui/loading-indicator/loading-indicator.component';
import {ErrorDisplayComponent} from '../../shared/ui/error-display/error-display.component';
import {KeyboardService} from '../../shared/a11y/keyboard-service';
import {Subscription} from 'rxjs';

export enum NavBarOption {
    Hide,
    Show
}

@Component({
    selector: 'app-container',
    templateUrl: 'container.component.html',
    styleUrls: ['container.component.css'],
    standalone: true,
    imports: [
        CommonModule,
        ScrollingModule,
        SectionComponent,
        LoadingIndicatorComponent,
        ErrorDisplayComponent
    ]
})
export class ContainerComponent implements OnInit, AfterViewInit, OnDestroy {
    @Input() sections: Comic[];
    @Output() scrollinfo = new EventEmitter<NavBarOption>();
    @ViewChild(CdkVirtualScrollViewport) viewport: CdkVirtualScrollViewport;

    // UI state
    loading = signal(false);
    error = signal<string | null>(null);
    lastOffset: number;

    // Keyboard shortcuts subscription
    private keyboardSubscription: Subscription;

    /** Approximate height of a comic card for virtual scrolling */
    private readonly COMIC_CARD_HEIGHT = 550;

    constructor(
        private scrollDispatcher: ScrollDispatcher,
        private comicService: ComicService,
        private keyboardService: KeyboardService
    ) { }

    ngOnInit() {
        this.loading.set(true);
        this.error.set(null);

        this.comicService.getComics().subscribe({
            next: (comics) => {
                // Defensive check: ensure we received a valid array
                if (!Array.isArray(comics)) {
                    console.error('Invalid comics data received:', comics);
                    this.error.set('Invalid data format received from server');
                    this.sections = [];
                    this.loading.set(false);
                    return;
                }

                this.sections = comics;
                this.loading.set(false);
                console.log(`Successfully loaded ${comics.length} comics`);
            },
            error: (err) => {
                console.error('Failed to load comics:', err);
                this.error.set(`Failed to load comics: ${err.message}`);
                this.loading.set(false);
            }
        });
    }

    refreshComics() {
        this.loading.set(true);
        this.error.set(null);

        try {
            this.comicService.refresh();
            setTimeout(() => {
                this.loading.set(false);
            }, 1000);
        } catch (err) {
            this.error.set('Error refreshing comics');
            this.loading.set(false);
        }
    }

    clearError() {
        this.error.set(null);
    }

    ngAfterViewInit(): void {
        this.scrollDispatcher.scrolled(10).subscribe((data: CdkScrollable | void) => {
            if (data) {
                this.onWindowScroll(data);
            }
        });

        // Register PageUp/PageDown keyboard shortcuts for scrolling between comics
        this.keyboardSubscription = this.keyboardService.registerComicListScrollShortcuts(
            () => this.scrollUpByOneComic(),
            () => this.scrollDownByOneComic()
        );
    }

    ngOnDestroy(): void {
        // Clean up keyboard shortcuts subscription
        if (this.keyboardSubscription) {
            this.keyboardSubscription.unsubscribe();
        }
    }

    private onWindowScroll(data: CdkScrollable) {
        const scrollTop = data.getElementRef().nativeElement.scrollTop || 0;
        if (this.lastOffset > scrollTop || scrollTop < 10) {
            this.scrollinfo.emit(NavBarOption.Hide);
        } else if (scrollTop > 100) {
            this.scrollinfo.emit(NavBarOption.Hide);
        }

        this.lastOffset = scrollTop;
      }

      /**
       * Dynamic item size function for virtualization
       * Returns the height of each comic card in the virtual scroll viewport
       */
      itemSizeFn = (index: number): number => {
        return this.COMIC_CARD_HEIGHT;
      };

      /**
       * Track comics by their ID for better performance in virtual scrolling
       * @param index The index of the item in the list
       * @param comic The comic item to track
       * @returns The comic ID or index if ID is not available
       */
      trackByComicId(index: number, comic: Comic): number {
        return comic?.id || index;
      }

      /**
       * Check if the comics list is empty or invalid
       * Defensive check to handle invalid data structures
       * @returns True if comics are not available or empty
       */
      isComicsEmpty(): boolean {
        return !this.sections || !Array.isArray(this.sections) || this.sections.length === 0;
      }

      /**
       * Scroll up by one comic section (PageUp key)
       * Intelligently snaps to the top of the previous comic card and focuses it
       */
      private scrollUpByOneComic(): void {
        if (!this.viewport) {
            return;
        }

        const currentOffset = this.viewport.measureScrollOffset();
        const viewportElement = this.viewport.elementRef.nativeElement;
        const comicElements = viewportElement.querySelectorAll('.comic-item');

        if (comicElements.length === 0) {
            return;
        }

        // Find which comic is currently at the top of viewport
        let currentComicIndex = -1;
        let currentComicTop = 0;

        for (let i = 0; i < comicElements.length; i++) {
            const element = comicElements[i] as HTMLElement;
            const elementTop = element.offsetTop;
            const elementBottom = elementTop + element.offsetHeight;

            // Check if this comic is at or near the top of the viewport
            if (elementTop <= currentOffset && elementBottom > currentOffset) {
                currentComicIndex = i;
                currentComicTop = elementTop;
                break;
            }
        }

        // Determine target comic
        let targetOffset = 0;
        let targetComicIndex = 0;

        if (currentComicIndex === -1) {
            // No comic found at current position, scroll to top
            targetOffset = 0;
            targetComicIndex = 0;
        } else if (currentOffset - currentComicTop > 10) {
            // Current comic is partially scrolled - snap to its top
            targetOffset = currentComicTop;
            targetComicIndex = currentComicIndex;
        } else if (currentComicIndex > 0) {
            // Already aligned, go to previous comic
            const prevElement = comicElements[currentComicIndex - 1] as HTMLElement;
            targetOffset = prevElement.offsetTop;
            targetComicIndex = currentComicIndex - 1;
        } else {
            // Already at first comic, scroll to top
            targetOffset = 0;
            targetComicIndex = 0;
        }

        this.viewport.scrollToOffset(targetOffset, 'smooth');

        // Focus the target comic after a brief delay to allow scrolling to complete
        setTimeout(() => {
            const targetElement = comicElements[targetComicIndex] as HTMLElement;
            const sectionElement = targetElement.querySelector('app-section');
            if (sectionElement) {
                (sectionElement as HTMLElement).focus();
            }
        }, 300);
      }

      /**
       * Scroll down by one comic section (PageDown key)
       * Intelligently snaps to the top of the next comic card and focuses it
       */
      private scrollDownByOneComic(): void {
        if (!this.viewport) {
            return;
        }

        const currentOffset = this.viewport.measureScrollOffset();
        const viewportElement = this.viewport.elementRef.nativeElement;
        const comicElements = viewportElement.querySelectorAll('.comic-item');

        if (comicElements.length === 0) {
            return;
        }

        // Find which comic is currently at the top of viewport
        let currentComicIndex = -1;
        let currentComicTop = 0;

        for (let i = 0; i < comicElements.length; i++) {
            const element = comicElements[i] as HTMLElement;
            const elementTop = element.offsetTop;
            const elementBottom = elementTop + element.offsetHeight;

            // Check if this comic is at or near the top of the viewport
            if (elementTop <= currentOffset && elementBottom > currentOffset) {
                currentComicIndex = i;
                currentComicTop = elementTop;
                break;
            }
        }

        // Determine target comic
        let targetOffset: number;
        let targetComicIndex: number | null = null;

        if (currentComicIndex === -1) {
            // No comic found, scroll by one card height
            targetOffset = currentOffset + this.COMIC_CARD_HEIGHT;
            // Find which comic we'll land on
            for (let i = 0; i < comicElements.length; i++) {
                const element = comicElements[i] as HTMLElement;
                if (element.offsetTop >= targetOffset) {
                    targetComicIndex = i;
                    break;
                }
            }
        } else if (currentOffset - currentComicTop > 10) {
            // Current comic is partially scrolled - snap to its top first
            targetOffset = currentComicTop;
            targetComicIndex = currentComicIndex;
        } else if (currentComicIndex < comicElements.length - 1) {
            // Already aligned, go to next comic
            const nextElement = comicElements[currentComicIndex + 1] as HTMLElement;
            targetOffset = nextElement.offsetTop;
            targetComicIndex = currentComicIndex + 1;
        } else {
            // Already at last comic, stay there
            return;
        }

        this.viewport.scrollToOffset(targetOffset, 'smooth');

        // Focus the target comic after a brief delay to allow scrolling to complete
        if (targetComicIndex !== null) {
            setTimeout(() => {
                const targetElement = comicElements[targetComicIndex!] as HTMLElement;
                const sectionElement = targetElement.querySelector('app-section');
                if (sectionElement) {
                    (sectionElement as HTMLElement).focus();
                }
            }, 300);
        }
      }

}
