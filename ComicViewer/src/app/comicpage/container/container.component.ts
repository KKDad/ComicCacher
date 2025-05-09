import { Component, OnInit, Input, Output, EventEmitter, inject, signal } from '@angular/core';

import { Comic } from '../../dto/comic';
import { ComicService } from '../../comic.service';
import { ScrollDispatcher, CdkScrollable, ScrollingModule } from '@angular/cdk/scrolling';
import { CommonModule } from '@angular/common';
import { SectionComponent } from '../section/section.component';
import { LoadingIndicatorComponent } from '../../shared/ui/loading-indicator/loading-indicator.component';
import { ErrorDisplayComponent } from '../../shared/ui/error-display/error-display.component';

export enum NavBarOption {
    Hide,
    Show
}

@Component({
    selector: 'container',
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
export class ContainerComponent implements OnInit {
    @Input() sections: Comic[];
    @Output() scrollinfo = new EventEmitter<NavBarOption>();

    // UI state
    loading = signal(false);
    error = signal<string | null>(null);
    lastOffset: number;

    constructor(private scrollDispatcher: ScrollDispatcher, private comicService: ComicService) { }

    ngOnInit() {
        this.loading.set(true);
        this.error.set(null);

        this.comicService.getComics().subscribe({
            next: (comics) => {
                this.sections = comics;
                this.loading.set(false);
            },
            error: (err) => {
                this.error.set(`Failed to load comics: ${err.message}`);
                this.loading.set(false);
            }
        });

        this.refreshComics();
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
        this.scrollDispatcher.scrolled(10).subscribe((data: CdkScrollable) => { this.onWindowScroll(data); });
    }

    private onWindowScroll(data: CdkScrollable) {
        const scrollTop = data.getElementRef().nativeElement.scrollTop || 0;
        console.log('scrollTop: ', scrollTop);
        if (this.lastOffset > scrollTop || scrollTop < 10) {
            this.scrollinfo.emit(NavBarOption.Hide);
        } else if (scrollTop > 100) {
            this.scrollinfo.emit(NavBarOption.Hide);
        }

        this.lastOffset = scrollTop;
      }

      // Dynamic item size function for virtualization
      itemSizeFn = (index: number) => {
        // Use a more appropriate sizing approach based on content
        // This is just a starting point that can be refined
        return 550; // Approximate height of a comic card
      };

      // Track comics by their ID for better performance
      trackByComicId(index: number, comic: Comic): number {
        return comic?.id || index;
      }

}
