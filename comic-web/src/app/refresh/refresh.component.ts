import {Component} from '@angular/core';
import {MessageService} from '../message.service';

import {MatButtonModule} from '@angular/material/button';

/**
 * Component for refreshing comic data
 * @deprecated This component is not currently used and may be removed in a future version
 */
@Component({
  selector: 'app-refresh',
  templateUrl: './refresh.component.html',
  standalone: true,
  imports: [
    MatButtonModule
]
})
export class RefreshComponent {

  constructor(public messageService: MessageService) {}

}
