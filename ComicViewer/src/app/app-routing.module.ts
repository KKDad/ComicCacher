import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';


import { DashboardComponent }   from './dashboard/dashboard.component';
import { ComicsComponent }      from './comics/comics.component';
import { ComicStripComponent }  from './comic-strip/comic-strip.component';


const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'detail/:id', component: ComicsComponent },
  { path: 'comics', component: ComicStripComponent }
];

@NgModule({
  imports: [ RouterModule.forRoot(routes) ],  
  exports: [ RouterModule ]
})
export class AppRoutingModule {}