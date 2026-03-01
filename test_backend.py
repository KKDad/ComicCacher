#!/usr/bin/env python3
"""Quick smoke test against the GraphQL backend."""

import json
import urllib.request

ENDPOINT = "http://10.0.0.47:8087/graphql"

def gql(query, variables=None, token=None):
    body = json.dumps({"query": query, "variables": variables or {}}).encode()
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(ENDPOINT, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read())
            return resp.status, data
    except urllib.error.HTTPError as e:
        data = json.loads(e.read()) if e.readable() else None
        return e.code, data
    except Exception as e:
        return None, str(e)

# 1. Login
print("=== LOGIN ===")
status, data = gql("""
  mutation Login($input: LoginInput!) {
    login(input: $input) {
      token
      refreshToken
      username
      displayName
    }
  }
""", {"input": {"username": "adrian", "password": "Password1"}})
print(f"Status: {status}")
print(json.dumps(data, indent=2))

token = data.get("data", {}).get("login", {}).get("token") if isinstance(data, dict) else None
if not token:
    print("No token — stopping.")
    exit(1)

# 2. GetMe
print("\n=== GET ME ===")
status, data = gql("query { me { username displayName email roles } }", token=token)
print(f"Status: {status}")
print(json.dumps(data, indent=2))

# 3. GetComics
print("\n=== GET COMICS ===")
status, data = gql("""
  query GetComics($first: Int) {
    comics(first: $first) {
      edges {
        node {
          id
          name
          newest
          avatarUrl
          lastStrip {
            imageUrl
            date
          }
        }
      }
    }
  }
""", {"first": 5}, token=token)
print(f"Status: {status}")
print(json.dumps(data, indent=2))

# 4. GetUserPreferences
print("\n=== GET USER PREFERENCES ===")
status, data = gql("""
  query GetUserPreferences {
    preferences {
      favoriteComics
      lastReadDates {
        comicId
        date
      }
    }
  }
""", token=token)
print(f"Status: {status}")
print(json.dumps(data, indent=2))
