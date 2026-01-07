import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { TestBed } from '@angular/core/testing';
import { ComicStateService } from './comic-state.service';
import { ComicService } from '../comic.service';
import { of, throwError } from 'rxjs';
import { Comic } from '../dto/comic';
import { ImageDto } from '../dto/image';

describe('ComicStateService', () => {
    let service: ComicStateService;
    let comicServiceSpy: any;  // originally Partial<ComicService>

    const mockComics: Comic[] = [
        {
            id: 1,
            name: 'Comic 1',
            author: 'Author 1',
            oldest: '2020-01-01',
            newest: '2020-12-31',
            enabled: true,
            description: 'Test comic 1',
            strip: '',
            avatar: ''
        },
        {
            id: 2,
            name: 'Comic 2',
            author: 'Author 2',
            oldest: '2020-01-01',
            newest: '2020-12-31',
            enabled: true,
            description: 'Test comic 2',
            strip: '',
            avatar: ''
        }
    ];

    const mockImageDto: ImageDto = {
        imageDate: '2020-06-15',
        imageData: 'base64data',
        mimeType: 'image/png',
        height: 100,
        width: 100
    };

    const mockNavigationResult = {
        found: true,
        image: mockImageDto,
        nearestPreviousDate: '2020-06-14',
        nearestNextDate: '2020-06-16'
    };

    beforeEach(() => {
        const spy = {
            getComics: vi.fn().mockName("ComicService.getComics"),
            getLatest: vi.fn().mockName("ComicService.getLatest"),
            getEarliest: vi.fn().mockName("ComicService.getEarliest"),
            getNext: vi.fn().mockName("ComicService.getNext"),
            getPrev: vi.fn().mockName("ComicService.getPrev")
        };

        TestBed.configureTestingModule({
            providers: [
                ComicStateService,
                { provide: ComicService, useValue: spy }
            ]
        });

        comicServiceSpy = TestBed.inject(ComicService) as Partial<ComicService>;
        comicServiceSpy.getComics.mockReturnValue(of(mockComics));
        comicServiceSpy.getLatest.mockReturnValue(of(mockNavigationResult));

        service = TestBed.inject(ComicStateService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('Initialization', () => {
        it('should load comics on initialization', () => {
            expect(comicServiceSpy.getComics).toHaveBeenCalled();
        });

        it('should have initial state with empty comics array', async () => {
            // Give the service time to load comics
            setTimeout(() => {
                expect(service.comics().length).toBe(2);
                ;
            }, 10);
        });

        it('should have no selected comic initially', () => {
            expect(service.selectedComicId()).toBeNull();
        });

        it('should have no current strip initially', () => {
            expect(service.currentStrip()).toBeNull();
        });

        it('should not be loading initially', async () => {
            setTimeout(() => {
                expect(service.loading()).toBe(false);
                ;
            }, 10);
        });

        it('should have no error initially', () => {
            expect(service.error()).toBeNull();
        });
    });

    describe('loadComics', () => {
        it('should load comics successfully', async () => {
            service.loadComics();

            setTimeout(() => {
                expect(service.comics()).toEqual(mockComics);
                expect(service.loading()).toBe(false);
                expect(service.error()).toBeNull();
                ;
            }, 10);
        });

        it('should set error when loading comics fails', async () => {
            comicServiceSpy.getComics.mockReturnValue(throwError(() => new Error('Network error')));

            service.loadComics();

            setTimeout(() => {
                expect(service.error()).toContain('Error loading comics');
                expect(service.loading()).toBe(false);
                ;
            }, 10);
        });
    });

    describe('selectComic', () => {
        it('should select a comic by id', () => {
            service.selectComic(1);
            expect(service.selectedComicId()).toBe(1);
        });

        it('should update selectedComic computed value', async () => {
            setTimeout(() => {
                service.selectComic(1);
                expect(service.selectedComic()?.id).toBe(1);
                expect(service.selectedComic()?.name).toBe('Comic 1');
                ;
            }, 10);
        });

        it('should load latest strip when comic is selected', async () => {
            service.selectComic(1);

            setTimeout(() => {
                expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(1);
                expect(service.currentStrip()).toEqual(mockImageDto);
                ;
            }, 50);
        });

        it('should return null for selectedComic when no comic is selected', () => {
            expect(service.selectedComic()).toBeNull();
        });
    });

    describe('navigateToFirst', () => {
        beforeEach(() => {
            comicServiceSpy.getEarliest.mockReturnValue(of(mockNavigationResult));
        });

        it('should navigate to the first strip', async () => {
            service.selectComic(1);
            service.navigateToFirst();

            setTimeout(() => {
                expect(comicServiceSpy.getEarliest).toHaveBeenCalledWith(1);
                expect(service.currentStrip()).toEqual(mockImageDto);
                ;
            }, 50);
        });

        it('should do nothing when no comic is selected', () => {
            service.navigateToFirst();
            expect(comicServiceSpy.getEarliest).not.toHaveBeenCalled();
        });

        it('should set error when navigation fails', async () => {
            comicServiceSpy.getEarliest.mockReturnValue(throwError(() => new Error('Not found')));

            service.selectComic(1);
            service.navigateToFirst();

            setTimeout(() => {
                expect(service.error()).toContain('Error loading first strip');
                expect(service.loading()).toBe(false);
                ;
            }, 50);
        });
    });

    describe('navigateToPrevious', () => {
        beforeEach(() => {
            comicServiceSpy.getPrev.mockReturnValue(of(mockNavigationResult));
        });

        it('should navigate to the previous strip', async () => {
            service.selectComic(1);

            setTimeout(() => {
                const currentStrip = service.currentStrip();
                expect(currentStrip).toBeTruthy();

                service.navigateToPrevious();

                setTimeout(() => {
                    expect(comicServiceSpy.getPrev).toHaveBeenCalledWith(1, currentStrip!.imageDate);
                    ;
                }, 20);
            }, 50);
        });

        it('should do nothing when no comic is selected', () => {
            service.navigateToPrevious();
            expect(comicServiceSpy.getPrev).not.toHaveBeenCalled();
        });

        it('should set error when navigation fails', async () => {
            comicServiceSpy.getPrev.mockReturnValue(throwError(() => new Error('Not found')));

            service.selectComic(1);

            setTimeout(() => {
                service.navigateToPrevious();

                setTimeout(() => {
                    expect(service.error()).toContain('Error loading previous strip');
                    ;
                }, 20);
            }, 50);
        });
    });

    describe('navigateToNext', () => {
        beforeEach(() => {
            comicServiceSpy.getNext.mockReturnValue(of(mockNavigationResult));
        });

        it('should navigate to the next strip', async () => {
            service.selectComic(1);

            setTimeout(() => {
                const currentStrip = service.currentStrip();
                expect(currentStrip).toBeTruthy();

                service.navigateToNext();

                setTimeout(() => {
                    expect(comicServiceSpy.getNext).toHaveBeenCalledWith(1, currentStrip!.imageDate);
                    ;
                }, 20);
            }, 50);
        });

        it('should do nothing when no comic is selected', () => {
            service.navigateToNext();
            expect(comicServiceSpy.getNext).not.toHaveBeenCalled();
        });

        it('should set error when navigation fails', async () => {
            comicServiceSpy.getNext.mockReturnValue(throwError(() => new Error('Not found')));

            service.selectComic(1);

            setTimeout(() => {
                service.navigateToNext();

                setTimeout(() => {
                    expect(service.error()).toContain('Error loading next strip');
                    ;
                }, 20);
            }, 50);
        });
    });

    describe('navigateToLatest', () => {
        beforeEach(() => {
            comicServiceSpy.getLatest.mockReturnValue(of(mockNavigationResult));
        });

        it('should navigate to the latest strip', async () => {
            service.selectComic(1);
            service.navigateToLatest();

            setTimeout(() => {
                expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(1);
                expect(service.currentStrip()).toEqual(mockImageDto);
                ;
            }, 50);
        });

        it('should do nothing when no comic is selected', () => {
            service.navigateToLatest();
            // getLatest is called once during initialization (in constructor)
            expect(comicServiceSpy.getLatest).toHaveBeenCalledTimes(0);
        });

        it('should set error when navigation fails', async () => {
            comicServiceSpy.getLatest.mockReturnValue(throwError(() => new Error('Not found')));

            service.selectComic(1);

            setTimeout(() => {
                // The error comes from the constructor's subscription when selecting a comic
                expect(service.error()).toContain('Error loading comic strip');
                expect(service.loading()).toBe(false);
                ;
            }, 50);
        });
    });

    describe('Observables', () => {
        it('should provide comics$ observable', async () => {
            service.comics$.subscribe(comics => {
                if (comics.length > 0) {
                    expect(comics).toEqual(mockComics);
                    ;
                }
            });
        });

        it('should provide selectedComic$ observable', async () => {
            service.selectComic(1);

            setTimeout(() => {
                service.selectedComic$.subscribe(comic => {
                    expect(comic?.id).toBe(1);
                    ;
                });
            }, 10);
        });

        it('should provide currentStrip$ observable', async () => {
            service.selectComic(1);

            setTimeout(() => {
                service.currentStrip$.subscribe(strip => {
                    if (strip) {
                        expect(strip).toEqual(mockImageDto);
                        ;
                    }
                });
            }, 50);
        });
    });
});
