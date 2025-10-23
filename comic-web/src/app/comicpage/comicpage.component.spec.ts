import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ComicpageComponent} from './comicpage.component';
import {expectExists} from '../testing/testing-utils';
import {NavBarOption} from './container/container.component';
import {CommonModule} from '@angular/common';
import {ComicService} from '../comic.service';
import {of} from 'rxjs';
import {Component, EventEmitter, Injectable, Input, Output} from '@angular/core';

@Injectable()
class MockComicService {
  getComics() {
    return of([
      { id: 1, name: 'Comic 1' },
      { id: 2, name: 'Comic 2' }
    ]);
  }
  
  refresh() {
    // No-op for testing
  }
}

// Mock ContainerComponent
@Component({
  // Use the same selector as the actual component
  selector: 'container',
  template: '<div>Mock Container</div>',
  standalone: true
})
class MockContainerComponent {
  @Input() sections = null;
  @Output() scrollinfo = new EventEmitter<NavBarOption>();
}

describe('ComicpageComponent', () => {
  let component: ComicpageComponent;
  let fixture: ComponentFixture<ComicpageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ComicpageComponent,
        CommonModule,
        MockContainerComponent
      ],
      providers: [
        { provide: ComicService, useClass: MockComicService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ComicpageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should contain a container component', () => {
    expectExists(fixture, 'container', 'Container component should be displayed');
  });

  it('should update showNavbar when handleNavbarEvent is called with Hide', () => {
    // Set initial value
    component.showNavbar = true;
    
    // Call the method with Hide
    component.handleNavbarEvent(NavBarOption.Hide);
    
    // Check that showNavbar was updated
    expect(component.showNavbar).toBeFalse();
  });

  it('should update showNavbar when handleNavbarEvent is called with Show', () => {
    // Set initial value
    component.showNavbar = false;
    
    // Call the method with Show
    component.handleNavbarEvent(NavBarOption.Show);
    
    // Check that showNavbar was updated
    expect(component.showNavbar).toBeTrue();
  });
});