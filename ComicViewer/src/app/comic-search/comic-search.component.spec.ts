import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ComicSearchComponent } from './comic-search.component';

describe('ComicSearchComponent', () => {
  let component: ComicSearchComponent;
  let fixture: ComponentFixture<ComicSearchComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ComicSearchComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ComicSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
