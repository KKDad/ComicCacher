import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, map, tap } from 'rxjs/operators';

import { Observable, of } from 'rxjs';

import { Comic } from './comic';
import { MessageService } from './message.service';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({ providedIn: 'root' })
export class ComicService {

  private comicUrl = "api/v1/comics";  // URL to web api

  constructor(
      private http: HttpClient,
      private messageService: MessageService) { }
 
  getComics(): Observable<Comic[]> {
    return this.http.get<Comic[]>(this.comicUrl)
        .pipe(
          tap(el => this.log(`fetched ${el.length} comics`)),
          catchError(this.handleError('getComics', []))
        );
  }

   /** GET comic by id. Will 404 if id not found */
  getComic(id: number): Observable<Comic> {
    if (id == 0)
      id = 14293307;
    const url = `${this.comicUrl}/${id}`;
    return this.http.get<Comic>(url).pipe(
      tap(el => this.log(`fetched Comic id=${id}, ${el.name}`)),      
      catchError(this.handleError<Comic>(`getComic id=${id}`))
    );
  }  


  /** GET Comic by id. Return `undefined` when id not found */
  getComicNo404<Data>(id: number): Observable<Comic> {
    const url = `${this.comicUrl}/?id=${id}`;
    return this.http.get<Comic[]>(url)
      .pipe(
        map(Comic => Comic[0]), // returns a {0|1} element array
        tap(h => {
          const outcome = h ? `fetched` : `did not find`;
          this.log(`${outcome} Comic id=${id}`);
        }),
        catchError(this.handleError<Comic>(`getComic id=${id}`))
      );
  }  

  /* GET comics whose name contains search term */
  searchComics(term: string): Observable<Comic[]> {
    if (!term.trim()) {
      // if not search term, return empty comic array.
      return of([]);
    }
    return this.http.get<Comic[]>(`${this.comicUrl}/?name=${term}`).pipe(
      tap(_ => this.log(`found comics matching "${term}"`)),
      catchError(this.handleError<Comic[]>('searchComics', []))
    );
  }  


  /** Log a message with the MessageService */
  private log(message: string) {
    this.messageService.add(`ComicService: ${message}`);
  }

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private handleError<T> (operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
  
      // TODO: send the error to remote logging infrastructure
      console.error(error); // log to console instead
  
      // TODO: better job of transforming error for user consumption
      this.log(`${operation} failed: ${error.message}`);
  
      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }  

}