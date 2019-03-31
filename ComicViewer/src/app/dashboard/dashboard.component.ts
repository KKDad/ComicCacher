import { Component, OnInit } from '@angular/core';

import { Comic } from '../dto/comic';
import { ComicService } from '../comic.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  comics: Comic[] = [];
 
  constructor(private comicService: ComicService) {     
  }

  ngOnInit() {
    this.getComics();
  }

  getComics(): void {
    this.comicService.getComics()
      .subscribe(comics => this.comics = comics.slice(1, 5));
  }

}
