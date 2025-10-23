import {ComponentFixture} from '@angular/core/testing';
import {AboutComponent} from './about.component';
import {Router} from '@angular/router';
import {click, createStandaloneComponentFixture, expectExists, getText} from '../testing/testing-utils';

describe('AboutComponent', () => {
  let component: AboutComponent;
  let fixture: ComponentFixture<AboutComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);

    fixture = createStandaloneComponentFixture(
      AboutComponent,
      [], // No additional imports needed, they're already in the component
      [{ provide: Router, useValue: routerSpy }]
    );

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display information about the application', () => {
    expectExists(fixture, 'mat-card', 'Card should be displayed');
    expect(getText(fixture, 'mat-card-content p:first-child'))
      .toContain('Daily comics is a web comics cacher');
    expect(getText(fixture, 'mat-card-content p:nth-child(3)')).toContain('Angular 19');
  });

  it('should navigate home when button is clicked', () => {
    click(fixture, 'button');
    expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/');
  });
});
