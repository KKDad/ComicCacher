import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ComponentFixture } from '@angular/core/testing';
import { AboutComponent } from './about.component';
import { Router } from '@angular/router';
import { click, createStandaloneComponentFixture, expectExists, getText } from '../testing/testing-utils'
import type { SpyObj } from '../testing/testing-utils';

describe('AboutComponent', () => {
    let component: AboutComponent;
    let fixture: ComponentFixture<AboutComponent>;
    let routerSpy: SpyObj<Router, 'navigateByUrl'>;

    beforeEach(() => {
        routerSpy = {
            navigateByUrl: vi.fn().mockName("Router.navigateByUrl")
        };

        fixture = createStandaloneComponentFixture(AboutComponent, [], // No additional imports needed, they're already in the component
            [{ provide: Router, useValue: routerSpy }]);

        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should display information about the application', () => {
        expectExists(fixture, 'mat-card', 'Card should be displayed');
        expect(getText(fixture, '.description-section p:first-child'))
            .toContain('Daily Comics is a web comics cacher');

        // Tech stack is now in a definition list
        expect(getText(fixture, '.tech-list')).toContain('Angular 21');
        expect(getText(fixture, '.tech-list')).toContain('Spring Boot 4');
    });

    it('should navigate home when button is clicked', () => {
        click(fixture, 'button');
        expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/');
    });
});
