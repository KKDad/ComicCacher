const collator = new Intl.Collator(undefined, { sensitivity: 'base', numeric: true });

/** Orders comics by name the way a reader expects: "Baby Blues" before "BC", ignoring case. */
export function compareByName(a: { name: string }, b: { name: string }): number {
  return collator.compare(a.name, b.name);
}
