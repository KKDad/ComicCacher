import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ComicStripComponent } from './comic-strip.component';

describe('ComicStripComponent', () => {
  let component: ComicStripComponent;
  let fixture: ComponentFixture<ComicStripComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ComicStripComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ComicStripComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
