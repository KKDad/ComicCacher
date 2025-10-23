import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';

import {ScrollingModule} from '@angular/cdk/scrolling';

import {AppComponent} from './app.component';
import {SectionComponent} from './comicpage/section/section.component';
import {ContainerComponent, NavBarOption} from './comicpage/container/container.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

import {MatCardModule} from '@angular/material/card';
import {AppRoutingModule} from './app-routing.module';
import {RefreshComponent} from './refresh/refresh.component';
import {ComicpageComponent} from './comicpage/comicpage.component';
import {AboutComponent} from './about/about.component';
import {RouterModule} from '@angular/router';
import {MatButtonModule} from '@angular/material/button';


@NgModule({ declarations: [
        AppComponent,
        SectionComponent,
        ContainerComponent,
        RefreshComponent,
        ComicpageComponent,
        AboutComponent
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        FormsModule,
        BrowserAnimationsModule,
        MatCardModule,
        AppRoutingModule,
        ScrollingModule,
        RouterModule,
        MatButtonModule], providers: [ContainerComponent, provideHttpClient(withInterceptorsFromDi())] })
export class AppModule {
  constructor(private containerComponent: ContainerComponent) { }

  ngAfterViewInit(): void {
    this.containerComponent.scrollinfo.subscribe((data: NavBarOption) => { this.onWindowScroll(data); });
}
  onWindowScroll(data: NavBarOption) {
    switch (data) {
      case NavBarOption.Show:
        break;
      case NavBarOption.Hide:
          break;
      default:
          break;
      }
 }


 }
