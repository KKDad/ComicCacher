import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';

/**
 * About page component displaying application information
 */
@Component({
  selector: 'app-about',
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.css'],
  standalone: true,
  imports: [
    MatCardModule,
    MatButtonModule
  ]
})
export class AboutComponent {

  constructor(private router: Router) { }

  /**
   * Navigate back to the home page
   */
  onNavigateHome(): void {
    this.router.navigateByUrl('/');
  }

}
