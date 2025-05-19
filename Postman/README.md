# ComicCacher API Testing Environment

This directory contains the Postman collection and environment settings for testing the ComicCacher API.

## Contents

- `collection.json` - Postman collection 
- `environment.json` - Environment variables required for testing

## Setup Instructions

### 1. Prerequisites

- [Postman](https://www.postman.com/downloads/) installed on your system
- ComicCacher API running locally or on a remote server

### 2. Import Collection and Environment

1. Open Postman
2. Click on "Import" in the upper left corner
3. Select both `updated_collection.json` and `environment.json` files
4. Confirm the import

### 3. Configure Environment

1. In Postman, select the "ComicCacher Environment" from the environment dropdown in the upper right
2. Verify the `baseUrl` is set correctly:
   - Default is `http://localhost:8888`
   - Adjust if your API is running on a different host or port

### 4. Running the Tests

#### Option 1: Run All Tests

1. Select the "ComicCacher API" collection in the left sidebar
2. Click the "Run" button in the collection header
3. In the Collection Runner, ensure all requests are selected
4. Click the "Run ComicCacher API" button
5. The tests will execute in sequence, setting environment variables as needed

#### Option 2: Run Individual Test Groups

You can run specific folders of tests:

1. Expand the collection in the left sidebar
2. Right-click on a folder (e.g., "1. Setup")
3. Select "Run" from the context menu
4. Click the "Run" button in the Collection Runner

## Test Sequence

The collection is organized in a logical sequence to handle dependencies between tests:

1. **Setup** - Register/login and obtain authentication tokens
2. **Comics** - Basic comic operations (list, get, create)
3. **Comic Images** - Retrieve comic images and navigate between strips
4. **User Management** - Profile operations
5. **Preferences** - User preference management
6. **Updates and Metrics** - Comic updates and system metrics
7. **Cleanup** - Delete test data
8. **Health Check** - System health verification

## Environment Variables

The collection uses the following environment variables:

| Variable | Description | Set By |
|----------|-------------|--------|
| baseUrl | API base URL | Manual configuration |
| username | User's username | Register User test |
| token | JWT authentication token | Login/Register tests |
| refreshToken | Token for refresh operations | Login/Register tests |
| testComicId | ID of an existing comic | Get All Comics test |
| secondTestComicId | ID of another comic | Get All Comics test |
| createdComicId | ID of a newly created comic | Create Comic test |
| testDate | Date for comic navigation | Get First Comic Strip test |

## Troubleshooting

### Common Issues

1. **Authentication Errors (401)**
   - Check if the token is set correctly in the environment
   - Login again to refresh the token

2. **Missing Variables**
   - Ensure tests are run in the correct sequence
   - Pre-request scripts will warn about missing required variables

3. **Comic Not Found Errors (404)**
   - Verify the comic IDs are valid
   - Run the "Get All Comics" test to update the comic IDs

4. **Server Connection Issues**
   - Verify the API is running
   - Check the `baseUrl` in the environment settings

### Debugging Tips

- Enable Console in Postman (View > Show Postman Console)
- Check the pre-request and test scripts output
- Examine the response body for error messages

## Notes for Test Maintenance

1. **Adding New Tests**
   - Follow the existing pattern for setting and using environment variables
   - Add appropriate test scripts to validate responses
   - Include pre-request scripts to verify prerequisites

2. **Updating Existing Tests**
   - Maintain the sequence dependencies
   - Update assertions if API response format changes
   - Keep environment variable handling consistent

3. **Making Collection Changes**
   - Update this README if the structure or variables change
   - Document any new dependencies between tests
