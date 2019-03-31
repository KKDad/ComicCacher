import { Component, NgModule } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  template: `<container [sections]="sections"></container>`
})
export class AppComponent 
{
  title = 'The Comic Viewer';
   
}
