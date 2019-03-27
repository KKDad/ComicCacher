import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule }    from '@angular/common/http';

import { AppComponent } from './app.component';
import { ComicsComponent } from './comics/comics.component';
import { ComicStripComponent } from './comic-strip/comic-strip.component';
import { MessagesComponent } from './messages/messages.component';
import { AppRoutingModule } from './app-routing.module';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ComicSearchComponent } from './comic-search/comic-search.component';
import { SectionComponent } from './section/section.component';
import { ContainerComponent } from './container/container.component';

@NgModule({
  declarations: [
    AppComponent,
    ComicsComponent,
    ComicStripComponent,
    MessagesComponent,
    DashboardComponent,
    ComicSearchComponent,
    SectionComponent,
    ContainerComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    AppRoutingModule,
    HttpClientModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
