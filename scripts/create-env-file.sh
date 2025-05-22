#!/bin/bash

# Define the output .env file name
ENV_FILE=".env"

# Function to generate a random alphanumeric string of a specific length
# $1: desired length of the random part of the string
# $2: (optional) prefix for the string
generate_random_string() {
  local length=${1}
  local prefix=${2:-} # Default to empty prefix if not provided
  local random_chars=""

  # Check if length is provided and is a positive number
  if [[ -z "$length" || "$length" -le 0 ]]; then
    echo "Error: Length must be a positive number for generate_random_string." >&2
    return 1
  fi

  # Generate random characters using od (octal dump) for hex, then grep for desired range
  # Read a bit more than needed to ensure enough random characters after filtering
  # `od -x` gives hex, `tr -d ' '` removes spaces, then `grep -o '[0-9a-fA-F]'` filters to hex chars
  # Then we just use those hex chars as our "random" string, or convert if truly alphanumeric needed
  # For simple alphanumeric, we can generate a longer hex string and slice it.

  # Simpler and more direct: generate hex and use that.
  # This produces a string of [0-9a-f] characters.
  # If you strictly need A-Z (uppercase) as well, it's slightly more complex.
  # Let's generate a string of A-Za-z0-9
  random_chars=$(head /dev/urandom | LC_ALL=C tr -dc 'A-Za-z0-9' | head -c "$length")

  # Double-check the length, just in case (should be fine with LC_ALL=C now)
  if [ ${#random_chars} -lt "$length" ]; then
    # Fallback if the above still fails for some very rare reason or very short lengths
    # This loop is slow but guarantees length and avoids `tr` errors with LC_ALL=C
    local current_len=${#random_chars}
    while [ "$current_len" -lt "$length" ]; do
      random_chars+=$(LC_ALL=C head /dev/urandom | tr -dc 'A-Za-z0-9' | head -c 1)
      current_len=${#random_chars}
    done
  fi

  echo "${prefix}${random_chars:0:length}" # Ensure exact length
}

# Generate random values for each variable
# Using appropriate lengths for typical use cases
RANDOM_DB_USER="user_$(generate_random_string 8)" # 8 random chars after "user_"
RANDOM_DB_PASS="$(generate_random_string 24)"    # 24 random chars for password
RANDOM_API_KEY="api_$(generate_random_string 32)" # 32 random chars after "api_"
RANDOM_AWS_ACCESS_KEY_ID="AKIA$(generate_random_string 16)" # AWS keys often start with AKIA, 16 random chars
RANDOM_AWS_SECRET_ACCESS_KEY="$(generate_random_string 40)" # AWS secret keys are typically 40 random chars

# Create or overwrite the .env file
echo "# Generated on $(date)" > "${ENV_FILE}"
echo "DB_USER=${RANDOM_DB_USER}" >> "${ENV_FILE}"
echo "DB_PASS=${RANDOM_DB_PASS}" >> "${ENV_FILE}"
echo "API_KEY=${RANDOM_API_KEY}" >> "${ENV_FILE}"
echo "AWS_ACCESS_KEY_ID=${RANDOM_AWS_ACCESS_KEY_ID}" >> "${ENV_FILE}"
echo "AWS_SECRET_ACCESS_KEY=${RANDOM_AWS_SECRET_ACCESS_KEY}" >> "${ENV_FILE}"

echo "Generated ${ENV_FILE} with random values."
echo "---"
cat "${ENV_FILE}"