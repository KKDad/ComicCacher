import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule }    from '@angular/common/http';

import { ScrollingModule } from '@angular/cdk/scrolling';
import { ScrollingModule as ExperimentalScrollingModule } from '@angular/cdk-experimental/scrolling';

import { AppComponent } from './app.component';
import { SectionComponent } from './comicpage/section/section.component';
import { ContainerComponent } from './comicpage/container/container.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MatButtonModule, MatCardModule} from '@angular/material';
import { AppRoutingModule } from './app-routing.module';
import { RefreshComponent } from './refresh/refresh.component';
import { ComicpageComponent } from './comicpage/comicpage.component';
import { AboutComponent } from './about/about.component'


@NgModule({
  declarations: [
    AppComponent,
    SectionComponent,
    ContainerComponent,
    RefreshComponent,
    ComicpageComponent,
    AboutComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    BrowserAnimationsModule,
    MatButtonModule,
    MatCardModule,
    AppRoutingModule,
    ScrollingModule,
    ExperimentalScrollingModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
