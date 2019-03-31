import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, map, tap } from 'rxjs/operators';

import { Observable, of } from 'rxjs';

import { Comic } from './dto/comic';
import { ImageDto } from './dto/image';
import { MessageService } from './message.service';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({ providedIn: 'root' })
export class ComicService {

  

  constructor(
      private http: HttpClient,
      private messageService: MessageService) { }
 
  getComics(): Observable<Comic[]> {
    return this.http.get<Comic[]>("api/v1/comics")
        .pipe(
          tap(el => this.log(`fetched ${el.length} comics`)),
          catchError(this.handleError('getComics', []))
        );
  }

   /** GET comic by id. Will 404 if id not found */
  getComic(id: number): Observable<Comic> {
    if (id == 0)
      id = 14293307;
    const url = `api/v1/comic/${id}`;
    return this.http.get<Comic>(url).pipe(
      tap(el => this.log(`fetched Comic id=${id}, ${el.name}`)),      
      catchError(this.handleError<Comic>(`getComic id=${id}`))
    );
  }  

  getLatest(id : number) : Observable<ImageDto>
  {
    if (id == 0)
      return null;      
    const url = `api/v1/comic/${id}/strips/last`;
    return this.http.get<ImageDto>(url).pipe(
      tap(el => this.log(`fetched latest strip for comic id=${id}`)),      
      catchError(this.handleError<ImageDto>(`getComic id=${id}`))
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