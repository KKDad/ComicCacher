import {TestBed} from '@angular/core/testing';
import {KeyboardService} from './keyboard-service';

describe('KeyboardService', () => {
  let service: KeyboardService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(KeyboardService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listenForKeyCombo', () => {
    it('should emit when matching key is pressed', (done) => {
      const combo = { key: 'a' };
      const subscription = service.listenForKeyCombo(combo).subscribe((event) => {
        expect(event.key).toBe('a');
        subscription.unsubscribe();
        done();
      });

      // Simulate key press
      const event = new KeyboardEvent('keydown', { key: 'a' });
      document.dispatchEvent(event);
    });

    it('should match key case-insensitively', (done) => {
      const combo = { key: 'A' };
      const subscription = service.listenForKeyCombo(combo).subscribe((event) => {
        expect(event.key).toBe('a');
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 'a' });
      document.dispatchEvent(event);
    });

    it('should match key combo with alt modifier', (done) => {
      const combo = { key: 's', alt: true };
      const subscription = service.listenForKeyCombo(combo).subscribe((event) => {
        expect(event.altKey).toBeTrue();
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 's', altKey: true });
      document.dispatchEvent(event);
    });

    it('should match key combo with ctrl modifier', (done) => {
      const combo = { key: 'c', ctrl: true };
      const subscription = service.listenForKeyCombo(combo).subscribe((event) => {
        expect(event.ctrlKey).toBeTrue();
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 'c', ctrlKey: true });
      document.dispatchEvent(event);
    });

    it('should match key combo with shift modifier', (done) => {
      const combo = { key: 't', shift: true };
      const subscription = service.listenForKeyCombo(combo).subscribe((event) => {
        expect(event.shiftKey).toBeTrue();
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 't', shiftKey: true });
      document.dispatchEvent(event);
    });

    it('should match key combo with multiple modifiers', (done) => {
      const combo = { key: 'x', ctrl: true, shift: true };
      const subscription = service.listenForKeyCombo(combo).subscribe((event) => {
        expect(event.ctrlKey).toBeTrue();
        expect(event.shiftKey).toBeTrue();
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', {
        key: 'x',
        ctrlKey: true,
        shiftKey: true
      });
      document.dispatchEvent(event);
    });

    it('should not emit when modifier does not match', (done) => {
      const combo = { key: 'a', ctrl: true };
      let emitted = false;

      const subscription = service.listenForKeyCombo(combo).subscribe(() => {
        emitted = true;
      });

      // Press 'a' without ctrl
      const event = new KeyboardEvent('keydown', { key: 'a' });
      document.dispatchEvent(event);

      setTimeout(() => {
        expect(emitted).toBeFalse();
        subscription.unsubscribe();
        done();
      }, 10);
    });
  });

  describe('arrowKeys', () => {
    it('should emit "up" for ArrowUp', (done) => {
      const subscription = service.arrowKeys().subscribe((direction) => {
        expect(direction).toBe('up');
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 'ArrowUp' });
      document.dispatchEvent(event);
    });

    it('should emit "down" for ArrowDown', (done) => {
      const subscription = service.arrowKeys().subscribe((direction) => {
        expect(direction).toBe('down');
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 'ArrowDown' });
      document.dispatchEvent(event);
    });

    it('should emit "left" for ArrowLeft', (done) => {
      const subscription = service.arrowKeys().subscribe((direction) => {
        expect(direction).toBe('left');
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 'ArrowLeft' });
      document.dispatchEvent(event);
    });

    it('should emit "right" for ArrowRight', (done) => {
      const subscription = service.arrowKeys().subscribe((direction) => {
        expect(direction).toBe('right');
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 'ArrowRight' });
      document.dispatchEvent(event);
    });

    it('should not emit for non-arrow keys', (done) => {
      let emitted = false;

      const subscription = service.arrowKeys().subscribe(() => {
        emitted = true;
      });

      const event = new KeyboardEvent('keydown', { key: 'a' });
      document.dispatchEvent(event);

      setTimeout(() => {
        expect(emitted).toBeFalse();
        subscription.unsubscribe();
        done();
      }, 10);
    });
  });

  describe('escapeKey', () => {
    it('should emit when Escape key is pressed', (done) => {
      const subscription = service.escapeKey().subscribe((event) => {
        expect(event.key).toBe('Escape');
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 'Escape' });
      document.dispatchEvent(event);
    });
  });

  describe('enterKey', () => {
    it('should emit when Enter key is pressed', (done) => {
      const subscription = service.enterKey().subscribe((event) => {
        expect(event.key).toBe('Enter');
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      document.dispatchEvent(event);
    });
  });

  describe('spaceKey', () => {
    it('should emit when Space key is pressed', (done) => {
      const subscription = service.spaceKey().subscribe((event) => {
        expect(event.key).toBe(' ');
        subscription.unsubscribe();
        done();
      });

      const event = new KeyboardEvent('keydown', { key: ' ' });
      document.dispatchEvent(event);
    });
  });

  describe('registerComicNavigationShortcuts', () => {
    it('should call onFirst when Home key is pressed', (done) => {
      const onFirst = jasmine.createSpy('onFirst');
      const onPrev = jasmine.createSpy('onPrev');
      const onNext = jasmine.createSpy('onNext');
      const onLast = jasmine.createSpy('onLast');

      const subscription = service.registerComicNavigationShortcuts(
        onFirst,
        onPrev,
        onNext,
        onLast
      );

      const event = new KeyboardEvent('keydown', { key: 'Home' });
      document.dispatchEvent(event);

      setTimeout(() => {
        expect(onFirst).toHaveBeenCalled();
        subscription.unsubscribe();
        done();
      }, 10);
    });

    it('should call onPrev when ArrowLeft is pressed', (done) => {
      const onFirst = jasmine.createSpy('onFirst');
      const onPrev = jasmine.createSpy('onPrev');
      const onNext = jasmine.createSpy('onNext');
      const onLast = jasmine.createSpy('onLast');

      const subscription = service.registerComicNavigationShortcuts(
        onFirst,
        onPrev,
        onNext,
        onLast
      );

      const event = new KeyboardEvent('keydown', { key: 'ArrowLeft' });
      document.dispatchEvent(event);

      setTimeout(() => {
        expect(onPrev).toHaveBeenCalled();
        subscription.unsubscribe();
        done();
      }, 10);
    });

    // PageUp/PageDown are no longer part of registerComicNavigationShortcuts
    // They are now in registerComicListScrollShortcuts for viewport scrolling

    it('should call onNext when ArrowRight is pressed', (done) => {
      const onFirst = jasmine.createSpy('onFirst');
      const onPrev = jasmine.createSpy('onPrev');
      const onNext = jasmine.createSpy('onNext');
      const onLast = jasmine.createSpy('onLast');

      const subscription = service.registerComicNavigationShortcuts(
        onFirst,
        onPrev,
        onNext,
        onLast
      );

      const event = new KeyboardEvent('keydown', { key: 'ArrowRight' });
      document.dispatchEvent(event);

      setTimeout(() => {
        expect(onNext).toHaveBeenCalled();
        subscription.unsubscribe();
        done();
      }, 10);
    });


    it('should call onLast when End key is pressed', (done) => {
      const onFirst = jasmine.createSpy('onFirst');
      const onPrev = jasmine.createSpy('onPrev');
      const onNext = jasmine.createSpy('onNext');
      const onLast = jasmine.createSpy('onLast');

      const subscription = service.registerComicNavigationShortcuts(
        onFirst,
        onPrev,
        onNext,
        onLast
      );

      const event = new KeyboardEvent('keydown', { key: 'End' });
      document.dispatchEvent(event);

      setTimeout(() => {
        expect(onLast).toHaveBeenCalled();
        subscription.unsubscribe();
        done();
      }, 10);
    });

    it('should return a subscription that can be unsubscribed', () => {
      const onFirst = jasmine.createSpy('onFirst');
      const onPrev = jasmine.createSpy('onPrev');
      const onNext = jasmine.createSpy('onNext');
      const onLast = jasmine.createSpy('onLast');

      const subscription = service.registerComicNavigationShortcuts(
        onFirst,
        onPrev,
        onNext,
        onLast
      );

      expect(subscription).toBeDefined();
      expect(subscription.unsubscribe).toBeDefined();
      subscription.unsubscribe();
    });
  });

  describe('registerComicListScrollShortcuts', () => {
    it('should call onPageUp when PageUp is pressed', (done) => {
      const onPageUp = jasmine.createSpy('onPageUp');
      const onPageDown = jasmine.createSpy('onPageDown');

      const subscription = service.registerComicListScrollShortcuts(
        onPageUp,
        onPageDown
      );

      const event = new KeyboardEvent('keydown', { key: 'PageUp' });
      document.dispatchEvent(event);

      setTimeout(() => {
        expect(onPageUp).toHaveBeenCalled();
        subscription.unsubscribe();
        done();
      }, 10);
    });

    it('should call onPageDown when PageDown is pressed', (done) => {
      const onPageUp = jasmine.createSpy('onPageUp');
      const onPageDown = jasmine.createSpy('onPageDown');

      const subscription = service.registerComicListScrollShortcuts(
        onPageUp,
        onPageDown
      );

      const event = new KeyboardEvent('keydown', { key: 'PageDown' });
      document.dispatchEvent(event);

      setTimeout(() => {
        expect(onPageDown).toHaveBeenCalled();
        subscription.unsubscribe();
        done();
      }, 10);
    });

    it('should return a subscription that can be unsubscribed', () => {
      const onPageUp = jasmine.createSpy('onPageUp');
      const onPageDown = jasmine.createSpy('onPageDown');

      const subscription = service.registerComicListScrollShortcuts(
        onPageUp,
        onPageDown
      );

      expect(subscription).toBeDefined();
      expect(subscription.unsubscribe).toBeDefined();
      subscription.unsubscribe();
    });
  });
});
