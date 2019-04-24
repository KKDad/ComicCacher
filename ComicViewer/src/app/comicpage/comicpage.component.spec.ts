import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ComicpageComponent } from './comicpage.component';

describe('ComicpageComponent', () => {
  let component: ComicpageComponent;
  let fixture: ComponentFixture<ComicpageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ComicpageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ComicpageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
