import {Component, OnInit} from '@angular/core';
import {MessageService} from '../message.service';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';

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
export class RefreshComponent implements OnInit {

  constructor(public messageService: MessageService) {}

  ngOnInit() {
  }

}
