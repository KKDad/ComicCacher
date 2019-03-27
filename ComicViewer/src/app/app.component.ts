import { Component, NgModule } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  template: `<container [sections]="sections"></container>`
})
export class AppComponent {
  title = 'The Comic Viewer';

  public sections = [
    {name:"Bird",img:"https://upload.wikimedia.org/wikipedia/commons/thumb/2/2a/Hummingbird.jpg/320px-Hummingbird.jpg"},
    {name:"Wheel", img:"https://upload.wikimedia.org/wikipedia/commons/thumb/c/c6/The_OC_Fair_ferris_wheel.jpg/320px-The_OC_Fair_ferris_wheel.jpg"},
    {name:"Horses", img:"https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/D%C3%BClmen%2C_Merfeld%2C_D%C3%BClmener_Wildpferde_in_der_Wildbahn_--_2016_--_4740.jpg/320px-D%C3%BClmen%2C_Merfeld%2C_D%C3%BClmener_Wildpferde_in_der_Wildbahn_--_2016_--_4740.jpg"},
    {name:"Lamp",img:"https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Gl%C3%BChlampe_explodiert.jpg/320px-Gl%C3%BChlampe_explodiert.jpg"}
  ];
    
}
