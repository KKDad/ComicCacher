import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {ComponentFixture} from '@angular/core/testing';
import {RefreshComponent} from './refresh.component';
import {MessageService} from '../message.service';
import {click, createStandaloneComponentFixture, expectExists, expectNotExists} from '../testing/testing-utils';

describe('RefreshComponent', () => {
  let component: RefreshComponent;
  let fixture: ComponentFixture<RefreshComponent>;
  let messageService: MessageService;

  beforeEach(() => {
    // Create a real MessageService instance since it's simple
    messageService = new MessageService();

    fixture = createStandaloneComponentFixture(
      RefreshComponent,
      [], // Component already has imports
      [{ provide: MessageService, useValue: messageService }]
    );

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not display messages when there are none', () => {
    expectNotExists(fixture, 'h2', 'Message header should not be visible when no messages');
    expectNotExists(fixture, 'button', 'Clear button should not be visible when no messages');
  });

  it('should display messages when they exist', () => {
    // Add some messages
    messageService.add('Test message 1');
    messageService.add('Test message 2');
    fixture.detectChanges();

    // Verify messages are displayed
    expectExists(fixture, 'h2', 'Message header should be visible');
    expectExists(fixture, 'button', 'Clear button should be visible');
    expect(fixture.nativeElement.textContent).toContain('Test message 1');
    expect(fixture.nativeElement.textContent).toContain('Test message 2');
  });

  it('should clear messages when clear button is clicked', () => {
    // Add some messages
    messageService.add('Test message');
    fixture.detectChanges();

    // Verify message is displayed
    expectExists(fixture, 'div', 'Message should be visible');

    // Click clear button
    click(fixture, 'button');
    fixture.detectChanges();

    // Verify messages are cleared
    expect(fixture.nativeElement.textContent).not.toContain('Test message');
    expect(messageService.messages.length).toBe(0);
  });
});
