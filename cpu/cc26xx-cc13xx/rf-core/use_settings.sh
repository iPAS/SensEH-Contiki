#!/bin/bash

possible=$(find . -maxdepth 1 -type d -iname smartrf-settings.\* | sed -r 's/\.\/smartrf-settings\.(.+)/'\''\1'\''/')
[[ $# -eq 0 ]] && echo 'Please specify the setting to be used, i.e: '${possible} && exit 255
setting=smartrf-settings.$1
[[ ! -d $setting ]] && echo 'Setting directory '$setting' is not found!' && exit 255


ln -sf ${setting}/smartrf-settings.[ch] .
