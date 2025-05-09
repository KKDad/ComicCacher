import { Component, OnInit } from '@angular/core';
import { ContainerComponent } from './container/container.component';
import { SectionComponent } from './section/section.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-comicpage',
  templateUrl: './comicpage.component.html',
  styleUrls: ['./comicpage.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ContainerComponent
  ]
})
export class ComicpageComponent implements OnInit {

  constructor() { }

  sections = null;

  ngOnInit() {
  }

}
