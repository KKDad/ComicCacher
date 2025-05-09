import { DebugElement, Type, SchemaMetadata } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

/**
 * Utility functions for Angular component testing
 */

interface TestModuleOptions {
  schemas?: SchemaMetadata[];
}

/**
 * Create a component fixture using the specified component type and imports
 * 
 * @param component The component class to create
 * @param imports Array of standalone components/directives/pipes to import
 * @param providers Array of providers for dependency injection
 * @param options Additional TestModule options (schemas, etc)
 * @returns ComponentFixture for the component
 */
export function createStandaloneComponentFixture<T>(
  component: Type<T>,
  imports: any[] = [],
  providers: any[] = [],
  options: TestModuleOptions = {}
): ComponentFixture<T> {
  TestBed.configureTestingModule({
    imports: [component, ...imports],
    providers,
    schemas: options.schemas
  });

  return TestBed.createComponent(component);
}

/**
 * Find a single element within the component's DOM by CSS selector
 * 
 * @param fixture The component fixture
 * @param selector The CSS selector to find
 * @returns DebugElement for the matching element
 */
export function findEl<T>(fixture: ComponentFixture<T>, selector: string): DebugElement {
  return fixture.debugElement.query(By.css(selector));
}

/**
 * Find all elements matching the CSS selector
 * 
 * @param fixture The component fixture
 * @param selector The CSS selector to find
 * @returns Array of DebugElements for the matching elements
 */
export function findEls<T>(fixture: ComponentFixture<T>, selector: string): DebugElement[] {
  return fixture.debugElement.queryAll(By.css(selector));
}

/**
 * Get text content from an element (trimmed)
 * 
 * @param fixture The component fixture
 * @param selector The CSS selector to find
 * @returns The text content of the element
 */
export function getText<T>(fixture: ComponentFixture<T>, selector: string): string {
  return findEl(fixture, selector).nativeElement.textContent.trim();
}

/**
 * Expect that the element with the specified selector exists in the DOM
 * 
 * @param fixture The component fixture
 * @param selector The CSS selector to find
 * @param expectation The expectation message
 */
export function expectExists<T>(
  fixture: ComponentFixture<T>,
  selector: string,
  expectation: string
): void {
  const element = findEl(fixture, selector);
  expect(element).withContext(`Element with selector "${selector}" should exist: ${expectation}`).not.toBeNull();
}

/**
 * Expect that the element with the specified selector does not exist in the DOM
 * 
 * @param fixture The component fixture
 * @param selector The CSS selector to find
 * @param expectation The expectation message
 */
export function expectNotExists<T>(
  fixture: ComponentFixture<T>,
  selector: string,
  expectation: string
): void {
  const element = fixture.debugElement.query(By.css(selector));
  expect(element).withContext(`Element with selector "${selector}" should not exist: ${expectation}`).toBeNull();
}

/**
 * Click an element and detect changes
 * 
 * @param fixture The component fixture
 * @param selector The CSS selector to find
 */
export function click<T>(fixture: ComponentFixture<T>, selector: string): void {
  const element = findEl(fixture, selector).nativeElement;
  element.click();
  fixture.detectChanges();
}

/**
 * Create a spy for a service method
 * 
 * @param service The service to spy on
 * @param method The method name to spy on
 * @param returnValue The value to return when the method is called
 * @returns The spy
 */
export function spyOnService<T, K extends keyof T>(
  service: T,
  method: K,
  returnValue: any
): jasmine.Spy {
  return spyOn(service, method as any).and.returnValue(returnValue);
}