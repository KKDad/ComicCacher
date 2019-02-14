import { Component, OnInit } from '@angular/core';
import { COMICS } from '../mock-comics';

@Component({
  selector: 'app-comics',
  templateUrl: './comics.component.html',
  styleUrls: ['./comics.component.css']
})

export class ComicsComponent implements OnInit {
  
  comics = COMICS;

  constructor() { }

  ngOnInit() {
  }

}
