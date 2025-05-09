import { Component, AfterViewInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ContainerComponent, NavBarOption } from './comicpage/container/container.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule
  ]
})
export class AppComponent implements AfterViewInit {
  title = 'The Comic Reader';

  constructor(private containerComponent: ContainerComponent) { }

  ngAfterViewInit(): void {
    this.containerComponent.scrollinfo.subscribe((data: NavBarOption) => { this.onWindowScroll(data); });
  }

  onWindowScroll(data: NavBarOption) {
    console.log('onWindowScroll', data);
    switch (data) {
      case NavBarOption.Show:
        console.log('Expand NavBar');
        break;
      case NavBarOption.Hide:
        console.log('Shrink NavBar');
        break;
      default:
        console.log('Unknown');
    }
  }
}
