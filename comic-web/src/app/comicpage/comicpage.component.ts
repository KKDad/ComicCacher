import {Component, OnInit} from '@angular/core';
import {ContainerComponent, NavBarOption} from './container/container.component';
import {CommonModule} from '@angular/common';
import {Comic} from '../dto/comic';
import {ComicService} from '../comic.service';

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
  sections: Comic[] = [];
  showNavbar = true;

  constructor(private comicService: ComicService) { }

  ngOnInit() {
    // Subscribe to comics data from service
    this.comicService.getComics().subscribe(comics => {
      this.sections = comics;
    });
  }

  /**
   * Handle navbar visibility events from container component
   */
  handleNavbarEvent(option: NavBarOption): void {
    this.showNavbar = option === NavBarOption.Show;
  }
}