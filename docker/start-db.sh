#!/usr/bin/env bash

docker run --name tezroktest --rm -p 5432:5432 -e POSTGRES_DB=tezrokdb -e POSTGRES_USER=tezrokAdmin -e POSTGRES_PASSWORD=tezrokPwd -d postgres:15.2
