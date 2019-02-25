import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';

import { Comic } from '../comic';
import { ComicService } from '../comic.service'

@Component({
  selector: 'app-comic-strip',
  templateUrl: './comic-strip.component.html',
  styleUrls: ['./comic-strip.component.css']
})
export class ComicStripComponent implements OnInit {
  comic: Comic;
 
  constructor(
    private route: ActivatedRoute,
    private comicService: ComicService,
    private location: Location
  ) {}
 
  ngOnInit(): void {
    this.getcomic();
  }
 
  getcomic(): void {
    const id = +this.route.snapshot.paramMap.get('id');
    this.comicService.getComic(id)
      .subscribe(comic => this.comic = comic);
  }
 
  goBack(): void {
    this.location.back();
  }

  save(): void {
    // this.comicService.updateComic(this.comic)
    //   .subscribe(() => this.goBack());
  }  
}