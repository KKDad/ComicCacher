import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';


import { DashboardComponent }   from './dashboard/dashboard.component';
import { ComicsComponent }      from './comics/comics.component';
import { ComicStripComponent }  from './comic-strip/comic-strip.component';


const routes: Routes = [
  { path: '', redirectTo: '/comics', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  // { path: 'comics/:id', component: ComicStripComponent },
  { path: 'comics', component: ComicsComponent }
];

@NgModule({
  imports: [ RouterModule.forRoot(routes) ],  
  exports: [ RouterModule ]
})
export class AppRoutingModule {}