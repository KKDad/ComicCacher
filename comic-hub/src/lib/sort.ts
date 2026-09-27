const collator = new Intl.Collator(undefined, { sensitivity: 'base', numeric: true });

/** Orders names the way a reader expects: "Baby Blues" before "BC", ignoring case. */
export function compareNames(a: string, b: string): number {
  return collator.compare(a, b);
}

/** Orders comics by name; see compareNames. */
export function compareByName(a: { name: string }, b: { name: string }): number {
  return compareNames(a.name, b.name);
}
