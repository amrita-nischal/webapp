#!/bin/bash

set -e

DIR="/opt/webapp/logs"

sudo mkdir -p "${DIR}"

sudo useradd --system -s /usr/sbin/nologin csye6225
