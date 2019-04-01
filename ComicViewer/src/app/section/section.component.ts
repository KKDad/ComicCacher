import { Component, OnInit, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { Comic } from '../dto/comic'
import { ComicService } from '../comic.service';
import { DomSanitizer} from '@angular/platform-browser';

import { Observable, of } from 'rxjs';

@Component({
    selector: 'section',
    templateUrl: 'section.component.html',
    styleUrls: ['section.component.css']
  
})
export class SectionComponent implements OnInit {   

    @Output() sectionPosition = new EventEmitter();
    @Input()  content: Comic;

    width: Number;
    height: Number;

    constructor(private element: ElementRef, private comicService: ComicService, private sanitizer: DomSanitizer) {}


    ngOnInit() {
        this.sectionPosition.emit({ name: this.content.name, position: this.element.nativeElement.offsetTop });
        this.content.strip = 'assets/images/loading_double_helix.gif';
        this.comicService.getLatest(this.content.id).subscribe(imagedto => {
            this.content.strip = 'data:' + imagedto.mimeType + ';base64,' + imagedto.imageData;
            this.height = imagedto.height;
            this.width = imagedto.width;
            console.log(`${this.content.name}: Image size is ${this.width}x${this.height}.`);                   
        });       
    }


    @HostListener('window:resize', ['$event'])
    onResize(event) {
      this.sectionPosition.emit({ name: this.content.name, position: this.element.nativeElement.offsetTop });
    }

    getSafeUrl() {       
        return this.sanitizer.bypassSecurityTrustResourceUrl(this.content.strip);     
    }   

    getSafeWidth() {       
        return this.sanitizer.bypassSecurityTrustResourceUrl(this.content.strip);     
    }    
}