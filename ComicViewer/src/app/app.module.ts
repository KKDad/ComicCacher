import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { ComicsComponent } from './comics/comics.component';
import { ComicStripComponent } from './comic-strip/comic-strip.component';

@NgModule({
  declarations: [
    AppComponent,
    ComicsComponent,
    ComicStripComponent
  ],
  imports: [
    BrowserModule,
    FormsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
