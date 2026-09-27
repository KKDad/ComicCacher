// Invented comics for the README screenshots. Titles, authors and gags are
// made up so the screenshots show no real strips, names or artwork.

export const DAYS = 14;

// cast: which drawers from strips.mjs appear, speaker 0 and speaker 1
// scene: background and props for every panel
// gags: one line per panel, as [speaker, text]; null leaves a panel silent
export const COMICS = [
  {
    id: 101,
    name: 'Backyard Expeditions',
    author: 'Nora Pell',
    description: 'A kid, a dog and one very large backyard.',
    cast: ['kid', 'dog'],
    scene: 'meadow',
    gags: [
      [[0, 'Today we map the backyard.'], [0, 'Uncharted land, Biscuit.'], [1, 'Woof.']],
      [[0, 'This stick is a sword.'], [0, 'And a telescope.'], [1, 'And lunch.']],
      [[0, 'Base camp is ready!'], null, [0, "Let's go in. It's cold."]],
      [[0, 'Look! Dragon tracks!'], [1, 'Those are mine.'], [0, 'Even better.']],
    ],
  },
  {
    id: 102,
    name: 'Owl Hours',
    author: 'Tobias Wren',
    description: 'Two owls keep very different schedules.',
    cast: ['owl', 'owl2'],
    scene: 'night',
    gags: [
      [[0, 'Good morning!'], [1, "It's midnight."], [0, 'Exactly.']],
      [[1, 'Who?'], [0, 'Who what?'], [1, 'Just warming up.']],
      [[0, 'I read all night.'], [1, 'You slept on the book.'], [0, 'Osmosis.']],
      [[1, 'Look, a shooting star!'], [0, 'Make a wish.'], [1, 'More mice.']],
    ],
  },
  {
    id: 103,
    name: 'Fern & Fronds',
    author: 'Petra Moss',
    description: 'Houseplants with opinions about the windowsill.',
    cast: ['plant', 'plant2'],
    scene: 'window',
    gags: [
      [[0, 'Is it sunny today?'], [1, 'Partly.'], [0, 'Which part?']],
      [[1, 'She watered me first.'], [0, 'You are dramatic.'], [1, 'I am thirsty.']],
      [[0, 'I grew a new leaf!'], null, [1, 'Show-off.']],
      [[1, 'Rotate me, please.'], [0, 'You just want the view.'], [1, 'Obviously.']],
    ],
  },
  {
    id: 104,
    name: 'Rocket Lunchbox',
    author: 'Dev Okafor',
    description: 'A kid built a robot. The robot packs lunch.',
    cast: ['kid', 'robot'],
    scene: 'kitchen',
    gags: [
      [[1, 'Lunch is ready.'], [0, 'Is that a bolt?'], [1, 'Extra iron.']],
      [[0, 'Can you do homework?'], [1, 'I can eat it.'], [0, 'Close enough.']],
      [[1, 'Scanning sandwich...'], null, [1, 'Crust detected.']],
      [[0, 'Ready for launch?'], [1, 'Seatbelt on.'], [0, "It's the bus."]],
    ],
  },
  {
    id: 105,
    name: 'Tidepool Tales',
    author: 'Marisol Reyes',
    description: 'Life, gossip and the tide schedule in a small rock pool.',
    cast: ['crab', 'gull'],
    scene: 'shore',
    gags: [
      [[0, 'Tide is coming in!'], [1, 'Again?'], [0, 'Twice a day.']],
      [[1, 'Nice shell.'], [0, 'It came with the place.'], [1, 'Lucky.']],
      [[0, 'I walk sideways.'], [1, 'Why?'], [0, 'Better view.']],
      [[1, 'Got any chips?'], null, [0, 'I AM the chips.']],
    ],
  },
  {
    id: 106,
    name: 'Weekend Robots',
    author: 'Iris Calder',
    description: 'Two robots try very hard to relax.',
    cast: ['robot', 'robot2'],
    scene: 'park',
    gags: [
      [[0, 'We are relaxing.'], [1, 'Relaxing at 94%.'], [0, 'Try harder.']],
      [[1, 'I made a sandwich.'], [0, 'Out of what?'], [1, 'Mostly sand.']],
      [[0, 'Beep.'], [1, 'Boop.'], [0, 'Great talk.']],
      [[1, 'Is this fun?'], null, [0, 'Updating...']],
    ],
  },
  {
    id: 107,
    name: 'Lighthouse Keepers',
    author: 'Hal Brennan',
    description: 'A keeper, a gull and a very bright light.',
    cast: ['keeper', 'gull'],
    scene: 'coast',
    gags: [
      [[1, 'Is the light on?'], [0, 'Always.'], [1, 'Show-off.']],
      [[0, 'Foggy today.'], [1, 'Very.'], [0, 'Where are you?']],
      [[1, 'Any mail?'], [0, 'A postcard.'], [1, 'From the other lighthouse?']],
      [[0, 'Storm coming.'], null, [1, 'I felt that.']],
    ],
  },
  {
    id: 108,
    name: 'Snack Dragon',
    author: 'Juniper Holt',
    description: 'A small dragon with a large appetite.',
    cast: ['dragon', 'kid'],
    scene: 'cave',
    gags: [
      [[0, 'I guard this treasure.'], [1, "It's cookies."], [0, 'Treasure.']],
      [[1, 'Can you breathe fire?'], [0, 'Only toast.'], [1, 'Useful.']],
      [[0, 'I am fearsome!'], null, [0, 'After my nap.']],
      [[1, 'Want to share?'], [0, 'Dragons never share.'], [0, '...one crumb.']],
    ],
  },
];

// Favorites, and where the demo reader stopped in each (days before today).
// 102 and 104 are behind, so the dashboard shows "Continue" and "New".
export const FAVORITES = [101, 102, 104, 108];
export const LAST_READ_DAYS_AGO = { 101: 0, 102: 3, 104: 2, 108: 1 };

// Some comics stopped a day or two ago, so Latest Updates sorts by date.
export const NEWEST_DAYS_AGO = { 105: 1, 107: 2 };

// Wide strips: four panels instead of three.
export const FOUR_PANEL = new Set([103, 107]);

export const DEMO_USER = {
  username: 'reader',
  email: 'reader@example.com',
  displayName: 'Demo Reader',
  roles: ['USER'],
};
