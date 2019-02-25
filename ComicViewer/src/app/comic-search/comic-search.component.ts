import { Component, OnInit } from '@angular/core';

import { Observable, Subject } from 'rxjs';

import {
   debounceTime, distinctUntilChanged, switchMap
 } from 'rxjs/operators';

import { Comic } from '../comic';
import { ComicService } from '../comic.service';

@Component({
  selector: 'app-comic-search',
  templateUrl: './comic-search.component.html',
  styleUrls: [ './comic-search.component.css' ]
})
export class ComicSearchComponent implements OnInit {
  comics$: Observable<Comic[]>;
  private searchTerms = new Subject<string>();

  constructor(private comicService: ComicService) {}

  // Push a search term into the observable stream.
  search(term: string): void {
    this.searchTerms.next(term);
  }

  ngOnInit(): void {
    this.comics$ = this.searchTerms.pipe(
      // wait 300ms after each keystroke before considering the term
      debounceTime(300),

      // ignore new term if same as previous term
      distinctUntilChanged(),

      // switch to new search observable each time the term changes
      switchMap((term: string) => this.comicService.searchComics(term)),
    );
  }
}