import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { RefreshComponent } from './refresh/refresh.component'
import { ComicpageComponent } from './comicpage/comicpage.component'
import { AboutComponent } from './about/about.component';

const routes: Routes = [
  { path: '', redirectTo: '/comics', pathMatch: 'full' },
  { path: 'refresh', component: RefreshComponent },
  { path: 'comics', component: ComicpageComponent },
  { path: 'about', component: AboutComponent },

  // otherwise redirect to home
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [ RouterModule.forRoot(routes) ],
  exports: [ RouterModule ]
})
export class AppRoutingModule {}