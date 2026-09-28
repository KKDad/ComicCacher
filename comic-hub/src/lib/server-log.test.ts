import { newRequestId, operationName, resolveRequestId, timedGraphqlFetch } from './server-log';

describe('request ids', () => {
  it('generates 8 hex digits, like the API', () => {
    expect(newRequestId()).toMatch(/^[0-9a-f]{8}$/);
  });

  it('keeps a well-formed incoming id and replaces anything that could forge log lines', () => {
    expect(resolveRequestId('abc-DEF-123')).toBe('abc-DEF-123');
    expect(resolveRequestId('abc\ninjected')).toMatch(/^[0-9a-f]{8}$/);
    expect(resolveRequestId('x'.repeat(65))).toMatch(/^[0-9a-f]{8}$/);
    expect(resolveRequestId(null)).toMatch(/^[0-9a-f]{8}$/);
  });
});

describe('operationName', () => {
  it('prefers operationName, then the name in the query text', () => {
    expect(operationName(JSON.stringify({ operationName: 'GetComic', query: 'query Other { x }' }))).toBe('GetComic');
    expect(operationName(JSON.stringify({ query: 'mutation RefreshToken($t: String!) { x }' }))).toBe('RefreshToken');
    expect(operationName(JSON.stringify({ query: '{ comics { id } }' }))).toBe('anonymous');
    expect(operationName('not json')).toBe('unknown');
    expect(operationName(undefined)).toBe('unknown');
  });
});

describe('timedGraphqlFetch', () => {
  const body = JSON.stringify({ operationName: 'GetComic', query: 'query GetComic { x }' });

  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response('{}', { status: 200 }));
    vi.spyOn(console, 'log').mockImplementation(() => {});
    vi.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllEnvs();
  });

  it('sends the request id and keeps the caller headers', async () => {
    await timedGraphqlFetch('http://api/graphql', { method: 'POST', headers: { Authorization: 'Bearer t' }, body }, 'ab12cd34');

    const [, init] = vi.mocked(global.fetch).mock.calls[0];
    expect(init!.headers).toEqual({ Authorization: 'Bearer t', 'x-request-id': 'ab12cd34' });
  });

  it('adds the id to a Headers object too', async () => {
    await timedGraphqlFetch('http://api/graphql', { headers: new Headers({ Authorization: 'Bearer t' }), body }, 'ab12cd34');

    const headers = vi.mocked(global.fetch).mock.calls[0][1]!.headers as Headers;
    expect(headers.get('x-request-id')).toBe('ab12cd34');
    expect(headers.get('authorization')).toBe('Bearer t');
  });

  it('logs one timing line with the operation, status and request id', async () => {
    await timedGraphqlFetch('http://api/graphql', { body }, 'ab12cd34');

    expect(console.log).toHaveBeenCalledWith(expect.stringMatching(/^graphql GetComic -> 200 in \d+ms req=ab12cd34$/));
    expect(console.warn).not.toHaveBeenCalled();
  });

  it('warns when the call reaches SLOW_FETCH_MS', async () => {
    vi.stubEnv('SLOW_FETCH_MS', '0.001');
    vi.mocked(global.fetch).mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve(new Response('{}')), 5)),
    );

    await timedGraphqlFetch('http://api/graphql', { body }, 'ab12cd34');

    expect(console.warn).toHaveBeenCalledWith(expect.stringMatching(/^Slow request: graphql GetComic -> 200 in \d+ms req=ab12cd34$/));
  });

  it('logs a failed call and rethrows', async () => {
    vi.mocked(global.fetch).mockRejectedValue(new TypeError('fetch failed'));

    await expect(timedGraphqlFetch('http://api/graphql', { body }, 'ab12cd34')).rejects.toThrow('fetch failed');
    expect(console.log).toHaveBeenCalledWith(expect.stringMatching(/^graphql GetComic -> failed in \d+ms req=ab12cd34$/));
  });
});
