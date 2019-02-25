import { TestBed } from '@angular/core/testing';

import { ComicService } from './comic.service';

describe('ComicService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: ComicService = TestBed.get(ComicService);
    expect(service).toBeTruthy();
  });
});
