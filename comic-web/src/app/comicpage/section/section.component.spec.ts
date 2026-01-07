import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { SectionComponent } from './section.component';
import { ComicService } from '../../comic.service';
import { ComicStateService } from '../../state/comic-state.service';
import { KeyboardService } from '../../shared/a11y/keyboard-service';
import { DomSanitizer } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { createStandaloneComponentFixture } from '../../testing/testing-utils';
import { ElementRef } from '@angular/core';
import { Comic } from '../../dto/comic';
import { ImageDto } from '../../dto/image';
import { ComicNavigationResult } from '../../dto/comic-navigation-result';

describe('SectionComponent', () => {
    let component: SectionComponent;
    let fixture: ComponentFixture<SectionComponent>;
    let comicServiceSpy: any;  // originally Partial<ComicService>
    let stateServiceSpy: any;  // originally Partial<ComicStateService>
    let keyboardServiceSpy: any;  // originally Partial<KeyboardService>
    let sanitizerSpy: any;  // originally Partial<DomSanitizer>

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

    const mockNavigationResult: ComicNavigationResult = {
        found: true,
        image: mockImageDto,
        nearestPreviousDate: '2023-05-08',
        nearestNextDate: '2023-05-10',
        currentDate: '2023-05-09'
    };

    beforeEach(() => {
        // Create spies for all services
        comicServiceSpy = {
            getAvatar: vi.fn().mockName("ComicService.getAvatar"),
            getEarliest: vi.fn().mockName("ComicService.getEarliest"),
            getPrev: vi.fn().mockName("ComicService.getPrev"),
            getNext: vi.fn().mockName("ComicService.getNext"),
            getLatest: vi.fn().mockName("ComicService.getLatest")
        };

        stateServiceSpy = {
            updateCurrentStrip: vi.fn().mockName("ComicStateService.updateCurrentStrip")
        };
        keyboardServiceSpy = {
            registerComicStripNavigationShortcuts: vi.fn().mockName("KeyboardService.registerComicStripNavigationShortcuts")
        };
        sanitizerSpy = {
            bypassSecurityTrustResourceUrl: vi.fn().mockName("DomSanitizer.bypassSecurityTrustResourceUrl")
        };

        // Set up spy return values
        comicServiceSpy.getAvatar.mockReturnValue(of(mockImageDto));
        comicServiceSpy.getEarliest.mockReturnValue(of(mockNavigationResult));
        comicServiceSpy.getPrev.mockReturnValue(of(mockNavigationResult));
        comicServiceSpy.getNext.mockReturnValue(of(mockNavigationResult));
        comicServiceSpy.getLatest.mockReturnValue(of(mockNavigationResult));

        sanitizerSpy.bypassSecurityTrustResourceUrl.mockReturnValue('safe-url');
        // No return value needed for this method, it returns void

        // Create the mock ElementRef
        const mockElementRef = {
            nativeElement: {
                addEventListener: vi.fn()
            }
        };

        fixture = createStandaloneComponentFixture(SectionComponent, [], // Component already has imports
        [
            { provide: ComicService, useValue: comicServiceSpy },
            { provide: ComicStateService, useValue: stateServiceSpy },
            { provide: KeyboardService, useValue: keyboardServiceSpy },
            { provide: DomSanitizer, useValue: sanitizerSpy },
            { provide: ElementRef, useValue: mockElementRef }
        ]);

        component = fixture.componentInstance;
        component.content = mockComic;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should load avatar when visible (lazy loading)', async () => {
        // Initially, avatar should NOT be loaded
        expect(comicServiceSpy.getAvatar).not.toHaveBeenCalled();

        // Manually trigger the lazy loading (simulating IntersectionObserver)
        // Access private method via any type assertion
        (component as any).loadImagesLazily();

        // Now avatar should be loaded
        setTimeout(() => {
            expect(comicServiceSpy.getAvatar).toHaveBeenCalledWith(mockComic.id);
            ;
        }, 10);
    });

    it('should load latest comic when visible (lazy loading)', async () => {
        // Initially, latest comic should NOT be loaded
        expect(comicServiceSpy.getLatest).not.toHaveBeenCalled();

        // Manually trigger the lazy loading (simulating IntersectionObserver)
        (component as any).loadImagesLazily();

        // Now latest comic should be loaded
        setTimeout(() => {
            expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(mockComic.id);
            ;
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
        expect(component.loading()).toBe(false);
    });

    it('should handle navigation to previous comic', () => {
        component.imageDate = '2023-05-09';
        component.onPrev();
        expect(comicServiceSpy.getPrev).toHaveBeenCalledWith(mockComic.id, '2023-05-09');
        expect(component.loading()).toBe(false);
    });

    it('should handle navigation to next comic', () => {
        component.imageDate = '2023-05-09';
        component.onNext();
        expect(comicServiceSpy.getNext).toHaveBeenCalledWith(mockComic.id, '2023-05-09');
        expect(component.loading()).toBe(false);
    });

    it('should handle navigation to latest comic', () => {
        component.onNavigateLast();
        expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(mockComic.id);
        expect(component.loading()).toBe(false);
    });

    it('should handle errors when loading comics', () => {
        // Reset the getLatest spy to throw an error
        comicServiceSpy.getLatest.mockReturnValue(throwError(() => new Error('Test error')));

        component.onNavigateLast();
        expect(component.error()).toContain('Could not load latest comic');
        expect(component.loading()).toBe(false);
    });

    it('should set strip for large comics (CSS handles responsive sizing)', () => {
        const largeImage: ImageDto = {
            imageData: 'base64data',
            mimeType: 'image/png',
            width: 1200,
            height: 900,
            imageDate: '2023-05-09'
        };

        const largeResult: ComicNavigationResult = {
            found: true,
            image: largeImage,
            nearestPreviousDate: '2023-05-08',
            nearestNextDate: null
        };

        comicServiceSpy.getLatest.mockReturnValue(of(largeResult));
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

        const smallResult: ComicNavigationResult = {
            found: true,
            image: smallImage,
            nearestPreviousDate: '2023-05-08',
            nearestNextDate: null
        };

        comicServiceSpy.getLatest.mockReturnValue(of(smallResult));
        component.onNavigateLast();

        // Verify strip data URL is set correctly (CSS handles sizing)
        expect(component.content.strip).toContain('data:image/png;base64,');
        expect(component.imageDate).toBe('2023-05-09');
    });

    // Error handling tests
    describe('Error Handling', () => {
        it('should handle error when navigating to first comic', () => {
            comicServiceSpy.getEarliest.mockReturnValue(throwError(() => new Error('API error')));

            component.onNavigateFirst();

            expect(component.error()).toContain('Could not load first comic');
            expect(component.loading()).toBe(false);
        });

        it('should handle error when navigating to previous comic', () => {
            component.imageDate = '2023-05-09';
            comicServiceSpy.getPrev.mockReturnValue(throwError(() => new Error('API error')));

            component.onPrev();

            expect(component.error()).toContain('Could not load previous comic');
            expect(component.loading()).toBe(false);
        });

        it('should handle error when navigating to next comic', () => {
            component.imageDate = '2023-05-09';
            comicServiceSpy.getNext.mockReturnValue(throwError(() => new Error('API error')));

            component.onNext();

            expect(component.error()).toContain('Could not load next comic');
            expect(component.loading()).toBe(false);
        });

        it('should handle error when loading avatar', () => {
            comicServiceSpy.getAvatar.mockReturnValue(throwError(() => new Error('Avatar error')));

            component.loadAvatar();

            // Error should be logged but not displayed (non-critical)
            expect(comicServiceSpy.getAvatar).toHaveBeenCalledWith(mockComic.id);
        });
    });

    // Boundary handling tests
    describe('Boundary Handling', () => {
        it('should handle AT_END boundary when navigating first', () => {
            const boundaryResult: ComicNavigationResult = {
                found: false,
                image: null,
                reason: 'AT_END',
                requestedDate: '2023-01-01',
                nearestPreviousDate: '2022-12-31',
                nearestNextDate: null
            };

            comicServiceSpy.getEarliest.mockReturnValue(of(boundaryResult));

            component.onNavigateFirst();

            // Boundary messages go to boundaryMessage() not error()
            expect(component.boundaryMessage()).toContain("You're viewing the latest comic");
            expect(component.loading()).toBe(false);
        });

        it('should handle AT_BEGINNING boundary when navigating previous', () => {
            component.imageDate = '2023-05-09';
            const boundaryResult: ComicNavigationResult = {
                found: false,
                image: null,
                reason: 'AT_BEGINNING',
                requestedDate: '2020-01-01',
                nearestPreviousDate: null,
                nearestNextDate: '2020-01-02'
            };

            comicServiceSpy.getPrev.mockReturnValue(of(boundaryResult));

            component.onPrev();

            // Boundary messages go to boundaryMessage() not error()
            expect(component.boundaryMessage()).toContain("You're viewing the oldest comic");
            expect(component.loading()).toBe(false);
        });

        it('should handle NO_COMICS_AVAILABLE boundary', () => {
            const boundaryResult: ComicNavigationResult = {
                found: false,
                image: null,
                reason: 'NO_COMICS_AVAILABLE',
                requestedDate: '2023-01-01',
                nearestPreviousDate: null,
                nearestNextDate: null
            };

            comicServiceSpy.getLatest.mockReturnValue(of(boundaryResult));

            component.onNavigateLast();

            expect(component.error()).toContain('No comics available for this comic strip');
            expect(component.loading()).toBe(false);
        });

        it('should handle unknown boundary reason with default message', () => {
            const boundaryResult: ComicNavigationResult = {
                found: false,
                image: null,
                reason: 'UNKNOWN_REASON',
                requestedDate: '2023-01-01',
                nearestPreviousDate: null,
                nearestNextDate: null
            };

            comicServiceSpy.getNext.mockReturnValue(of(boundaryResult));
            component.imageDate = '2023-05-09';

            component.onNext();

            expect(component.error()).toContain('Unable to load comic');
            expect(component.loading()).toBe(false);
        });
    });

    // Defensive checks tests
    describe('Defensive Checks', () => {
        // Note: The defensive checks for missing imageDate are difficult to test in isolation
        // because ngOnInit() initializes imageDate before the tests run. The defensive code
        // exists as a safety net, but in normal operation imageDate will always be initialized.

        it('should handle missing newest date when both imageDate and newest are unavailable', () => {
            component.imageDate = ''; // Uninitialized
            component.content.newest = ''; // Also missing

            component.onPrev();

            expect(component.error()).toContain('Cannot navigate: comic date not available');
            expect(comicServiceSpy.getPrev).not.toHaveBeenCalled();
        });

        it('should handle missing newest date in onNext when both are unavailable', () => {
            component.imageDate = ''; // Uninitialized
            component.content.newest = ''; // Also missing

            component.onNext();

            expect(component.error()).toContain('Cannot navigate: comic date not available');
            expect(comicServiceSpy.getNext).not.toHaveBeenCalled();
        });
    });

    // Edge cases for URL handling
    describe('URL Handling Edge Cases', () => {
        it('should return null for "None" string in avatar URL', () => {
            component.content.avatar = 'None';
            expect(component.getAvatarUrl()).toBeNull();
        });

        it('should return null for "null" string in avatar URL', () => {
            component.content.avatar = 'null';
            expect(component.getAvatarUrl()).toBeNull();
        });

        it('should return null for "undefined" string in avatar URL', () => {
            component.content.avatar = 'undefined';
            expect(component.getAvatarUrl()).toBeNull();
        });

        it('should return null for empty string in avatar URL', () => {
            component.content.avatar = '';
            expect(component.getAvatarUrl()).toBeNull();
        });

        it('should return null for "None" string in strip URL', () => {
            component.content.strip = 'None';
            expect(component.getStripUrl()).toBeNull();
        });

        it('should return null for "null" string in strip URL', () => {
            component.content.strip = 'null';
            expect(component.getStripUrl()).toBeNull();
        });

        it('should return null for "undefined" string in strip URL', () => {
            component.content.strip = 'undefined';
            expect(component.getStripUrl()).toBeNull();
        });

        it('should return null for empty string in strip URL', () => {
            component.content.strip = '';
            expect(component.getStripUrl()).toBeNull();
        });

        it('should return null for getAvatarImage when URL is null', () => {
            component.content.avatar = 'None';
            expect(component.getAvatarImage()).toBeNull();
        });

        it('should return null for getComicImage when URL is null', () => {
            component.content.strip = 'None';
            expect(component.getComicImage()).toBeNull();
        });
    });

    // Image error handling tests
    describe('Image Error Handling', () => {
        it('should handle avatar image load error', () => {
            component.onAvatarError();
            expect(component.avatarLoadFailed()).toBe(true);
        });

        it('should handle strip image load error', () => {
            component.onStripError();
            expect(component.stripLoadFailed()).toBe(true);
        });
    });
});
