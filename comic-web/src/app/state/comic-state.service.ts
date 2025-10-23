import {computed, inject, Injectable, signal} from '@angular/core';
import {Comic} from '../dto/comic';
import {ImageDto} from '../dto/image';
import {ComicService} from '../comic.service';
import {toObservable} from '@angular/core/rxjs-interop';
import {catchError, EMPTY, shareReplay, switchMap} from 'rxjs';

export interface ComicState {
  comics: Comic[];
  selectedComicId: number | null;
  currentStrip: ImageDto | null;
  loading: boolean;
  error: string | null;
}

const initialState: ComicState = {
  comics: [],
  selectedComicId: null,
  currentStrip: null,
  loading: false,
  error: null
};

@Injectable({
  providedIn: 'root'
})
export class ComicStateService {
  private comicService = inject(ComicService);
  
  // STATE SIGNALS
  private state = signal<ComicState>(initialState);
  
  // PUBLIC SELECTORS
  readonly comics = computed(() => this.state().comics);
  readonly selectedComicId = computed(() => this.state().selectedComicId);
  readonly currentStrip = computed(() => this.state().currentStrip);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);
  
  // DERIVED SELECTORS
  readonly selectedComic = computed(() => {
    const id = this.selectedComicId();
    if (!id) return null;
    return this.comics().find(comic => comic.id === id) || null;
  });
  
  // OBSERVABLES
  readonly comics$ = toObservable(this.comics);
  readonly selectedComic$ = toObservable(this.selectedComic);
  readonly currentStrip$ = toObservable(this.currentStrip);
  
  constructor() {
    // Initialize comics from the service
    this.loadComics();
    
    // Keep track of the selected comic and load its latest strip
    toObservable(this.selectedComicId).pipe(
      switchMap(id => {
        if (!id) return EMPTY;
        return this.comicService.getLatest(id);
      }),
      catchError(error => {
        this.setError(`Error loading comic strip: ${error.message}`);
        return EMPTY;
      }),
      shareReplay(1)
    ).subscribe(strip => {
      if (strip) {
        this.updateCurrentStrip(strip);
      }
    });
  }
  
  // ACTION METHODS
  
  /**
   * Load all comics
   */
  loadComics(): void {
    this.setLoading(true);
    this.setError(null);
    
    this.comicService.getComics().subscribe({
      next: (comics) => {
        this.updateComics(comics);
        this.setLoading(false);
      },
      error: (err) => {
        this.setError(`Error loading comics: ${err.message}`);
        this.setLoading(false);
      }
    });
  }
  
  /**
   * Select a comic and load its latest strip
   */
  selectComic(id: number): void {
    this.setSelectedComicId(id);
  }
  
  /**
   * Navigate to the first strip
   */
  navigateToFirst(): void {
    const id = this.selectedComicId();
    if (!id) return;
    
    this.setLoading(true);
    this.comicService.getEarliest(id).subscribe({
      next: (strip) => {
        this.updateCurrentStrip(strip);
        this.setLoading(false);
      },
      error: (err) => {
        this.setError(`Error loading first strip: ${err.message}`);
        this.setLoading(false);
      }
    });
  }
  
  /**
   * Navigate to the previous strip
   */
  navigateToPrevious(): void {
    const id = this.selectedComicId();
    const current = this.currentStrip();
    if (!id || !current || !current.imageDate) return;
    
    this.setLoading(true);
    this.comicService.getPrev(id, current.imageDate).subscribe({
      next: (strip) => {
        this.updateCurrentStrip(strip);
        this.setLoading(false);
      },
      error: (err) => {
        this.setError(`Error loading previous strip: ${err.message}`);
        this.setLoading(false);
      }
    });
  }
  
  /**
   * Navigate to the next strip
   */
  navigateToNext(): void {
    const id = this.selectedComicId();
    const current = this.currentStrip();
    if (!id || !current || !current.imageDate) return;
    
    this.setLoading(true);
    this.comicService.getNext(id, current.imageDate).subscribe({
      next: (strip) => {
        this.updateCurrentStrip(strip);
        this.setLoading(false);
      },
      error: (err) => {
        this.setError(`Error loading next strip: ${err.message}`);
        this.setLoading(false);
      }
    });
  }
  
  /**
   * Navigate to the latest strip
   */
  navigateToLatest(): void {
    const id = this.selectedComicId();
    if (!id) return;
    
    this.setLoading(true);
    this.comicService.getLatest(id).subscribe({
      next: (strip) => {
        this.updateCurrentStrip(strip);
        this.setLoading(false);
      },
      error: (err) => {
        this.setError(`Error loading latest strip: ${err.message}`);
        this.setLoading(false);
      }
    });
  }
  
  // PRIVATE STATE UPDATE METHODS
  
  private updateComics(comics: Comic[]): void {
    this.state.update(state => ({
      ...state,
      comics
    }));
  }
  
  private setSelectedComicId(id: number | null): void {
    this.state.update(state => ({
      ...state,
      selectedComicId: id
    }));
  }
  
  private updateCurrentStrip(strip: ImageDto | null): void {
    this.state.update(state => ({
      ...state,
      currentStrip: strip
    }));
  }
  
  private setLoading(loading: boolean): void {
    this.state.update(state => ({
      ...state,
      loading
    }));
  }
  
  private setError(error: string | null): void {
    this.state.update(state => ({
      ...state,
      error
    }));
  }
}