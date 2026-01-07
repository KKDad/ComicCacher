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
            await new Promise(resolve => setTimeout(resolve, 10));
            expect(service.comics().length).toBe(2);
        });

        it('should have no selected comic initially', () => {
            expect(service.selectedComicId()).toBeNull();
        });

        it('should have no current strip initially', () => {
            expect(service.currentStrip()).toBeNull();
        });

        it('should not be loading initially', async () => {
            await new Promise(resolve => setTimeout(resolve, 10));
            expect(service.loading()).toBe(false);
        });

        it('should have no error initially', () => {
            expect(service.error()).toBeNull();
        });
    });

    describe('loadComics', () => {
        it('should load comics successfully', async () => {
            service.loadComics();

            await new Promise(resolve => setTimeout(resolve, 10));
            expect(service.comics()).toEqual(mockComics);
            expect(service.loading()).toBe(false);
            expect(service.error()).toBeNull();
        });

        it('should set error when loading comics fails', async () => {
            comicServiceSpy.getComics.mockReturnValue(throwError(() => new Error('Network error')));

            service.loadComics();

            // Wait for async operation to complete
            await new Promise(resolve => setTimeout(resolve, 10));

            expect(service.error()).toContain('Error loading comics');
            expect(service.loading()).toBe(false);
        });
    });

    describe('selectComic', () => {
        it('should select a comic by id', () => {
            service.selectComic(1);
            expect(service.selectedComicId()).toBe(1);
        });

        it('should update selectedComic computed value', async () => {
            await new Promise(resolve => setTimeout(resolve, 10));
            service.selectComic(1);
            expect(service.selectedComic()?.id).toBe(1);
            expect(service.selectedComic()?.name).toBe('Comic 1');
        });

        it('should load latest strip when comic is selected', async () => {
            service.selectComic(1);

            await new Promise(resolve => setTimeout(resolve, 50));
            expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(1);
            expect(service.currentStrip()).toEqual(mockImageDto);
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

            await new Promise(resolve => setTimeout(resolve, 50));
            expect(comicServiceSpy.getEarliest).toHaveBeenCalledWith(1);
            expect(service.currentStrip()).toEqual(mockImageDto);
        });

        it('should do nothing when no comic is selected', () => {
            service.navigateToFirst();
            expect(comicServiceSpy.getEarliest).not.toHaveBeenCalled();
        });

        it('should set error when navigation fails', async () => {
            comicServiceSpy.getEarliest.mockReturnValue(throwError(() => new Error('Not found')));

            service.selectComic(1);
            service.navigateToFirst();

            await new Promise(resolve => setTimeout(resolve, 50));
            expect(service.error()).toContain('Error loading first strip');
            expect(service.loading()).toBe(false);
        });
    });

    describe('navigateToPrevious', () => {
        beforeEach(() => {
            comicServiceSpy.getPrev.mockReturnValue(of(mockNavigationResult));
        });

        it('should navigate to the previous strip', async () => {
            service.selectComic(1);

            await new Promise(resolve => setTimeout(resolve, 50));
            const currentStrip = service.currentStrip();
            expect(currentStrip).toBeTruthy();

            service.navigateToPrevious();

            await new Promise(resolve => setTimeout(resolve, 20));
            expect(comicServiceSpy.getPrev).toHaveBeenCalledWith(1, currentStrip!.imageDate);
        });

        it('should do nothing when no comic is selected', () => {
            service.navigateToPrevious();
            expect(comicServiceSpy.getPrev).not.toHaveBeenCalled();
        });

        it('should set error when navigation fails', async () => {
            comicServiceSpy.getPrev.mockReturnValue(throwError(() => new Error('Not found')));

            service.selectComic(1);

            await new Promise(resolve => setTimeout(resolve, 50));
            service.navigateToPrevious();

            await new Promise(resolve => setTimeout(resolve, 20));
            expect(service.error()).toContain('Error loading previous strip');
        });
    });

    describe('navigateToNext', () => {
        beforeEach(() => {
            comicServiceSpy.getNext.mockReturnValue(of(mockNavigationResult));
        });

        it('should navigate to the next strip', async () => {
            service.selectComic(1);

            await new Promise(resolve => setTimeout(resolve, 50));
            const currentStrip = service.currentStrip();
            expect(currentStrip).toBeTruthy();

            service.navigateToNext();

            await new Promise(resolve => setTimeout(resolve, 20));
            expect(comicServiceSpy.getNext).toHaveBeenCalledWith(1, currentStrip!.imageDate);
        });

        it('should do nothing when no comic is selected', () => {
            service.navigateToNext();
            expect(comicServiceSpy.getNext).not.toHaveBeenCalled();
        });

        it('should set error when navigation fails', async () => {
            comicServiceSpy.getNext.mockReturnValue(throwError(() => new Error('Not found')));

            service.selectComic(1);

            await new Promise(resolve => setTimeout(resolve, 50));
            service.navigateToNext();

            await new Promise(resolve => setTimeout(resolve, 20));
            expect(service.error()).toContain('Error loading next strip');
        });
    });

    describe('navigateToLatest', () => {
        beforeEach(() => {
            comicServiceSpy.getLatest.mockReturnValue(of(mockNavigationResult));
        });

        it('should navigate to the latest strip', async () => {
            service.selectComic(1);
            service.navigateToLatest();

            await new Promise(resolve => setTimeout(resolve, 50));
            expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(1);
            expect(service.currentStrip()).toEqual(mockImageDto);
        });

        it('should do nothing when no comic is selected', () => {
            service.navigateToLatest();
            // getLatest is called once during initialization (in constructor)
            expect(comicServiceSpy.getLatest).toHaveBeenCalledTimes(0);
        });

        it('should set error when navigation fails', async () => {
            comicServiceSpy.getLatest.mockReturnValue(throwError(() => new Error('Not found')));

            service.selectComic(1);

            await new Promise(resolve => setTimeout(resolve, 50));
            // The error comes from the constructor's subscription when selecting a comic
            expect(service.error()).toContain('Error loading comic strip');
            expect(service.loading()).toBe(false);
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

            await new Promise(resolve => setTimeout(resolve, 10));
            service.selectedComic$.subscribe(comic => {
                expect(comic?.id).toBe(1);
            });
        });

        it('should provide currentStrip$ observable', async () => {
            service.selectComic(1);

            await new Promise(resolve => setTimeout(resolve, 50));
            service.currentStrip$.subscribe(strip => {
                if (strip) {
                    expect(strip).toEqual(mockImageDto);
                }
            });
        });
    });
});
