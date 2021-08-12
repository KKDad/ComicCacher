import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';

import { Comic } from '../../dto/comic';
import { ComicService } from '../../comic.service';
import { ScrollDispatcher, CdkScrollable } from '@angular/cdk/scrolling';

export enum NavBarOption
{
    Hide,
    Show
}

@Component({
    selector: 'container',
    templateUrl: 'container.component.html',
    styleUrls: ['container.component.css']
})
export class ContainerComponent implements OnInit 
{             
    @Input()  sections: Comic[];
    @Output() scrollinfo = new EventEmitter();    
    lastOffset: number;  
    
    constructor(private scrollDispatcher: ScrollDispatcher, private comicService: ComicService) { }

    ngOnInit() 
    {
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

}