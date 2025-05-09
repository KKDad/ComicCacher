import { Directive, ElementRef, Input, OnInit } from '@angular/core';

/**
 * A directive to enhance accessibility features on elements.
 * 
 * This directive adds various accessibility attributes to elements to improve 
 * screen reader support and keyboard navigation.
 */
@Directive({
  selector: '[appA11y]',
  standalone: true
})
export class A11yHelperDirective implements OnInit {
  @Input() appA11yLabel: string;
  @Input() appA11yRole: string;
  @Input() appA11yTabIndex: number;
  @Input() appA11yLive: 'polite' | 'assertive' | 'off';
  @Input() appA11yHidden: boolean;
  @Input() appA11yDescribedBy: string;
  @Input() appA11yExpanded: boolean;
  @Input() appA11yPressed: boolean;
  @Input() appA11ySelected: boolean;
  @Input() appA11yControls: string;

  constructor(private el: ElementRef) {}

  ngOnInit() {
    const element = this.el.nativeElement;

    // Add ARIA attributes based on input properties
    if (this.appA11yLabel !== undefined) {
      element.setAttribute('aria-label', this.appA11yLabel);
    }

    if (this.appA11yRole !== undefined) {
      element.setAttribute('role', this.appA11yRole);
    }

    if (this.appA11yTabIndex !== undefined) {
      element.setAttribute('tabindex', this.appA11yTabIndex.toString());
    }

    if (this.appA11yLive !== undefined) {
      element.setAttribute('aria-live', this.appA11yLive);
    }

    if (this.appA11yHidden !== undefined) {
      element.setAttribute('aria-hidden', this.appA11yHidden.toString());
    }

    if (this.appA11yDescribedBy !== undefined) {
      element.setAttribute('aria-describedby', this.appA11yDescribedBy);
    }

    if (this.appA11yExpanded !== undefined) {
      element.setAttribute('aria-expanded', this.appA11yExpanded.toString());
    }

    if (this.appA11yPressed !== undefined) {
      element.setAttribute('aria-pressed', this.appA11yPressed.toString());
    }

    if (this.appA11ySelected !== undefined) {
      element.setAttribute('aria-selected', this.appA11ySelected.toString());
    }

    if (this.appA11yControls !== undefined) {
      element.setAttribute('aria-controls', this.appA11yControls);
    }
  }
}