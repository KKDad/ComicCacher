import {Component, OnInit} from '@angular/core';
import {ContainerComponent, NavBarOption} from './container/container.component';

import {Comic} from '../dto/comic';
import {ComicService} from '../comic.service';

/**
 * Main comic page component that displays the comic container
 */
@Component({
  selector: 'app-comicpage',
  templateUrl: './comicpage.component.html',
  styleUrls: ['./comicpage.component.css'],
  standalone: true,
  imports: [
    ContainerComponent
]
})
export class ComicpageComponent implements OnInit {
  sections: Comic[] = [];
  showNavbar = true;

  constructor(private comicService: ComicService) { }

  ngOnInit(): void {
    // Subscribe to comics data from service
    this.comicService.getComics().subscribe(comics => {
      this.sections = comics;
    });
  }

  /**
   * Handle navbar visibility events from container component
   * @param option Show or Hide navbar option
   */
  handleNavbarEvent(option: NavBarOption): void {
    this.showNavbar = option === NavBarOption.Show;
  }
}