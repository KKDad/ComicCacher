import {ComponentFixture} from '@angular/core/testing';
import {SectionComponent} from './section.component';
import {ComicService} from '../../comic.service';
import {ComicStateService} from '../../state/comic-state.service';
import {KeyboardService} from '../../shared/a11y/keyboard-service';
import {DomSanitizer} from '@angular/platform-browser';
import {of, throwError} from 'rxjs';
import {createStandaloneComponentFixture} from '../../testing/testing-utils';
import {ElementRef} from '@angular/core';
import {Comic} from '../../dto/comic';
import {ImageDto} from '../../dto/image';

describe('SectionComponent', () => {
  let component: SectionComponent;
  let fixture: ComponentFixture<SectionComponent>;
  let comicServiceSpy: jasmine.SpyObj<ComicService>;
  let stateServiceSpy: jasmine.SpyObj<ComicStateService>;
  let keyboardServiceSpy: jasmine.SpyObj<KeyboardService>;
  let sanitizerSpy: jasmine.SpyObj<DomSanitizer>;

  const mockComic: Comic = {
    id: 1,
    name: 'Test Comic',
    strip: 'test-strip-url',
    avatar: 'test-avatar-url',
    author: 'Test Author',
    oldest: '2020-01-01',
    newest: '2023-01-01',
    description: 'Test Description'
  };

  const mockImageDto: ImageDto = {
    imageData: 'base64data',
    mimeType: 'image/png',
    width: 800,
    height: 600,
    imageDate: '2023-05-09'
  };

  beforeEach(() => {
    // Create spies for all services
    comicServiceSpy = jasmine.createSpyObj('ComicService', [
      'getAvatar',
      'getEarliest',
      'getPrev',
      'getNext',
      'getLatest'
    ]);

    stateServiceSpy = jasmine.createSpyObj('ComicStateService', ['updateCurrentStrip']);
    keyboardServiceSpy = jasmine.createSpyObj('KeyboardService', ['registerComicStripNavigationShortcuts']);
    sanitizerSpy = jasmine.createSpyObj('DomSanitizer', ['bypassSecurityTrustResourceUrl']);

    // Set up spy return values
    comicServiceSpy.getAvatar.and.returnValue(of(mockImageDto));
    comicServiceSpy.getEarliest.and.returnValue(of(mockImageDto));
    comicServiceSpy.getPrev.and.returnValue(of(mockImageDto));
    comicServiceSpy.getNext.and.returnValue(of(mockImageDto));
    comicServiceSpy.getLatest.and.returnValue(of(mockImageDto));

    sanitizerSpy.bypassSecurityTrustResourceUrl.and.returnValue('safe-url');
    // No return value needed for this method, it returns void

    // Create the mock ElementRef
    const mockElementRef = {
      nativeElement: {
        addEventListener: jasmine.createSpy('addEventListener')
      }
    };

    fixture = createStandaloneComponentFixture(
      SectionComponent,
      [], // Component already has imports
      [
        { provide: ComicService, useValue: comicServiceSpy },
        { provide: ComicStateService, useValue: stateServiceSpy },
        { provide: KeyboardService, useValue: keyboardServiceSpy },
        { provide: DomSanitizer, useValue: sanitizerSpy },
        { provide: ElementRef, useValue: mockElementRef }
      ]
    );

    component = fixture.componentInstance;
    component.content = mockComic;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load avatar when visible (lazy loading)', (done) => {
    // Initially, avatar should NOT be loaded
    expect(comicServiceSpy.getAvatar).not.toHaveBeenCalled();

    // Manually trigger the lazy loading (simulating IntersectionObserver)
    // Access private method via any type assertion
    (component as any).loadImagesLazily();

    // Now avatar should be loaded
    setTimeout(() => {
      expect(comicServiceSpy.getAvatar).toHaveBeenCalledWith(mockComic.id);
      done();
    }, 10);
  });

  it('should load latest comic when visible (lazy loading)', (done) => {
    // Initially, latest comic should NOT be loaded
    expect(comicServiceSpy.getLatest).not.toHaveBeenCalled();

    // Manually trigger the lazy loading (simulating IntersectionObserver)
    (component as any).loadImagesLazily();

    // Now latest comic should be loaded
    setTimeout(() => {
      expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(mockComic.id);
      done();
    }, 10);
  });

  it('should sanitize avatar image URL', () => {
    component.getAvatarImage();
    expect(sanitizerSpy.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(mockComic.avatar);
  });

  it('should sanitize comic image URL', () => {
    component.getComicImage();
    expect(sanitizerSpy.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(mockComic.strip);
  });

  it('should clear error', () => {
    component.error.set('test error');
    component.clearError();
    expect(component.error()).toBeNull();
  });

  it('should handle navigation to first comic', () => {
    component.onNavigateFirst();
    expect(comicServiceSpy.getEarliest).toHaveBeenCalledWith(mockComic.id);
    expect(component.loading()).toBeFalse();
  });

  it('should handle navigation to previous comic', () => {
    component.imageDate = '2023-05-09';
    component.onPrev();
    expect(comicServiceSpy.getPrev).toHaveBeenCalledWith(mockComic.id, '2023-05-09');
    expect(component.loading()).toBeFalse();
  });

  it('should handle navigation to next comic', () => {
    component.imageDate = '2023-05-09';
    component.onNext();
    expect(comicServiceSpy.getNext).toHaveBeenCalledWith(mockComic.id, '2023-05-09');
    expect(component.loading()).toBeFalse();
  });

  it('should handle navigation to latest comic', () => {
    component.onNavigateLast();
    expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(mockComic.id);
    expect(component.loading()).toBeFalse();
  });

  it('should handle errors when loading comics', () => {
    // Reset the getLatest spy to throw an error
    comicServiceSpy.getLatest.and.returnValue(throwError(() => new Error('Test error')));

    component.onNavigateLast();
    expect(component.error()).toContain('Could not load latest comic');
    expect(component.loading()).toBeFalse();
  });

  it('should set strip for large comics (CSS handles responsive sizing)', () => {
    const largeImage: ImageDto = {
      imageData: 'base64data',
      mimeType: 'image/png',
      width: 1200,
      height: 900,
      imageDate: '2023-05-09'
    };

    comicServiceSpy.getLatest.and.returnValue(of(largeImage));
    component.onNavigateLast();

    // Verify strip data URL is set correctly (CSS handles sizing)
    expect(component.content.strip).toContain('data:image/png;base64,');
    expect(component.imageDate).toBe('2023-05-09');
  });

  it('should set strip for smaller comics (CSS handles responsive sizing)', () => {
    const smallImage: ImageDto = {
      imageData: 'base64data',
      mimeType: 'image/png',
      width: 800,
      height: 600,
      imageDate: '2023-05-09'
    };

    comicServiceSpy.getLatest.and.returnValue(of(smallImage));
    component.onNavigateLast();

    // Verify strip data URL is set correctly (CSS handles sizing)
    expect(component.content.strip).toContain('data:image/png;base64,');
    expect(component.imageDate).toBe('2023-05-09');
  });
});
