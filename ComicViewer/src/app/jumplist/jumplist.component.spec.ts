import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { JumplistComponent } from './jumplist.component';

describe('JumplistComponent', () => {
  let component: JumplistComponent;
  let fixture: ComponentFixture<JumplistComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ JumplistComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(JumplistComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
