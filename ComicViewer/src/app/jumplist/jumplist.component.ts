import { Component, OnInit, Input } from '@angular/core';

import { Comic } from '../dto/comic';
import { ComicService } from '../comic.service';


@Component({
  selector: 'jumplist',
  templateUrl: 'jumplist.component.html',
  styleUrls: ['jumplist.component.css']
})
export class JumplistComponent implements OnInit 
{
  jumptargets: Comic[];

  constructor(private comicService: ComicService) { }


  ngOnInit() 
  {
    this.comicService.getComics().subscribe(c => this.jumptargets = c);    
  }


}
