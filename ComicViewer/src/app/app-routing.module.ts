import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { RefreshComponent } from './refresh/refresh.component'
import { ContainerComponent } from './container/container.component'
import { ComicpageComponent } from './comicpage/comicpage.component'

const routes: Routes = [
  { path: 'refresh', component: RefreshComponent },
  { path: 'comics', component: ComicpageComponent }
];

@NgModule({
  imports: [ RouterModule.forRoot(routes) ],
  exports: [ RouterModule ]
})
export class AppRoutingModule {}