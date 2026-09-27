import { compareByName, compareNames } from './sort';

describe('compareByName', () => {
  it('ignores case, so "Baby Blues" comes before "BC"', () => {
    const names = ['BC', 'Baby Blues', 'adam at home', 'Andy Capp'].map((name) => ({ name }));
    expect(names.sort(compareByName).map((c) => c.name)).toEqual(['adam at home', 'Andy Capp', 'Baby Blues', 'BC']);
  });

  it('orders numbers by value', () => {
    const names = ['Comic 10', 'Comic 2'].map((name) => ({ name }));
    expect(names.sort(compareByName).map((c) => c.name)).toEqual(['Comic 2', 'Comic 10']);
  });
});

describe('compareNames', () => {
  it('ignores case and orders numbers naturally', () => {
    expect(['BC', 'Baby Blues', 'comic 10', 'Comic 9'].sort(compareNames)).toEqual([
      'Baby Blues',
      'BC',
      'Comic 9',
      'comic 10',
    ]);
  });
});
