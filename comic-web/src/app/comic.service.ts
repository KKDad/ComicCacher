import {inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, EMPTY, Observable, of, shareReplay, tap} from 'rxjs';
import {toObservable} from '@angular/core/rxjs-interop';
import {Comic} from './dto/comic';
import {ImageDto} from './dto/image';

@Injectable({ providedIn: 'root' })
export class ComicService {
  private http = inject(HttpClient);

  // Signals for state management
  private comicsSignal = signal<Comic[]>([]);

  // Observable from signal
  comics$ = toObservable(this.comicsSignal);

  constructor() {
    // Initialize comics on service creation
    this.refresh();
  }

  /**
   * Get all comics from API
   */
  getComics(): Observable<Comic[]> {
    return this.comics$;
  }

  /**
   * Refresh comics data from API
   */
  refresh(): void {
    this.http.get<Comic[]>('api/v1/comics').pipe(
      // Handle errors globally
      catchError(error => {
        console.error('Error fetching comics', error);
        return of([]);
      }),
      // Share the same response with multiple subscribers
      shareReplay(1)
    ).subscribe(comicData => {
      this.comicsSignal.set(comicData);
    });
  }

  /**
   * Get comic by id
   */
  getComic(id: number): Observable<Comic> {
    if (id === 0) {
      id = 14293307; // Default ID
    }

    const url = `api/v1/comics/${id}`;
    return this.http.get<Comic>(url).pipe(
      catchError(this.handleError<Comic>(`getComic id=${id}`))
    );
  }

  /**
   * Get earliest comic strip for a given comic
   */
  getEarliest(id: number): Observable<ImageDto> {
    if (id === 0) {
      return EMPTY;
    }

    const url = `api/v1/comics/${id}/strips/first`;
    return this.http.get<ImageDto>(url).pipe(
      catchError(this.handleError<ImageDto>(`getEarliest id=${id}`))
    );
  }

  /**
   * Get latest comic strip for a given comic
   */
  getLatest(id: number): Observable<ImageDto> {
    if (id === 0) {
      return EMPTY;
    }

    const url = `api/v1/comics/${id}/strips/last`;
    return this.http.get<ImageDto>(url).pipe(
      catchError(this.handleError<ImageDto>(`getLatest id=${id}`))
    );
  }

  /**
   * Get next comic strip for a given comic and current date
   */
  getNext(id: number, current: String): Observable<ImageDto> {
    if (id === 0) {
      return EMPTY;
    }

    const url = `api/v1/comics/${id}/next/${current}`;
    return this.http.get<ImageDto>(url).pipe(
      catchError(this.handleError<ImageDto>(`getNext id=${id}`))
    );
  }

  /**
   * Get previous comic strip for a given comic and current date
   */
  getPrev(id: number, current: String): Observable<ImageDto> {
    if (id === 0) {
      return EMPTY;
    }

    const url = `api/v1/comics/${id}/previous/${current}`;
    return this.http.get<ImageDto>(url).pipe(
      catchError(this.handleError<ImageDto>(`getPrev id=${id}`))
    );
  }

  /**
   * Get avatar image for a given comic
   */
  getAvatar(id: number): Observable<ImageDto> {
    if (id === 0) {
      return EMPTY;
    }

    const url = `api/v1/comics/${id}/avatar`;
    return this.http.get<ImageDto>(url).pipe(
      catchError(this.handleError<ImageDto>(`getAvatar id=${id}`))
    );
  }

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error.message}`);
      return of(result as T);
    };
  }
}
