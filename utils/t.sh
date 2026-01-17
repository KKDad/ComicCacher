curl --location 'http://portainer.stapledon.ca:8888/api/v1/auth/register' \
--header 'Content-Type: application/json' \
--data-raw '{
  "username": "agilbert",
  "password": "securePassword123",
  "email": "test@example.com",
  "firstName": "Adrian",
  "lastName": "Gilbert"
}'
