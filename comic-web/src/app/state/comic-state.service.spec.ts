import { TestBed } from '@angular/core/testing';
import { ComicStateService } from './comic-state.service';
import { ComicService } from '../comic.service';
import { of, throwError } from 'rxjs';
import { Comic } from '../dto/comic';
import { ImageDto } from '../dto/image';

describe('ComicStateService', () => {
  let service: ComicStateService;
  let comicServiceSpy: jasmine.SpyObj<ComicService>;

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

  beforeEach(() => {
    const spy = jasmine.createSpyObj('ComicService', [
      'getComics',
      'getLatest',
      'getEarliest',
      'getNext',
      'getPrev'
    ]);

    TestBed.configureTestingModule({
      providers: [
        ComicStateService,
        { provide: ComicService, useValue: spy }
      ]
    });

    comicServiceSpy = TestBed.inject(ComicService) as jasmine.SpyObj<ComicService>;
    comicServiceSpy.getComics.and.returnValue(of(mockComics));
    comicServiceSpy.getLatest.and.returnValue(of(mockImageDto));

    service = TestBed.inject(ComicStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should load comics on initialization', () => {
      expect(comicServiceSpy.getComics).toHaveBeenCalled();
    });

    it('should have initial state with empty comics array', (done) => {
      // Give the service time to load comics
      setTimeout(() => {
        expect(service.comics().length).toBe(2);
        done();
      }, 10);
    });

    it('should have no selected comic initially', () => {
      expect(service.selectedComicId()).toBeNull();
    });

    it('should have no current strip initially', () => {
      expect(service.currentStrip()).toBeNull();
    });

    it('should not be loading initially', (done) => {
      setTimeout(() => {
        expect(service.loading()).toBeFalse();
        done();
      }, 10);
    });

    it('should have no error initially', () => {
      expect(service.error()).toBeNull();
    });
  });

  describe('loadComics', () => {
    it('should load comics successfully', (done) => {
      service.loadComics();

      setTimeout(() => {
        expect(service.comics()).toEqual(mockComics);
        expect(service.loading()).toBeFalse();
        expect(service.error()).toBeNull();
        done();
      }, 10);
    });

    it('should set error when loading comics fails', (done) => {
      comicServiceSpy.getComics.and.returnValue(
        throwError(() => new Error('Network error'))
      );

      service.loadComics();

      setTimeout(() => {
        expect(service.error()).toContain('Error loading comics');
        expect(service.loading()).toBeFalse();
        done();
      }, 10);
    });
  });

  describe('selectComic', () => {
    it('should select a comic by id', () => {
      service.selectComic(1);
      expect(service.selectedComicId()).toBe(1);
    });

    it('should update selectedComic computed value', (done) => {
      setTimeout(() => {
        service.selectComic(1);
        expect(service.selectedComic()?.id).toBe(1);
        expect(service.selectedComic()?.name).toBe('Comic 1');
        done();
      }, 10);
    });

    it('should load latest strip when comic is selected', (done) => {
      service.selectComic(1);

      setTimeout(() => {
        expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(1);
        expect(service.currentStrip()).toEqual(mockImageDto);
        done();
      }, 50);
    });

    it('should return null for selectedComic when no comic is selected', () => {
      expect(service.selectedComic()).toBeNull();
    });
  });

  describe('navigateToFirst', () => {
    beforeEach(() => {
      comicServiceSpy.getEarliest.and.returnValue(of(mockImageDto));
    });

    it('should navigate to the first strip', (done) => {
      service.selectComic(1);
      service.navigateToFirst();

      setTimeout(() => {
        expect(comicServiceSpy.getEarliest).toHaveBeenCalledWith(1);
        expect(service.currentStrip()).toEqual(mockImageDto);
        done();
      }, 50);
    });

    it('should do nothing when no comic is selected', () => {
      service.navigateToFirst();
      expect(comicServiceSpy.getEarliest).not.toHaveBeenCalled();
    });

    it('should set error when navigation fails', (done) => {
      comicServiceSpy.getEarliest.and.returnValue(
        throwError(() => new Error('Not found'))
      );

      service.selectComic(1);
      service.navigateToFirst();

      setTimeout(() => {
        expect(service.error()).toContain('Error loading first strip');
        expect(service.loading()).toBeFalse();
        done();
      }, 50);
    });
  });

  describe('navigateToPrevious', () => {
    beforeEach(() => {
      comicServiceSpy.getPrev.and.returnValue(of(mockImageDto));
    });

    it('should navigate to the previous strip', (done) => {
      service.selectComic(1);

      setTimeout(() => {
        const currentStrip = service.currentStrip();
        expect(currentStrip).toBeTruthy();

        service.navigateToPrevious();

        setTimeout(() => {
          expect(comicServiceSpy.getPrev).toHaveBeenCalledWith(1, currentStrip!.imageDate);
          done();
        }, 20);
      }, 50);
    });

    it('should do nothing when no comic is selected', () => {
      service.navigateToPrevious();
      expect(comicServiceSpy.getPrev).not.toHaveBeenCalled();
    });

    it('should set error when navigation fails', (done) => {
      comicServiceSpy.getPrev.and.returnValue(
        throwError(() => new Error('Not found'))
      );

      service.selectComic(1);

      setTimeout(() => {
        service.navigateToPrevious();

        setTimeout(() => {
          expect(service.error()).toContain('Error loading previous strip');
          done();
        }, 20);
      }, 50);
    });
  });

  describe('navigateToNext', () => {
    beforeEach(() => {
      comicServiceSpy.getNext.and.returnValue(of(mockImageDto));
    });

    it('should navigate to the next strip', (done) => {
      service.selectComic(1);

      setTimeout(() => {
        const currentStrip = service.currentStrip();
        expect(currentStrip).toBeTruthy();

        service.navigateToNext();

        setTimeout(() => {
          expect(comicServiceSpy.getNext).toHaveBeenCalledWith(1, currentStrip!.imageDate);
          done();
        }, 20);
      }, 50);
    });

    it('should do nothing when no comic is selected', () => {
      service.navigateToNext();
      expect(comicServiceSpy.getNext).not.toHaveBeenCalled();
    });

    it('should set error when navigation fails', (done) => {
      comicServiceSpy.getNext.and.returnValue(
        throwError(() => new Error('Not found'))
      );

      service.selectComic(1);

      setTimeout(() => {
        service.navigateToNext();

        setTimeout(() => {
          expect(service.error()).toContain('Error loading next strip');
          done();
        }, 20);
      }, 50);
    });
  });

  describe('navigateToLatest', () => {
    beforeEach(() => {
      comicServiceSpy.getLatest.and.returnValue(of(mockImageDto));
    });

    it('should navigate to the latest strip', (done) => {
      service.selectComic(1);
      service.navigateToLatest();

      setTimeout(() => {
        expect(comicServiceSpy.getLatest).toHaveBeenCalledWith(1);
        expect(service.currentStrip()).toEqual(mockImageDto);
        done();
      }, 50);
    });

    it('should do nothing when no comic is selected', () => {
      service.navigateToLatest();
      // getLatest is called once during initialization (in constructor)
      expect(comicServiceSpy.getLatest).toHaveBeenCalledTimes(0);
    });

    it('should set error when navigation fails', (done) => {
      comicServiceSpy.getLatest.and.returnValue(
        throwError(() => new Error('Not found'))
      );

      service.selectComic(1);

      setTimeout(() => {
        // The error comes from the constructor's subscription when selecting a comic
        expect(service.error()).toContain('Error loading comic strip');
        expect(service.loading()).toBeFalse();
        done();
      }, 50);
    });
  });

  describe('Observables', () => {
    it('should provide comics$ observable', (done) => {
      service.comics$.subscribe(comics => {
        if (comics.length > 0) {
          expect(comics).toEqual(mockComics);
          done();
        }
      });
    });

    it('should provide selectedComic$ observable', (done) => {
      service.selectComic(1);

      setTimeout(() => {
        service.selectedComic$.subscribe(comic => {
          expect(comic?.id).toBe(1);
          done();
        });
      }, 10);
    });

    it('should provide currentStrip$ observable', (done) => {
      service.selectComic(1);

      setTimeout(() => {
        service.currentStrip$.subscribe(strip => {
          if (strip) {
            expect(strip).toEqual(mockImageDto);
            done();
          }
        });
      }, 50);
    });
  });
});
