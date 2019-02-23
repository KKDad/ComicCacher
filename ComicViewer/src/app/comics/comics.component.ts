import { Component, OnInit } from '@angular/core';
import { Comic } from '../comic';
import { COMICS } from '../mock-comics';

@Component({
  selector: 'app-comics',
  templateUrl: './comics.component.html',
  styleUrls: ['./comics.component.css']
})

export class ComicsComponent implements OnInit {
  
  comics = COMICS;
  currentComic: Comic;

  constructor() { }

  ngOnInit() {
  }

  onSelect(comic: Comic): void {
    this.currentComic = comic;
  }

}