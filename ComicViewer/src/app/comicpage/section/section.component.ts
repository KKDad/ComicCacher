import { Component, OnInit, ElementRef, Input } from '@angular/core';
import { Comic } from '../../dto/comic';
import { ComicService } from '../../comic.service';
import { DomSanitizer} from '@angular/platform-browser';
import { ImageDto } from '../../dto/image';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'section',
    templateUrl: 'section.component.html',
    styleUrls: ['section.component.css'],
    standalone: true,
    imports: [
        CommonModule,
        MatCardModule,
        MatButtonModule
    ]
})
export class SectionComponent implements OnInit {

    @Input()  content: Comic;

    max_width = 900;

    width: number;
    height: number;
    imageDate: string;

    constructor(private element: ElementRef, private comicService: ComicService, private sanitizer: DomSanitizer) {}


    ngOnInit() {
        this.onNavigateLast();
        this.comicService.getAvatar(this.content.id).subscribe(imagedto => {
            if (imagedto && imagedto.mimeType) {
                this.content.avatar = 'data:' + imagedto.mimeType + ';base64,' + imagedto.imageData;
            }
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

    private setStrip(imagedto: ImageDto) {
        if (!imagedto || !imagedto.imageData) {
            return;
        }

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
        // console.log(`${this.content.name}: Image size is ${this.width}x${this.height}.`);
    }

}
