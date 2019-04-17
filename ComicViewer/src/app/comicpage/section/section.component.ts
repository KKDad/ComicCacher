import { Component, OnInit, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { Comic } from '../../dto/comic'
import { ComicService } from '../../comic.service';
import { DomSanitizer} from '@angular/platform-browser';

import { Observable, of } from 'rxjs';

@Component({
    selector: 'section',
    templateUrl: 'section.component.html',
    styleUrls: ['section.component.css']
  
})
export class SectionComponent implements OnInit {   

    @Input()  content: Comic;

    width: Number;
    height: Number;
    imageDate: String;

    constructor(private element: ElementRef, private comicService: ComicService, private sanitizer: DomSanitizer) {}


    ngOnInit() {
        this.onNavigateLast();
        this.comicService.getAvatar(this.content.id).subscribe(imagedto => {
            this.content.avatar = 'data:' + imagedto.mimeType + ';base64,' + imagedto.imageData;
        });         
    }

    getAvatarImage() {
        return this.sanitizer.bypassSecurityTrustResourceUrl(this.content.avatar);     
    }

    getComicImage() {       
        return this.sanitizer.bypassSecurityTrustResourceUrl(this.content.strip);     
    }
    
    onNavigateFirst() {
        this.comicService.getEarliest(this.content.id).subscribe(imagedto => { this.setStrip(imagedto); });
    }
    onPrev() {
        this.comicService.getPrev(this.content.id, this.imageDate).subscribe(imagedto => { this.setStrip(imagedto); });
    }
    onNext() {
        this.comicService.getNext(this.content.id, this.imageDate).subscribe(imagedto => { this.setStrip(imagedto); });
    }

    onNavigateLast() {
        this.comicService.getLatest(this.content.id).subscribe(imagedto => { this.setStrip(imagedto); });
    }

    private setStrip(imagedto: import("c:/git/ComicCacher/ComicViewer/src/app/dto/image").ImageDto) {
        this.content.strip = 'data:' + imagedto.mimeType + ';base64,' + imagedto.imageData;
        this.height = imagedto.height;
        this.width = imagedto.width;
        this.imageDate = imagedto.imageDate;
        //console.log(`${this.content.name}: Image size is ${this.width}x${this.height}.`);
    }
    
}