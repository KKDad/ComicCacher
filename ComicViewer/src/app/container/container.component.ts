import { Component, OnInit, Input, OnChanges, ElementRef, HostListener } from '@angular/core';
import {SectionComponent} from '../section/section.component'

import { Comic } from '../dto/comic';
import { ComicService } from '../comic.service';

@Component({
    selector: 'container',
    templateUrl: 'container.component.html',
    styleUrls: ['container.component.css']
})
export class ContainerComponent implements OnInit {

    private sectionsIndex: any = [];  
    @Input()  sections: Comic[];
    
    constructor( private el: ElementRef, private comicService: ComicService) { }

    ngOnInit() {
        this.getComicSections();
    }

    sectionPosition($event) {
        //filter out the old position if it has been set
        this.sectionsIndex = this.sectionsIndex.filter(item => item.name != $event.name);
        //set the new position
        this.sectionsIndex.push($event);
        //sort the section based on their apperance order 
        this.sectionsIndex.sort((a: any, b: any) => {
            return b.position - a.position;
        });
    }

    getComicSections(): void {
        this.comicService.getComics()
            .subscribe(comics => this.sections = comics);
      }     
}