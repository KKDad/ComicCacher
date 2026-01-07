import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class MessageService {
  private _messages = signal<string[]>([]);

  /** Messages as a readonly signal for zoneless change detection */
  readonly messages = computed(() => this._messages());

  add(message: string) {
    this._messages.update(msgs => [...msgs, message]);
  }

  clear() {
    this._messages.set([]);
  }
}
