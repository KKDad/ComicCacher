import { Component, AfterViewInit, HostListener, signal } from '@angular/core';
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

  // Track if the navigation bar is collapsed
  isNavCollapsed = signal(false);

  // Track last scroll position
  private lastScrollTop = 0;

  constructor(private containerComponent: ContainerComponent) { }

  ngAfterViewInit(): void {
    // Subscribe to container component's scroll events
    this.containerComponent.scrollinfo.subscribe((data: NavBarOption) => {
      this.onWindowScroll(data);
    });
  }

  // Listen for window scroll events
  @HostListener('window:scroll', ['$event'])
  onGlobalScroll(event: Event): void {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;

    // Determine if we should collapse the nav bar
    if (scrollTop > 100 && scrollTop > this.lastScrollTop) {
      this.isNavCollapsed.set(true);
    } else if (scrollTop < 10 || scrollTop < this.lastScrollTop) {
      this.isNavCollapsed.set(false);
    }

    this.lastScrollTop = scrollTop;
  }

  // Handle scroll events from container component
  onWindowScroll(data: NavBarOption) {
    switch (data) {
      case NavBarOption.Show:
        this.isNavCollapsed.set(false);
        break;
      case NavBarOption.Hide:
        this.isNavCollapsed.set(true);
        break;
      default:
        // Do nothing
    }
  }
}
