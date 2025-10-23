import {TestBed} from '@angular/core/testing';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';

import {ComicService} from './comic.service';
import {Comic} from './dto/comic';
import {ImageDto} from './dto/image';
import {Injectable} from '@angular/core';

@Injectable()
class MockComicService extends ComicService {
  constructor() {
    super();
    // Override the constructor to prevent HTTP requests
    // We'll mock the refresh method instead
  }

  // Override refresh to do nothing in constructor
  refresh(): void {
    // No-op for testing
  }
}

describe('ComicService', () => {
  let service: ComicService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: ComicService, useClass: MockComicService }
      ]
    });

    service = TestBed.inject(ComicService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // Verify that no unmatched requests are outstanding
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getComic', () => {
    it('should return a comic by id', () => {
      const mockComic: Comic = {
        id: 123,
        name: 'Test Comic',
        author: 'Test Author',
        oldest: '2020-01-01',
        newest: '2020-12-31',
        description: 'Test description',
        strip: null,
        avatar: null
      };

      service.getComic(123).subscribe(comic => {
        expect(comic).toEqual(mockComic);
      });

      const req = httpMock.expectOne('api/v1/comics/123');
      expect(req.request.method).toBe('GET');
      req.flush(mockComic);
    });

    it('should use default id if id is 0', () => {
      service.getComic(0).subscribe();

      // Should use the default ID defined in the service (14293307)
      const req = httpMock.expectOne('api/v1/comics/14293307');
      expect(req.request.method).toBe('GET');
      req.flush({});
    });

    it('should handle errors', () => {
      let errorHandled = false;
      
      // Setup spies
      spyOn(console, 'error');
      
      service.getComic(999).subscribe({
        next: () => {
          // If we get here, it means the error was handled
          errorHandled = true;
        }
      });

      const req = httpMock.expectOne('api/v1/comics/999');
      req.error(new ErrorEvent('Network error'));
      
      // Verify error was logged and handled
      expect(console.error).toHaveBeenCalled();
      expect(errorHandled).toBeTrue();
    });
  });

  describe('getLatest', () => {
    it('should return the latest strip for a comic', () => {
      const mockImage: ImageDto = {
        mimeType: 'image/png',
        imageData: 'base64-encoded-data',
        height: 500,
        width: 800,
        imageDate: '2020-12-31'
      };

      service.getLatest(123).subscribe(image => {
        expect(image).toEqual(mockImage);
      });

      const req = httpMock.expectOne('api/v1/comics/123/strips/last');
      expect(req.request.method).toBe('GET');
      req.flush(mockImage);
    });

    it('should return EMPTY if id is 0', () => {
      let wasEmitted = false;
      
      const subscription = service.getLatest(0).subscribe({
        next: () => {
          wasEmitted = true;
        }
      });

      // No HTTP requests should be made
      httpMock.expectNone('api/v1/comics/0/strips/last');
      
      // Verify nothing was emitted
      expect(wasEmitted).toBeFalse();

      subscription.unsubscribe();
    });
  });

  describe('getEarliest', () => {
    it('should return the earliest strip for a comic', () => {
      const mockImage: ImageDto = {
        mimeType: 'image/png',
        imageData: 'base64-encoded-data',
        height: 500,
        width: 800,
        imageDate: '2020-01-01'
      };

      service.getEarliest(123).subscribe(image => {
        expect(image).toEqual(mockImage);
      });

      const req = httpMock.expectOne('api/v1/comics/123/strips/first');
      expect(req.request.method).toBe('GET');
      req.flush(mockImage);
    });

    it('should return EMPTY if id is 0', () => {
      let wasEmitted = false;
      
      const subscription = service.getEarliest(0).subscribe({
        next: () => {
          wasEmitted = true;
        }
      });

      // No HTTP requests should be made
      httpMock.expectNone('api/v1/comics/0/strips/first');
      
      // Verify nothing was emitted
      expect(wasEmitted).toBeFalse();

      subscription.unsubscribe();
    });

    it('should handle errors', () => {
      let errorHandled = false;
      
      // Setup spies
      spyOn(console, 'error');
      
      service.getEarliest(999).subscribe({
        next: () => {
          // If we get here, it means the error was handled
          errorHandled = true;
        }
      });

      const req = httpMock.expectOne('api/v1/comics/999/strips/first');
      req.error(new ErrorEvent('Network error'));
      
      // Verify error was logged and handled
      expect(console.error).toHaveBeenCalled();
      expect(errorHandled).toBeTrue();
    });
  });

  describe('getNext', () => {
    it('should return the next strip for a comic and date', () => {
      const mockImage: ImageDto = {
        mimeType: 'image/png',
        imageData: 'base64-encoded-data',
        height: 500,
        width: 800,
        imageDate: '2020-01-02'
      };

      service.getNext(123, '2020-01-01').subscribe(image => {
        expect(image).toEqual(mockImage);
      });

      const req = httpMock.expectOne('api/v1/comics/123/next/2020-01-01');
      expect(req.request.method).toBe('GET');
      req.flush(mockImage);
    });

    it('should return EMPTY if id is 0', () => {
      let wasEmitted = false;
      
      const subscription = service.getNext(0, '2020-01-01').subscribe({
        next: () => {
          wasEmitted = true;
        }
      });

      // No HTTP requests should be made
      httpMock.expectNone('api/v1/comics/0/next/2020-01-01');
      
      // Verify nothing was emitted
      expect(wasEmitted).toBeFalse();

      subscription.unsubscribe();
    });

    it('should handle errors', () => {
      let errorHandled = false;
      
      // Setup spies
      spyOn(console, 'error');
      
      service.getNext(999, '2020-01-01').subscribe({
        next: () => {
          // If we get here, it means the error was handled
          errorHandled = true;
        }
      });

      const req = httpMock.expectOne('api/v1/comics/999/next/2020-01-01');
      req.error(new ErrorEvent('Network error'));
      
      // Verify error was logged and handled
      expect(console.error).toHaveBeenCalled();
      expect(errorHandled).toBeTrue();
    });
  });

  describe('getPrev', () => {
    it('should return the previous strip for a comic and date', () => {
      const mockImage: ImageDto = {
        mimeType: 'image/png',
        imageData: 'base64-encoded-data',
        height: 500,
        width: 800,
        imageDate: '2020-01-01'
      };

      service.getPrev(123, '2020-01-02').subscribe(image => {
        expect(image).toEqual(mockImage);
      });

      const req = httpMock.expectOne('api/v1/comics/123/previous/2020-01-02');
      expect(req.request.method).toBe('GET');
      req.flush(mockImage);
    });

    it('should return EMPTY if id is 0', () => {
      let wasEmitted = false;
      
      const subscription = service.getPrev(0, '2020-01-02').subscribe({
        next: () => {
          wasEmitted = true;
        }
      });

      // No HTTP requests should be made
      httpMock.expectNone('api/v1/comics/0/previous/2020-01-02');
      
      // Verify nothing was emitted
      expect(wasEmitted).toBeFalse();

      subscription.unsubscribe();
    });

    it('should handle errors', () => {
      let errorHandled = false;
      
      // Setup spies
      spyOn(console, 'error');
      
      service.getPrev(999, '2020-01-02').subscribe({
        next: () => {
          // If we get here, it means the error was handled
          errorHandled = true;
        }
      });

      const req = httpMock.expectOne('api/v1/comics/999/previous/2020-01-02');
      req.error(new ErrorEvent('Network error'));
      
      // Verify error was logged and handled
      expect(console.error).toHaveBeenCalled();
      expect(errorHandled).toBeTrue();
    });
  });

  describe('getAvatar', () => {
    it('should return the avatar for a comic', () => {
      const mockImage: ImageDto = {
        mimeType: 'image/png',
        imageData: 'base64-encoded-data',
        height: 100,
        width: 100,
        imageDate: '2025-10-23'
      };

      service.getAvatar(123).subscribe(image => {
        expect(image).toEqual(mockImage);
      });

      const req = httpMock.expectOne('api/v1/comics/123/avatar');
      expect(req.request.method).toBe('GET');
      req.flush(mockImage);
    });

    it('should return EMPTY if id is 0', () => {
      let wasEmitted = false;
      
      const subscription = service.getAvatar(0).subscribe({
        next: () => {
          wasEmitted = true;
        }
      });

      // No HTTP requests should be made
      httpMock.expectNone('api/v1/comics/0/avatar');
      
      // Verify nothing was emitted
      expect(wasEmitted).toBeFalse();

      subscription.unsubscribe();
    });

    it('should handle errors', () => {
      let errorHandled = false;
      
      // Setup spies
      spyOn(console, 'error');
      
      service.getAvatar(999).subscribe({
        next: () => {
          // If we get here, it means the error was handled
          errorHandled = true;
        }
      });

      const req = httpMock.expectOne('api/v1/comics/999/avatar');
      req.error(new ErrorEvent('Network error'));
      
      // Verify error was logged and handled
      expect(console.error).toHaveBeenCalled();
      expect(errorHandled).toBeTrue();
    });
  });

  describe('getComics', () => {
    it('should return comics from the signal', () => {
      // Set up comics signal manually
      (service as any).comicsSignal.set([
        { id: 1, name: 'Comic 1' },
        { id: 2, name: 'Comic 2' }
      ]);

      service.getComics().subscribe(comics => {
        expect(comics.length).toBe(2);
        expect(comics[0].name).toBe('Comic 1');
        expect(comics[1].name).toBe('Comic 2');
      });
    });
  });

  describe('refresh', () => {
    it('should fetch comics and update the signal', () => {
      const mockComics: Comic[] = [
        { id: 1, name: 'Comic 1', author: 'Author 1', description: 'Desc 1', oldest: '2020-01-01', newest: '2020-12-31', strip: null, avatar: null },
        { id: 2, name: 'Comic 2', author: 'Author 2', description: 'Desc 2', oldest: '2020-01-01', newest: '2020-12-31', strip: null, avatar: null }
      ];

      // We can test the real refresh method on our mock service
      (service as MockComicService).refresh = ComicService.prototype.refresh;
      
      // Call refresh
      service.refresh();
      
      // Verify HTTP request was made
      const req = httpMock.expectOne('api/v1/comics');
      expect(req.request.method).toBe('GET');
      
      // Respond with mock data
      req.flush(mockComics);
      
      // Check if signal was updated by using getComics
      service.getComics().subscribe(comics => {
        expect(comics).toEqual(mockComics);
      });
    });

    it('should handle errors when refreshing comics', () => {
      // Spy on console.error
      spyOn(console, 'error');
      
      // We can test the real refresh method on our mock service
      (service as MockComicService).refresh = ComicService.prototype.refresh;
      
      // Call refresh
      service.refresh();
      
      // Get the request
      const req = httpMock.expectOne('api/v1/comics');
      
      // Simulate network error
      req.error(new ErrorEvent('Network error'));
      
      // Verify error was logged
      expect(console.error).toHaveBeenCalled();
      
      // Signal should have been set with empty array
      service.getComics().subscribe(comics => {
        expect(comics).toEqual([]);
      });
    });
  });
});