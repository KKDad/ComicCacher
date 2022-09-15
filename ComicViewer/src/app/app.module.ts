import { BrowserModule } from '@angular/platform-browser';
import { NgModule, Directive } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule }    from '@angular/common/http';

import { ScrollingModule } from '@angular/cdk/scrolling';
import {CdkVirtualScrollViewport} from "@angular/cdk/scrolling";

import { AppComponent } from './app.component';
import { SectionComponent } from './comicpage/section/section.component';
import { ContainerComponent, NavBarOption } from './comicpage/container/container.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MatCardContent, MatCardModule, MatCardActions, MatCardSubtitle, MatCardTitle } from '@angular/material/card';
import { AppRoutingModule } from './app-routing.module';
import { RefreshComponent } from './refresh/refresh.component';
import { ComicpageComponent } from './comicpage/comicpage.component';
import { AboutComponent } from './about/about.component';
import { RouterModule } from '@angular/router';


@Directive()
@NgModule({
  declarations: [
    AppComponent,
    SectionComponent,
    ContainerComponent,
    RefreshComponent,
    ComicpageComponent,
    AboutComponent, 
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    BrowserAnimationsModule,
    MatCardContent,
    MatCardModule,
    MatCardActions,
    MatCardSubtitle,
    MatCardTitle, 
    AppRoutingModule,
    ScrollingModule,
    CdkVirtualScrollViewport,    
    RouterModule
  ],
  providers: [ContainerComponent],
  bootstrap: [AppComponent]
})
export class AppModule {
  // containerComponent: any;

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
          console.log('Srink NavBar');
          break;
      default:
          console.log('Unknown');
      }
 }


 }
