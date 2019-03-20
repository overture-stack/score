#!/bin/bash

set -e

host="$1"
port="$2"
user="$3"
password="$4"

until PGPASSWORD=$password psql -h $host -p $port -U $user -c '\l'; do
    >&2 echo "Postgres is unavailable - sleeping"
    sleep 1
done

>&2 echo "Postgres is up"
