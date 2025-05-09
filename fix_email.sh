#\!/bin/bash

# Script to update author email in recent commits
# From: agilbert@upgrade.com
# To: adrian@gilbert.ca

# First, stash any changes to prevent issues
git stash -u

# Backup the current state for safety
git tag backup_before_email_fix

# Create a temporary script for git filter-branch
cat > /tmp/update_commit_email.sh << 'EOT'
#\!/bin/bash

if [ "$GIT_AUTHOR_EMAIL" = "agilbert@upgrade.com" ]; then
  export GIT_AUTHOR_EMAIL="adrian@gilbert.ca"
fi
if [ "$GIT_COMMITTER_EMAIL" = "agilbert@upgrade.com" ]; then
  export GIT_COMMITTER_EMAIL="adrian@gilbert.ca"
fi
EOT

chmod +x /tmp/update_commit_email.sh

# Use git filter-branch to rewrite history
git filter-branch -f --env-filter "source /tmp/update_commit_email.sh" -- --since="1 day ago"

# Clean up
rm /tmp/update_commit_email.sh

# Show the changes made
echo "Email addresses updated. Here are the commits from the last day:"
git log --since="1 day ago" --pretty=format:"%h %an <%ae> %s" | head -10

# If there were stashed changes, restore them
git stash pop 2>/dev/null || true

echo "Email addresses updated successfully."
