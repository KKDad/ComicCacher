import { ComponentFixture } from '@angular/core/testing';
import { ContainerComponent, NavBarOption } from './container.component';
import { ScrollDispatcher, CdkScrollable, CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { ComicService } from '../../comic.service';
import {
  createStandaloneComponentFixture,
  expectExists,
  findEl
} from '../../testing/testing-utils';
import { of, throwError, Subject } from 'rxjs';
import { Comic } from '../../dto/comic';
import { Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { 
  SectionStubComponent, 
  LoadingIndicatorStubComponent, 
  ErrorDisplayStubComponent,
  VirtualScrollViewportStubComponent
} from '../../testing/stub-components';
import { CommonModule } from '@angular/common';

// Mock CdkScrollable class
class MockCdkScrollable {
  scrollTop = 0;
  
  getElementRef() {
    return {
      nativeElement: {
        scrollTop: this.scrollTop
      }
    };
  }
}

describe('ContainerComponent', () => {
  let component: ContainerComponent;
  let fixture: ComponentFixture<ContainerComponent>;
  let comicServiceSpy: jasmine.SpyObj<ComicService>;
  let scrollDispatcherSpy: jasmine.SpyObj<ScrollDispatcher>;
  let scrollSubject: Subject<CdkScrollable>;
  let mockScrollable: MockCdkScrollable;

  const mockComics: Comic[] = [
    { id: 1, name: 'Comic 1', strip: 'strip1', avatar: 'avatar1', author: 'Author 1', oldest: '2020-01-01', newest: '2023-01-01', description: 'Description 1' },
    { id: 2, name: 'Comic 2', strip: 'strip2', avatar: 'avatar2', author: 'Author 2', oldest: '2020-01-01', newest: '2023-01-01', description: 'Description 2' }
  ];

  beforeEach(() => {
    comicServiceSpy = jasmine.createSpyObj('ComicService', ['getComics', 'refresh']);
    scrollDispatcherSpy = jasmine.createSpyObj('ScrollDispatcher', ['scrolled']);
    
    // Create a subject to emit scroll events
    scrollSubject = new Subject<CdkScrollable>();
    mockScrollable = new MockCdkScrollable();
    
    // Setup the getComics spy to return mock comics
    comicServiceSpy.getComics.and.returnValue(of(mockComics));

    // Setup the scrolled spy to return our subject
    scrollDispatcherSpy.scrolled.and.returnValue(scrollSubject.asObservable());

    fixture = createStandaloneComponentFixture(
      ContainerComponent,
      [
        NoopAnimationsModule, // Add NoopAnimationsModule for animation testing
        CommonModule,
        SectionStubComponent,
        LoadingIndicatorStubComponent,
        ErrorDisplayStubComponent,
        VirtualScrollViewportStubComponent
      ],
      [
        { provide: ComicService, useValue: comicServiceSpy },
        { provide: ScrollDispatcher, useValue: scrollDispatcherSpy },
        // Mock the CdkVirtualScrollViewport to prevent scrolling registration errors
        { provide: CdkVirtualScrollViewport, useClass: VirtualScrollViewportStubComponent }
      ],
      // Use schema to ignore unknown elements
      { schemas: [CUSTOM_ELEMENTS_SCHEMA] }
    );

    component = fixture.componentInstance;
    
    // Spy on scrollinfo EventEmitter
    spyOn(component.scrollinfo, 'emit');
    
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load comics on init', () => {
    expect(comicServiceSpy.getComics).toHaveBeenCalled();
    expect(component.sections).toEqual(mockComics);
    expect(component.loading()).toBeFalse();
  });

  it('should handle errors when loading comics', () => {
    // Reset the component
    fixture = createStandaloneComponentFixture(
      ContainerComponent,
      [
        NoopAnimationsModule,
        CommonModule,
        SectionStubComponent,
        LoadingIndicatorStubComponent,
        ErrorDisplayStubComponent,
        VirtualScrollViewportStubComponent
      ],
      [
        { provide: ComicService, useValue: jasmine.createSpyObj('ComicService', {
          getComics: throwError(() => new Error('Test error')),
          refresh: null
        }) },
        { provide: ScrollDispatcher, useValue: scrollDispatcherSpy },
        { provide: CdkVirtualScrollViewport, useClass: VirtualScrollViewportStubComponent }
      ],
      { schemas: [CUSTOM_ELEMENTS_SCHEMA] }
    );

    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.error()).toContain('Failed to load comics');
    expect(component.loading()).toBeFalse();
    expectExists(fixture, 'error-display', 'Error display should be visible');
  });

  it('should show loading indicator when loading', () => {
    component.loading.set(true);
    fixture.detectChanges();
    
    expectExists(fixture, 'loading-indicator', 'Loading indicator should be visible');
  });

  it('should refresh comics', () => {
    component.refreshComics();
    expect(comicServiceSpy.refresh).toHaveBeenCalled();
    expect(component.loading()).toBeTrue();

    // Fast-forward the setTimeout
    jasmine.clock().install();
    jasmine.clock().tick(1000);

    expect(component.loading()).toBeFalse();
    jasmine.clock().uninstall();
  });

  it('should clear error when requested', () => {
    component.error.set('Test error');
    component.clearError();
    expect(component.error()).toBeNull();
  });

  it('should emit NavBarOption.Hide when scrolling down more than lastOffset', () => {
    // Set initial lastOffset
    component.lastOffset = 50;
    
    // Simulate scrolling down to 120
    mockScrollable.scrollTop = 120;
    scrollSubject.next(mockScrollable as unknown as CdkScrollable);
    
    // Should emit NavBarOption.Hide
    expect(component.scrollinfo.emit).toHaveBeenCalledWith(NavBarOption.Hide);
    
    // lastOffset should be updated
    expect(component.lastOffset).toBe(120);
  });
  
  it('should emit NavBarOption.Hide when near the top of page', () => {
    // Simulate scrolling to top
    mockScrollable.scrollTop = 5;
    scrollSubject.next(mockScrollable as unknown as CdkScrollable);
    
    // Should emit NavBarOption.Hide
    expect(component.scrollinfo.emit).toHaveBeenCalledWith(NavBarOption.Hide);
    
    // lastOffset should be updated
    expect(component.lastOffset).toBe(5);
  });

  it('should return proper item size for virtualization', () => {
    expect(component.itemSizeFn(0)).toBe(550);
  });

  it('should track comics by ID', () => {
    const comic: Comic = { id: 5, name: 'Test', strip: 'test', avatar: 'test', author: 'Author', oldest: '2020-01-01', newest: '2023-01-01', description: 'Description' };
    expect(component.trackByComicId(10, comic)).toBe(5);
  });

  it('should return index for tracking when comic has no ID', () => {
    const comic = { name: 'Test', strip: 'test', avatar: 'test', author: 'Author', oldest: '2020-01-01', newest: '2023-01-01', description: 'Description' } as Comic;
    expect(component.trackByComicId(10, comic)).toBe(10);
  });
});