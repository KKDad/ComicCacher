import { Component, NgModule } from '@angular/core';
import { Comic } from './dto/comic'
import { from } from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent 
{
  title = 'The Comic Viewer';
  sections = null; 
}
