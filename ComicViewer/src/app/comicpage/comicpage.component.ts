import { Component, OnInit } from '@angular/core';
import { ContainerComponent } from './container/container.component';
import { SectionComponent  } from './section/section.component';

@Component({
  selector: 'app-comicpage',
  templateUrl: './comicpage.component.html',
  styleUrls: ['./comicpage.component.css']
})
export class ComicpageComponent implements OnInit {

  constructor() { }

  sections = null;

  ngOnInit() {
  }

}
