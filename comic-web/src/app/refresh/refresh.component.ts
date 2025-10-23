import {Component} from '@angular/core';
import {MessageService} from '../message.service';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';

/**
 * Component for refreshing comic data
 * @deprecated This component is not currently used and may be removed in a future version
 */
@Component({
  selector: 'app-refresh',
  templateUrl: './refresh.component.html',
  styleUrls: ['./refresh.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule
  ]
})
export class RefreshComponent {

  constructor(public messageService: MessageService) {}

}
