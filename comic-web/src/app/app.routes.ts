import {Routes} from '@angular/router';
import {RefreshComponent} from './refresh/refresh.component';
import {ComicpageComponent} from './comicpage/comicpage.component';
import {AboutComponent} from './about/about.component';

export const routes: Routes = [
  // { path: '', redirectTo: '/comics', pathMatch: 'full' },
  { path: 'refresh', component: RefreshComponent },
  { path: '', component: ComicpageComponent },
  { path: 'about', component: AboutComponent },

  // otherwise redirect to the comic page
  { path: '**', redirectTo: '' }
];