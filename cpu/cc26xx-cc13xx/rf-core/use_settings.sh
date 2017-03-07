#!/bin/bash

[[ $# -eq 0 ]] && echo 'Please specify the setting to be used.' && exit 255
setting=smartrf-settings.$1
[[ ! -d $setting ]] && echo 'Setting directory '$setting' is not found!' && exit 255


ln -sf ${setting}/smartrf-settings.[ch] .
