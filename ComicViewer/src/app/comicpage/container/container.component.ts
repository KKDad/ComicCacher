import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';

import { Comic } from '../../dto/comic';
import { ComicService } from '../../comic.service';
import { ScrollDispatcher, CdkScrollable, ScrollingModule } from '@angular/cdk/scrolling';
import { CommonModule } from '@angular/common';
import { SectionComponent } from '../section/section.component';

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
        SectionComponent
    ]
})
export class ContainerComponent implements OnInit {
    @Input()  sections: Comic[];
    @Output() scrollinfo = new EventEmitter<NavBarOption>();
    lastOffset: number;

    constructor(private scrollDispatcher: ScrollDispatcher, private comicService: ComicService) { }

    ngOnInit() {
        this.comicService.getComics().subscribe(c => this.sections = c);
        this.comicService.refresh();
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
