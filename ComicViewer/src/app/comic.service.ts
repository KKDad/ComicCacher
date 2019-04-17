import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, tap } from 'rxjs/operators';

import { Observable, of } from 'rxjs';

import { Comic } from './dto/comic';
import { ImageDto } from './dto/image';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({ providedIn: 'root' })
export class ComicService 
{  
  comics: Comic[];

  getComics(): Observable<Comic[]> 
  {
    const studentsObservable = new Observable(observer => { setTimeout(() => { observer.next(this.comics); }, 1000); return studentsObservable; });
    return studentsObservable;  
  }
  
  constructor(private http: HttpClient) { }
 
  refresh(): void {
    this.http.get<Comic[]>("api/v1/comics").toPromise().then(comicData => this.comics = comicData);
  }

   /** GET comic by id. Will 404 if id not found */
  getComic(id: number): Observable<Comic> {
    if (id == 0)
      id = 14293307;
    const url = `api/v1/comics/${id}`;
    return this.http.get<Comic>(url).pipe(
      tap(el => console.log(`fetched Comic id=${id}, ${el.name}`)),      
      catchError(this.handleError<Comic>(`getComic id=${id}`))
    );
  }  

  getEarliest(id : number) : Observable<ImageDto>
  {
    if (id == 0)
      return null;      
    const url = `api/v1/comics/${id}/strips/first`;
    return this.http.get<ImageDto>(url).pipe(
      tap(el => console.log(`fetched first strip for comic id=${id}`)),      
      catchError(this.handleError<ImageDto>(`getComic id=${id}`))
    );
  }  

  getLatest(id : number) : Observable<ImageDto>
  {
    if (id == 0)
      return null;      
    const url = `api/v1/comics/${id}/strips/last`;
    return this.http.get<ImageDto>(url).pipe(
      tap(el => console.log(`fetched latest strip for comic id=${id}`)),      
      catchError(this.handleError<ImageDto>(`getComic id=${id}`))
    );
  }

  getNext(id : number, current: String) : Observable<ImageDto>
  {
    if (id == 0)
      return null;      
    const url = `api/v1/comics/${id}/next/${current}`;
    return this.http.get<ImageDto>(url).pipe(
      tap(el => console.log(`fetched next strip for comic id=${id}`)),      
      catchError(this.handleError<ImageDto>(`getComic id=${id}`))
    );
  }  

  getPrev(id : number, current: String) : Observable<ImageDto>
  {
    if (id == 0)
      return null;      
    const url = `api/v1/comics/${id}/previous/${current}`;
    return this.http.get<ImageDto>(url).pipe(
      tap(el => console.log(`fetched next strip for comic id=${id}`)),      
      catchError(this.handleError<ImageDto>(`getComic id=${id}`))
    );
  }    

  getAvatar(id : number) : Observable<ImageDto>
  {
    if (id == 0)
      return null;      
    const url = `api/v1/comics/${id}/avatar`;
    return this.http.get<ImageDto>(url).pipe(
      tap(el => console.log(`fetched latest strip for comic id=${id}`)),      
      catchError(this.handleError<ImageDto>(`getComic id=${id}`))
    );
  }  

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private handleError<T> (operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
  
      console.error(error); // log to console instead
   
      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }  

}