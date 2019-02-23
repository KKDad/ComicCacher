import { Component, OnInit, Input } from '@angular/core';
import { Comic } from '../comic';

@Component({
  selector: 'app-comic-strip',
  templateUrl: './comic-strip.component.html',
  styleUrls: ['./comic-strip.component.css']
})
export class ComicStripComponent implements OnInit {
  @Input() comic: Comic;

  constructor() { }

  ngOnInit() {
  }

}
