import {Component, OnInit, ElementRef, EventEmitter, HostListener,Input, Output } from '@angular/core';
import {Section} from '../section'
import { from } from 'rxjs';


@Component({
    selector: 'section',
    templateUrl: 'section.component.html',
    styleUrls: ['section.component.css']
  
})
export class SectionComponent implements OnInit {

    @Output() sectionPosition = new EventEmitter();
    @Input()  content: Section;

    constructor(private element: ElementRef) {}

    ngOnInit() {
        this.sectionPosition.emit({ name: this.content.name, position: this.element.nativeElement.offsetTop });
    }


    @HostListener('window:resize', ['$event'])
    onResize(event) {
      this.sectionPosition.emit({ name: this.content.name, position: this.element.nativeElement.offsetTop });
    }

}