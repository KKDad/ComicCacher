import { documentOperationName } from './graphql-operation';

describe('documentOperationName', () => {
  it('names the query or mutation in the document', () => {
    expect(documentOperationName('query GetComic($id: Int!) { comic(id: $id) { id } }')).toBe('GetComic');
    expect(documentOperationName('mutation RefreshToken($t: String!) { x }')).toBe('RefreshToken');
    expect(documentOperationName('query GetComic { comic { ...F } }\nfragment F on Comic { id }')).toBe('GetComic');
  });

  it('is undefined for an anonymous document', () => {
    expect(documentOperationName('{ comics { id } }')).toBeUndefined();
    expect(documentOperationName('query { comics }')).toBeUndefined();
  });
});
