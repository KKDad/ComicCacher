import { Component, OnInit, Input } from '@angular/core';

import { Comic } from '../dto/comic';
import { ComicService } from '../comic.service';

@Component({
    selector: 'container',
    templateUrl: 'container.component.html',
    styleUrls: ['container.component.css']
})
export class ContainerComponent implements OnInit 
{    
    @Input()  sections: Comic[];
    
    constructor(private comicService: ComicService) { }

    ngOnInit() 
    {
        this.comicService.getComics().subscribe(c => this.sections = c);
        this.comicService.refresh();
    }
}