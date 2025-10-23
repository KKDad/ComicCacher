import {ComponentFixture} from '@angular/core/testing';
import {AppComponent} from './app.component';
import {ContainerComponent, NavBarOption} from './comicpage/container/container.component';
import {RouterModule} from '@angular/router';
import {EventEmitter} from '@angular/core';
import {createStandaloneComponentFixture, expectExists, getText} from './testing/testing-utils';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let containerComponentMock: jasmine.SpyObj<ContainerComponent> & { scrollinfo: EventEmitter<NavBarOption> };

  beforeEach(() => {
    // Create a mock for the ContainerComponent with EventEmitter
    const scrollInfoEmitter = new EventEmitter<NavBarOption>();
    containerComponentMock = jasmine.createSpyObj('ContainerComponent', [], { scrollinfo: scrollInfoEmitter }) as any;

    // Create the fixture
    fixture = createStandaloneComponentFixture(
      AppComponent,
      [RouterModule.forRoot([])], // RouterModule is needed for routerLink
      [{ provide: ContainerComponent, useValue: containerComponentMock }]
    );

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('should have the correct title', () => {
    expect(component.title).toEqual('The Comic Reader');
  });

  it('should render title in a h1 tag', () => {
    const titleText = getText(fixture, 'h1.title');
    expect(titleText).toContain('Daily Comics');
  });

  it('should include navigation links', () => {
    expectExists(fixture, 'a[href="https://github.com/KKDad/ComicCacher"]', 'Project link should exist');
    expectExists(fixture, 'a[href="docs/index.html"]', 'API link should exist');
    // Check for About button by finding button with routerLink directive in .nav-side wrapper
    const aboutButton = fixture.nativeElement.querySelector('.nav-side button');
    expect(aboutButton).withContext('About button should exist').toBeTruthy();
  });

  it('should include router outlet', () => {
    expectExists(fixture, 'router-outlet', 'Router outlet should exist');
  });

  it('should collapse navbar when NavBarOption.Hide is received', () => {
    // Call onWindowScroll directly
    component.onWindowScroll(NavBarOption.Hide);
    fixture.detectChanges();

    // Check that the navbar is collapsed
    expect(component.isNavCollapsed()).toBeTrue();
    expect(fixture.nativeElement.querySelector('.topnav.collapsed')).toBeTruthy();
  });

  it('should show navbar when NavBarOption.Show is received', () => {
    // First collapse the navbar
    component.isNavCollapsed.set(true);
    expect(component.isNavCollapsed()).toBeTrue();

    // Call onWindowScroll directly to show the navbar
    component.onWindowScroll(NavBarOption.Show);

    // Check that the signal was updated to show the navbar
    expect(component.isNavCollapsed()).toBeFalse();
  });

  it('should handle window scroll events', () => {
    // Mock the window scroll
    const scrollEvent = new Event('scroll');

    // Test scrolling down (should collapse)
    Object.defineProperty(window, 'pageYOffset', { value: 150, configurable: true });
    window.dispatchEvent(scrollEvent);
    expect(component.isNavCollapsed()).toBeTrue();

    // Test scrolling to top (should show)
    Object.defineProperty(window, 'pageYOffset', { value: 5, configurable: true });
    window.dispatchEvent(scrollEvent);
    expect(component.isNavCollapsed()).toBeFalse();
  });
});
