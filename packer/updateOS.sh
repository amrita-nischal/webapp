#!/bin/bash

set -e

export DEBIAN_FRONTEND=noninteractive 
export CHECKPOINT_DISABLE=1

sudo apt-get update 
sudo apt-get upgrade -y 
sudo apt-get clean

sudo apt-get install -y postgresql-client
