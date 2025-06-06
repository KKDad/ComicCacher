import { Injectable } from '@angular/core';
import { Observable, fromEvent, filter } from 'rxjs';
import { map } from 'rxjs/operators';

export type KeyCombo = {
  key: string;
  alt?: boolean;
  ctrl?: boolean;
  shift?: boolean;
};

/**
 * Service for handling keyboard navigation and shortcuts
 * 
 * This service provides utilities for components to respond to
 * keyboard events and implement proper keyboard navigation.
 */
@Injectable({
  providedIn: 'root'
})
export class KeyboardService {
  // Stream of all keyboard events
  private keyDown$: Observable<KeyboardEvent> = fromEvent<KeyboardEvent>(document, 'keydown');
  
  constructor() { }
  
  /**
   * Returns an observable that emits when a specific key combo is pressed
   * @param combo The key combination to listen for
   * @returns An observable of keyboard events matching the combo
   */
  listenForKeyCombo(combo: KeyCombo): Observable<KeyboardEvent> {
    return this.keyDown$.pipe(
      filter(event => {
        // Match the specified key
        const keyMatch = event.key.toLowerCase() === combo.key.toLowerCase();
        
        // Match modifier keys if specified
        const altMatch = combo.alt === undefined || event.altKey === combo.alt;
        const ctrlMatch = combo.ctrl === undefined || event.ctrlKey === combo.ctrl;
        const shiftMatch = combo.shift === undefined || event.shiftKey === combo.shift;
        
        return keyMatch && altMatch && ctrlMatch && shiftMatch;
      })
    );
  }
  
  /**
   * Returns an observable for arrow key navigation
   * @returns Observable that emits the arrow key direction
   */
  arrowKeys(): Observable<'up' | 'down' | 'left' | 'right'> {
    return this.keyDown$.pipe(
      filter(event => [
        'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'
      ].includes(event.key)),
      map(event => {
        switch (event.key) {
          case 'ArrowUp': return 'up';
          case 'ArrowDown': return 'down';
          case 'ArrowLeft': return 'left';
          case 'ArrowRight': return 'right';
          default: return null;
        }
      })
    );
  }
  
  /**
   * Listen for escape key presses
   * @returns Observable that emits when escape is pressed
   */
  escapeKey(): Observable<KeyboardEvent> {
    return this.listenForKeyCombo({ key: 'Escape' });
  }
  
  /**
   * Listen for enter key presses
   * @returns Observable that emits when enter is pressed
   */
  enterKey(): Observable<KeyboardEvent> {
    return this.listenForKeyCombo({ key: 'Enter' });
  }
  
  /**
   * Listen for space key presses
   * @returns Observable that emits when space is pressed
   */
  spaceKey(): Observable<KeyboardEvent> {
    return this.listenForKeyCombo({ key: ' ' });
  }
  
  /**
   * Register global keyboard shortcuts for navigating comics
   * @param onFirst Function to call when first is requested
   * @param onPrev Function to call when previous is requested
   * @param onNext Function to call when next is requested
   * @param onLast Function to call when last is requested
   */
  registerComicNavigationShortcuts(
    onFirst: () => void,
    onPrev: () => void,
    onNext: () => void,
    onLast: () => void
  ): void {
    // First comic: Home key
    this.listenForKeyCombo({ key: 'Home' }).subscribe(() => onFirst());
    
    // Previous comic: Left arrow or PageUp
    this.listenForKeyCombo({ key: 'ArrowLeft' }).subscribe(() => onPrev());
    this.listenForKeyCombo({ key: 'PageUp' }).subscribe(() => onPrev());
    
    // Next comic: Right arrow or PageDown
    this.listenForKeyCombo({ key: 'ArrowRight' }).subscribe(() => onNext());
    this.listenForKeyCombo({ key: 'PageDown' }).subscribe(() => onNext());
    
    // Last comic: End key
    this.listenForKeyCombo({ key: 'End' }).subscribe(() => onLast());
  }
}